package kr.inventory.domain.dining.repository;

import kr.inventory.domain.dining.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    Optional<DiningTable> findByStore_StorePublicIdAndTablePublicId(UUID storePublicId, UUID tablePublicId);
}
