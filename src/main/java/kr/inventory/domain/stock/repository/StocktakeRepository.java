package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.Stocktake;
import kr.inventory.domain.stock.entity.StocktakeSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StocktakeRepository extends JpaRepository<Stocktake,Long> {
    List<Stocktake> findBySheet(StocktakeSheet sheet);
}
