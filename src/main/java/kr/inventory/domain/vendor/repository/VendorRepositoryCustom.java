package kr.inventory.domain.vendor.repository;

import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface VendorRepositoryCustom {

	Page<Vendor> findByStoreIdWithFilters(Long storeId, VendorStatus status, String search, Pageable pageable);

	Optional<Vendor> findMostSimilarVendor(Long storeId, String name);
}
