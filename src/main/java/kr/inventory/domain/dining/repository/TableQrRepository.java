package kr.inventory.domain.dining.repository;

import kr.inventory.domain.dining.entity.TableQr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TableQrRepository extends JpaRepository<TableQr, Long>, TableQrRepositoryCustom {
    Optional<TableQr> findTopByTable_TableIdOrderByRotationVersionDesc(Long tableId);

    Optional<TableQr> findActiveQrByTable_TableId(Long tableId);

    List<TableQr> findAllByTable_Store_StoreId(Long storeId);
}