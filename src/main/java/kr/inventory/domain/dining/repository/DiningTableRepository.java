package kr.inventory.domain.dining.repository;

import kr.inventory.domain.dining.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    Optional<DiningTable> findByStore_StoreIdAndTablePublicId(Long storeId, UUID tablePublicId);
    List<DiningTable> findAllByStore_StoreId(Long storeId);
}