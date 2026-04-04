package kr.inventory.domain.store.service;

import kr.inventory.domain.notification.service.publish.NotificationPublishCommand;
import kr.inventory.domain.notification.service.publish.NotificationPublishRequestEvent;
import kr.inventory.domain.notification.service.publish.NotificationPublishService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

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
    private final NotificationPublishService notificationPublishService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InvitationCreateResponse createInvitation(Long userId, UUID storePublicId) {
        Store store = storeRepository.findByStorePublicId(storePublicId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        User invitedBy = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        OffsetDateTime expiresAt = OffsetDateTime.now().plus(invitationProperties.getTtl());
        Optional<Invitation> existingInvitationOpt = invitationRepository.findByStoreStoreId(store.getStoreId());

        String token = generateToken();
        String currentCode = existingInvitationOpt.map(Invitation::getCode).orElse(null);
        String code = generateCodeAvoidingSame(currentCode);

        Invitation invitation = existingInvitationOpt
                .map(existing -> {
                    existing.renew(invitedBy, token, code, expiresAt);
                    return existing;
                })
                .orElseGet(() -> Invitation.create(store, invitedBy, token, code, expiresAt));

        invitation = invitationRepository.save(invitation);

        return InvitationCreateResponse.from(invitation, invitationProperties.getFrontBaseUrl());
    }

    @Transactional
    public InvitationAcceptResponse acceptInvitation(Long userId, InvitationAcceptRequest request) {
        boolean hasToken = request != null
                && request.token() != null
                && !request.token().isBlank();

        boolean hasCode = request != null
                && request.code() != null
                && !request.code().isBlank();

        if (hasToken == hasCode) {
            throw new StoreException(StoreErrorCode.INVALID_INVITATION_REQUEST);
        }

        Invitation invitation = hasToken
                ? invitationRepository.findByToken(request.token())
                .orElseThrow(() -> new StoreException(StoreErrorCode.INVITATION_NOT_FOUND))
                : invitationRepository.findByCode(request.code())
                .orElseThrow(() -> new StoreException(StoreErrorCode.INVITATION_NOT_FOUND));

        if (invitation.getStatus() == InvitationStatus.REVOKED) {
            throw new StoreException(StoreErrorCode.INVITATION_REVOKED);
        }

        if (!OffsetDateTime.now().isBefore(invitation.getExpiresAt())) {
            throw new StoreException(StoreErrorCode.INVITATION_EXPIRED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        Store store = invitation.getStore();
        Optional<StoreMember> existingMember = storeMemberRepository
                .findByStoreStoreIdAndUserUserId(store.getStoreId(), userId);

        StoreMember joinedMember;
        StoreMemberRole role;

        if (existingMember.isPresent()) {
            StoreMember member = existingMember.get();
            if (member.getStatus() == StoreMemberStatus.ACTIVE) {
                throw new StoreException(StoreErrorCode.ALREADY_MEMBER);
            }

            // INACTIVE → ACTIVE 재활성화
            member.updateStatus(StoreMemberStatus.ACTIVE);

            // 재활성화 후 ACTIVE 매장이 1개뿐이면 대표 매장으로 설정
            long activeStoreCount = storeMemberRepository.countActiveByUserId(userId);
            if (activeStoreCount == 1) {
                storeMemberRepository.unsetAllDefaultsByUserId(userId);
                member.setAsDefault();
            }

            joinedMember = member;
            role = member.getRole();
        } else {
            Integer maxDisplayOrder = storeMemberRepository.findMaxDisplayOrderByUserUserId(userId);
            Integer displayOrder = maxDisplayOrder + 1;

            // ACTIVE 매장 개수로 첫 매장 여부 판단 (INACTIVE는 제외)
            long activeStoreCount = storeMemberRepository.countActiveByUserId(userId);
            boolean isFirstStore = (activeStoreCount == 0);

            StoreMember newMember = StoreMember.create(store, user, StoreMemberRole.MEMBER, displayOrder, isFirstStore);
            joinedMember = storeMemberRepository.save(newMember);
            role = StoreMemberRole.MEMBER;
        }

        sendInvitationNotifications(user, store, role);

        return InvitationAcceptResponse.from(joinedMember);
    }

    private void sendInvitationNotifications(User joinedUser, Store store, StoreMemberRole role) {
        eventPublisher.publishEvent(new NotificationPublishRequestEvent(
                NotificationPublishCommand.storeMemberRegistered(
                        joinedUser.getUserId(),
                        store.getStorePublicId(),
                        store.getName(),
                        role.name()
                )
        ));

        storeMemberRepository.findAllByStoreStoreIdWithUser(store.getStoreId())
                .stream()
                .filter(member -> member.getRole() == StoreMemberRole.OWNER)
                .filter(member -> member.getStatus() == StoreMemberStatus.ACTIVE)
                .map(member -> member.getUser().getUserId())
                .filter(ownerUserId -> !ownerUserId.equals(joinedUser.getUserId()))
                .distinct()
                .forEach(ownerUserId -> {
                    NotificationPublishCommand command = NotificationPublishCommand.storeMemberJoined(
                            ownerUserId,
                            store.getStorePublicId(),
                            store.getName(),
                            joinedUser.getUserId(),
                            joinedUser.getName(),
                            role.name()
                    );
                    eventPublisher.publishEvent(new NotificationPublishRequestEvent(command));
                });
    }

    public InvitationItemResponse getActiveInvitation(UUID storePublicId) {
        Store store = storeRepository.findByStorePublicId(storePublicId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        Invitation invitation = invitationRepository.findByStoreStoreId(store.getStoreId())
                .orElseThrow(() -> new StoreException(StoreErrorCode.NO_ACTIVE_INVITATION));

        if (invitation.getStatus() != InvitationStatus.ACTIVE) {
            throw new StoreException(StoreErrorCode.NO_ACTIVE_INVITATION);
        }
        if (!OffsetDateTime.now().isBefore(invitation.getExpiresAt())) {
            throw new StoreException(StoreErrorCode.NO_ACTIVE_INVITATION);
        }

        return InvitationItemResponse.from(invitation, invitationProperties.getFrontBaseUrl());
    }

    @Transactional
    public void revokeActiveInvitation(UUID storePublicId) {
        Store store = storeRepository.findByStorePublicId(storePublicId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        Invitation invitation = invitationRepository.findByStoreStoreId(store.getStoreId())
                .orElseThrow(() -> new StoreException(StoreErrorCode.NO_ACTIVE_INVITATION));

        if (invitation.getStatus() != InvitationStatus.ACTIVE) {
            throw new StoreException(StoreErrorCode.NO_ACTIVE_INVITATION);
        }
        if (!OffsetDateTime.now().isBefore(invitation.getExpiresAt())) {
            throw new StoreException(StoreErrorCode.NO_ACTIVE_INVITATION);
        }

        invitation.revoke();
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[InvitationConstants.TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String generateCodeAvoidingSame(String currentCode) {
        String code = generateUniqueCode();
        if (currentCode != null && currentCode.equals(code)) {
            return generateUniqueCode();
        }
        return code;
    }

    private String generateUniqueCode() {
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            String code = generateRandomCode();
            if (invitationRepository.findByCode(code).isEmpty()) {
                return code;
            }
        }
        throw new StoreException(StoreErrorCode.CODE_GENERATION_FAILED);
    }

    private String generateRandomCode() {
        int code = SECURE_RANDOM.nextInt(100000000);
        return String.format("%08d", code);
    }
}
