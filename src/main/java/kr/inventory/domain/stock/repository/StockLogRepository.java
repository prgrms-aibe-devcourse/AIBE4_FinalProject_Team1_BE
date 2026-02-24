package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockLogRepository extends JpaRepository<StockLog, Long> {
}
