package kr.inventory.domain.dining.repository;

import kr.inventory.domain.dining.entity.TableSession;
import kr.inventory.domain.dining.entity.enums.TableSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {
    // sessionToken 검증용 (추후 주문/결제에서 사용)
    Optional<TableSession> findBySessionTokenHashAndStatus(String sessionTokenHash, TableSessionStatus status);

    // 같은 테이블에 ACTIVE 세션이 여러 개 생기지 않게 정리하고 싶을 때 사용
    List<TableSession> findAllByTable_TableIdAndStatus(Long tableId, TableSessionStatus status);

    // 만료 처리 배치용
    List<TableSession> findAllByStatusAndExpiresAtBefore(TableSessionStatus status, OffsetDateTime now);
}
