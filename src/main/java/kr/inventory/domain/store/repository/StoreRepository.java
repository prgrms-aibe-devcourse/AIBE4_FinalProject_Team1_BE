package kr.inventory.domain.store.repository;

import kr.inventory.domain.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, Long> {

    boolean existsByBusinessRegistrationNumber(String businessRegistrationNumber);

    Optional<Store> findByStorePublicId(UUID storePublicId);

}
