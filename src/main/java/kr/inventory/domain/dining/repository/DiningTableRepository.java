package kr.inventory.domain.dining.repository;

import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.entity.enums.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    Optional<DiningTable> findByStore_StoreIdAndTablePublicIdAndStatusNot(Long storeId, UUID tablePublicId, TableStatus status);

    Optional<DiningTable> findByStore_StorePublicIdAndTablePublicIdAndStatusNot(UUID storePublicId, UUID tablePublicId, TableStatus status);

    List<DiningTable> findAllByStore_StoreIdAndStatusNot(Long storeId, TableStatus status);

    List<DiningTable> findAllByStore_StoreIdAndTablePublicIdInAndStatusNot(Long storeId, List<UUID> tablePublicIds, TableStatus status);
}