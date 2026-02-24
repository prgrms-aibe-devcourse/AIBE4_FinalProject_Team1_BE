package kr.inventory.domain.vendor.repository;

import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;

import java.util.List;

public interface VendorRepositoryCustom {

    List<Vendor> findByStoreIdWithFilters(Long storeId, VendorStatus status);
}
