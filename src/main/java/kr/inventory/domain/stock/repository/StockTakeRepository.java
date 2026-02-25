package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StockTake;
import kr.inventory.domain.stock.entity.StockTakeSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTakeRepository extends JpaRepository<StockTake,Long> {
    List<StockTake> findBySheet(StockTakeSheet sheet);
}
