package kr.inventory.domain.vendor.repository;

import kr.inventory.domain.vendor.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long>, VendorRepositoryCustom {

    boolean existsByStoreStoreIdAndName(Long storeId, String name);
}
