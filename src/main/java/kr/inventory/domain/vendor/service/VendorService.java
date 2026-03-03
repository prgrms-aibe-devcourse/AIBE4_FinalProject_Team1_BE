package kr.inventory.domain.vendor.service;

import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.vendor.controller.dto.request.VendorCreateRequest;
import kr.inventory.domain.vendor.controller.dto.response.VendorResponse;
import kr.inventory.domain.vendor.controller.dto.request.VendorUpdateRequest;
import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.vendor.exception.VendorErrorCode;
import kr.inventory.domain.vendor.exception.VendorException;
import kr.inventory.domain.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public List<VendorResponse> getVendorsByStore(Long userId, UUID storePublicId, VendorStatus status) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        List<Vendor> vendors = vendorRepository.findByStoreIdWithFilters(storeId, status);
        return vendors.stream()
                .map(VendorResponse::from)
                .toList();
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
