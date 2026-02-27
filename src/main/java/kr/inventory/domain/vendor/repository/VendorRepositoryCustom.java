package kr.inventory.domain.vendor.repository;

import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;

import java.util.List;
import java.util.Optional;

public interface VendorRepositoryCustom {

	List<Vendor> findByStoreIdWithFilters(Long storeId, VendorStatus status);

	Optional<Vendor> findMostSimilarVendor(Long storeId, String name);
}
