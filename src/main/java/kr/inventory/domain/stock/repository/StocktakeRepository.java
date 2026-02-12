package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.Stocktake;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StocktakeRepository extends JpaRepository<Stocktake,Long> {
}
