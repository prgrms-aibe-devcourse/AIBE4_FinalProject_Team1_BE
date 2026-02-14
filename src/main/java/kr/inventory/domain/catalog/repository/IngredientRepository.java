package kr.inventory.domain.catalog.repository;

import kr.inventory.domain.catalog.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    List<Ingredient> findAllByStoreIdAndIdIn(Long storeId, List<Long> ingredientIds);
}
