package kr.inventory.domain.store.service;

import kr.inventory.domain.store.constant.InvitationConstants;
import kr.inventory.domain.store.controller.dto.request.InvitationAcceptRequest;
import kr.inventory.domain.store.controller.dto.response.InvitationAcceptResponse;
import kr.inventory.domain.store.controller.dto.response.InvitationCreateResponse;
import kr.inventory.domain.store.controller.dto.response.InvitationItemResponse;
import kr.inventory.domain.store.entity.Invitation;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.InvitationStatus;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.InvitationRepository;
import kr.inventory.domain.store.repository.StoreMemberRepository;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.exception.UserErrorCode;
import kr.inventory.domain.user.exception.UserException;
import kr.inventory.domain.user.repository.UserRepository;
import kr.inventory.global.config.InvitationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final StoreMemberRepository storeMemberRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final InvitationProperties invitationProperties;

    @Transactional
    public InvitationCreateResponse createInvitation(Long userId, Long storeId) {
        boolean isOwner = storeMemberRepository.hasRole(storeId, userId, StoreMemberRole.OWNER);
        if (!isOwner) {
            throw new StoreException(StoreErrorCode.OWNER_PERMISSION_REQUIRED);
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        User invitedBy = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 초대 만료 시간 설정
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(invitationProperties.getTtl());

        // 매장당 초대는 1개만 존재 -> 있으면 갱신하고 없으면 새로 생성
        Optional<Invitation> existingInvitationOpt = invitationRepository.findByStoreStoreId(storeId);

        String token = generateToken();

        String currentCode = existingInvitationOpt.map(Invitation::getCode).orElse(null);
        String code = generateCodeAvoidingSame(currentCode);

        Invitation invitation;
        if (existingInvitationOpt.isPresent()) {
            invitation = existingInvitationOpt.get();
            invitation.renew(invitedBy, token, code, expiresAt); // 재발급
        } else {
            invitation = Invitation.create(store, invitedBy, token, code, expiresAt);
            invitation = invitationRepository.save(invitation);
        }

        return InvitationCreateResponse.from(invitation, invitationProperties.getFrontBaseUrl());
    }


    /*
     * - token 또는 code 중 하나만 허용
     * - REVOKED(취소)면 거절
     * - expiresAt 지났으면 만료 처리 거절
    * */
    @Transactional
    public InvitationAcceptResponse acceptInvitation(Long userId, InvitationAcceptRequest request) {
        boolean hasToken = request != null
                && request.token() != null
                && !request.token().isBlank();

        boolean hasCodeFlow = request != null
                && request.storeId() != null
                && request.code() != null
                && !request.code().isBlank();

        // true = ture / false = false
        // 둘 다 없음 또는 둘 다 있음 -> 입력 형태 오류(xor)
        if (hasToken == hasCodeFlow) {
            throw new StoreException(StoreErrorCode.INVALID_INVITATION_REQUEST);
        }

        // 토큰 or 코드 검증
        Invitation invitation = hasToken
                ? invitationRepository.findByToken(request.token())
                .orElseThrow(() -> new StoreException(StoreErrorCode.INVITATION_NOT_FOUND))
                : invitationRepository.findByStoreStoreIdAndCode(request.storeId(), request.code())
                .orElseThrow(() -> new StoreException(StoreErrorCode.INVITATION_NOT_FOUND));

        if (invitation.getStatus() == InvitationStatus.REVOKED) {
            throw new StoreException(StoreErrorCode.INVITATION_REVOKED);
        }

        // 만료 시간 검증 (now >= expiresAt)
        if (!OffsetDateTime.now().isBefore(invitation.getExpiresAt())) {
            throw new StoreException(StoreErrorCode.INVITATION_EXPIRED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        Store store = invitation.getStore();
        Optional<StoreMember> existingMember = storeMemberRepository
                .findByStoreStoreIdAndUserUserId(store.getStoreId(), userId);

        if (existingMember.isPresent()) {
            StoreMember member = existingMember.get();
            if (member.getStatus() == StoreMemberStatus.ACTIVE) {
                throw new StoreException(StoreErrorCode.ALREADY_MEMBER); // 이미 존재하는 멤버일 때, ACTIVE 이면 에러 메시지
            }
            member.updateStatus(StoreMemberStatus.ACTIVE); // INACTIVE 이면, ACTIVE 전환
            return InvitationAcceptResponse.from(member);
        }

        StoreMember newMember = StoreMember.create(store, user, StoreMemberRole.MEMBER);
        StoreMember saved = storeMemberRepository.save(newMember);

        return InvitationAcceptResponse.from(saved);
    }

    public InvitationItemResponse getActiveInvitation(Long userId, Long storeId) {
        boolean isOwner = storeMemberRepository.hasRole(storeId, userId, StoreMemberRole.OWNER);
        if (!isOwner) {
            throw new StoreException(StoreErrorCode.OWNER_PERMISSION_REQUIRED);
        }

        Invitation invitation = invitationRepository.findByStoreStoreId(storeId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.NO_ACTIVE_INVITATION));

        // ACTIVE + expiresAt 유효한 경우만 활성 초대
        if (invitation.getStatus() != InvitationStatus.ACTIVE) {
            throw new StoreException(StoreErrorCode.NO_ACTIVE_INVITATION);
        }
        if (!OffsetDateTime.now().isBefore(invitation.getExpiresAt())) {
            throw new StoreException(StoreErrorCode.NO_ACTIVE_INVITATION);
        }

        return InvitationItemResponse.from(invitation, invitationProperties.getFrontBaseUrl());
    }

    @Transactional
    public void revokeActiveInvitation(Long userId, Long storeId) {
        boolean isOwner = storeMemberRepository.hasRole(storeId, userId, StoreMemberRole.OWNER);
        if (!isOwner) {
            throw new StoreException(StoreErrorCode.OWNER_PERMISSION_REQUIRED);
        }

        Invitation invitation = invitationRepository.findByStoreStoreId(storeId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.NO_ACTIVE_INVITATION));

        if (invitation.getStatus() != InvitationStatus.ACTIVE) {
            throw new StoreException(StoreErrorCode.NO_ACTIVE_INVITATION);
        }
        if (!OffsetDateTime.now().isBefore(invitation.getExpiresAt())) {
            throw new StoreException(StoreErrorCode.NO_ACTIVE_INVITATION);
        }

        invitation.revoke();
    }

    // 초대 token 생성
    private String generateToken() {
        byte[] tokenBytes = new byte[InvitationConstants.TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    // 초대 코드 생성(storeId + code)
    private String generateCodeAvoidingSame(String currentCode) {
        String code = generateRandomCode();
        if (currentCode != null && currentCode.equals(code)) {
            return generateRandomCode();
        }
        return code;
    }

    private String generateRandomCode() {
        int code = SECURE_RANDOM.nextInt(100000000);
        return String.format("%08d", code);
    }
}
