package kr.inventory.domain.reference.service;

import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.reference.controller.dto.request.VendorCreateRequest;
import kr.inventory.domain.reference.controller.dto.request.VendorSearchRequest;
import kr.inventory.domain.reference.controller.dto.response.VendorResponse;
import kr.inventory.domain.reference.controller.dto.request.VendorUpdateRequest;
import kr.inventory.domain.reference.entity.Vendor;
import kr.inventory.domain.reference.entity.enums.VendorStatus;
import kr.inventory.domain.reference.exception.VendorErrorCode;
import kr.inventory.domain.reference.exception.VendorException;
import kr.inventory.domain.reference.repository.VendorRepository;
import kr.inventory.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorService {

    private final VendorRepository vendorRepository;
    private final StoreRepository storeRepository;
    private final StoreAccessValidator storeAccessValidator;

    @Transactional
    public VendorResponse createVendor(Long userId, UUID storePublicId, VendorCreateRequest request) {
        // 1. 매장 조회
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        // 2. 거래처명 중복 체크
        if (vendorRepository.existsByStoreStoreIdAndName(storeId, request.name())) {
            throw new VendorException(VendorErrorCode.VENDOR_DUPLICATE_NAME);
        }

        // 3. 거래처 생성
        Vendor vendor = Vendor.create(
                store,
                request.name(),
                request.contactPerson(),
                request.phone(),
                request.email(),
                request.leadTimeDays()
        );

        // 4. 저장
        Vendor savedVendor = vendorRepository.save(vendor);

        return VendorResponse.from(savedVendor);
    }

    public PageResponse<VendorResponse> getVendorsByStore(
            Long userId,
            UUID storePublicId,
            VendorSearchRequest searchRequest,
            Pageable pageable
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Page<Vendor> vendorPage = vendorRepository.findByStoreIdWithFilters(
                storeId,
                searchRequest.status(),
                searchRequest.search(),
                pageable
        );

        Page<VendorResponse> responsePage = vendorPage.map(VendorResponse::from);
        return PageResponse.from(responsePage);
    }

    public VendorResponse getVendor(UUID storePublicId, UUID vendorPublicId, Long userId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        Vendor vendor = getValidVendor(vendorPublicId, storeId);

        return VendorResponse.from(vendor);
    }

    @Transactional
    public VendorResponse updateVendor(UUID storePublicId, UUID vendorPublicId, Long userId, VendorUpdateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        Vendor vendor = getValidVendor(vendorPublicId, storeId);

        // 연락처 정보 수정
        vendor.updateContactInfo(
                request.contactPerson(),
                request.phone(),
                request.email()
        );

        // 리드타임 수정
        if (request.leadTimeDays() != null) {
            vendor.updateLeadTime(request.leadTimeDays());
        }

        // 상태 수정
        if (request.status() != null) {
            if (request.status() == VendorStatus.ACTIVE) {
                vendor.activate();
            } else if (request.status() == VendorStatus.INACTIVE) {
                vendor.deactivate();
            }
        }

        return VendorResponse.from(vendor);
    }

    @Transactional
    public void deactivateVendor(UUID storePublicId, UUID vendorPublicId, Long userId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        Vendor vendor = getValidVendor(vendorPublicId, storeId);

        vendor.deactivate();
    }

    private Vendor getValidVendor(UUID vendorPublicId, Long storeId) {
        Vendor vendor = vendorRepository.findByVendorPublicId(vendorPublicId)
                .orElseThrow(() -> new VendorException(VendorErrorCode.VENDOR_NOT_FOUND));

        validateVendorBelongsToStore(vendor, storeId);

        return vendor;
    }

    private void validateVendorBelongsToStore(Vendor vendor, Long storeId) {
        if (!vendor.getStore().getStoreId().equals(storeId)) {
            throw new VendorException(VendorErrorCode.VENDOR_NOT_FOUND);
        }
    }
}
