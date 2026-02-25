package kr.inventory.domain.dining.repository;

import kr.inventory.domain.dining.entity.TableQr;
import kr.inventory.domain.dining.entity.enums.TableQrStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TableQrRepository extends JpaRepository<TableQr, Long> {

    Optional<TableQr> findByTable_Store_StoreIdAndQrPublicId(Long storeId, UUID qrPublicId);

    Optional<TableQr> findTopByTable_TableIdOrderByRotationVersionDesc(Long tableId);

    Optional<TableQr> findByTable_Store_StorePublicIdAndEntryTokenHashAndStatus(
            UUID storePublicId,
            String entryTokenHash,
            TableQrStatus status
    );
}
