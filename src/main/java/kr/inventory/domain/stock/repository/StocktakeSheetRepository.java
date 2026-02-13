package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StocktakeSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StocktakeSheetRepository extends JpaRepository<StocktakeSheet,Long> {
    Optional<StocktakeSheet> findBySheet(StocktakeSheet sheet);
}
