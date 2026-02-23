package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.entity.StocktakeSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StocktakeSheetRepository extends JpaRepository<StocktakeSheet,Long>, StocktakeSheetRepositoryCustom {
    List<StocktakeSheet> findAllByStoreIdOrderByCreatedAtDesc(Long storeId);
}