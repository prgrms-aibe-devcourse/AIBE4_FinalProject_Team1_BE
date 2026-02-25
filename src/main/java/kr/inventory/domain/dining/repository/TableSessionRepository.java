package kr.inventory.domain.dining.repository;

import kr.inventory.domain.dining.entity.TableSession;
import kr.inventory.domain.dining.entity.enums.TableSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {

    Optional<TableSession> findBySessionTokenHashAndStatus(String sessionTokenHash, TableSessionStatus status);

    Optional<TableSession> findByTable_Store_StorePublicIdAndSessionTokenHashAndStatus(
            UUID storePublicId,
            String sessionTokenHash,
            TableSessionStatus status
    );
}
