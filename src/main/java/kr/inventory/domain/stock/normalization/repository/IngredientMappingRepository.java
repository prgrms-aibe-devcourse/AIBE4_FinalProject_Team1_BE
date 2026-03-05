package kr.inventory.domain.stock.normalization.repository;

import kr.inventory.domain.reference.entity.IngredientMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientMappingRepository extends JpaRepository<IngredientMapping, Long>, IngredientMappingRepositoryCustom {
}
