package kr.inventory.domain.store.service;

import kr.inventory.domain.store.controller.dto.request.*;
import kr.inventory.domain.store.controller.dto.response.*;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreMemberRepository;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.exception.UserErrorCode;
import kr.inventory.domain.user.exception.UserException;
import kr.inventory.domain.user.repository.UserRepository;
import kr.inventory.global.util.BusinessRegistrationNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreMemberRepository storeMemberRepository;
    private final UserRepository userRepository;
    private final BusinessStatusVerifier businessStatusVerifier;

    @Transactional
    public StoreCreateResponse createStore(Long userId, StoreCreateRequest request) {

        // 사업자등록번호 정규화 (하이픈 제거, 숫자만 10자리)
        String normalizedBrn = BusinessRegistrationNumberUtil.normalize(request.businessRegistrationNumber());

        // 사업자등록번호 중복 검증
        if (storeRepository.existsByBusinessRegistrationNumber(normalizedBrn)) {
            throw new StoreException(StoreErrorCode.DUPLICATE_BUSINESS_REGISTRATION_NUMBER);
        }

        // 사업자등록번호 유효성 및 상태 검증
        businessStatusVerifier.verify(normalizedBrn);

        Store store = Store.create(request.name(), normalizedBrn);
        Store savedStore = storeRepository.save(store);

        // 생성자를 OWNER로 등록
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // displayOrder 계산
        Integer maxDisplayOrder = storeMemberRepository.findMaxDisplayOrderByUserUserId(userId);
        Integer displayOrder = maxDisplayOrder + 1;

        // 첫 매장 여부 확인
        boolean isFirstStore = (maxDisplayOrder == -1);

        StoreMember owner = StoreMember.create(savedStore, user, StoreMemberRole.OWNER, displayOrder, isFirstStore);
        storeMemberRepository.save(owner);

        return StoreCreateResponse.from(savedStore, StoreMemberRole.OWNER);
    }

    public List<MyStoreResponse> getMyStores(Long userId) {
        List<StoreMember> memberships = storeMemberRepository.findAllByUserUserIdWithStore(userId);

        return memberships.stream()
            .map(MyStoreResponse::from)
            .toList();
    }

    public MyStoreResponse getStoreByPublicId(Long userId, UUID storePublicId) {
        Store store = storeRepository.findByStorePublicId(storePublicId)
            .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        StoreMember member = storeMemberRepository
            .findByStoreStoreIdAndUserUserId(store.getStoreId(), userId)
            .orElseThrow(() -> new StoreException(StoreErrorCode.NOT_STORE_MEMBER));

        return MyStoreResponse.from(member);
    }

    @Transactional
    public MyStoreResponse updateStoreName(Long userId, UUID storePublicId, StoreNameUpdateRequest request) {
        Store store = storeRepository.findByStorePublicId(storePublicId)
            .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        StoreMember member = storeMemberRepository
            .findByStoreStoreIdAndUserUserId(store.getStoreId(), userId)
            .orElseThrow(() -> new StoreException(StoreErrorCode.NOT_STORE_MEMBER));

        if (member.getRole() != StoreMemberRole.OWNER) {
            throw new StoreException(StoreErrorCode.OWNER_PERMISSION_REQUIRED);
        }

        store.updateName(request.name());

        return MyStoreResponse.from(member);
    }

    @Transactional
    public void setDefaultStore(Long userId, UUID storePublicId) {
        Store store = storeRepository.findByStorePublicId(storePublicId)
            .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        StoreMember member = storeMemberRepository
            .findByStoreStoreIdAndUserUserId(store.getStoreId(), userId)
            .orElseThrow(() -> new StoreException(StoreErrorCode.NOT_STORE_MEMBER));

        // 기존 대표 매장 해제
        storeMemberRepository.unsetAllDefaultsByUserId(userId);

        // 현재 매장을 대표 매장으로 설정
        member.setAsDefault();
    }
}
