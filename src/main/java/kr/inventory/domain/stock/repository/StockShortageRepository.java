package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockShortage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockShortageRepository extends JpaRepository<StockShortage, Long> {
}
