package kr.inventory.domain.reference.repository;

import kr.inventory.domain.reference.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, Long>, VendorRepositoryCustom {

    boolean existsByStoreStoreIdAndName(Long storeId, String name);
    Optional<Vendor> findByVendorPublicId(UUID vendorPublicId);
}
