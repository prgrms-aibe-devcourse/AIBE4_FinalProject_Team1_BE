package kr.inventory.domain.catalog.repository;

import kr.inventory.domain.catalog.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
