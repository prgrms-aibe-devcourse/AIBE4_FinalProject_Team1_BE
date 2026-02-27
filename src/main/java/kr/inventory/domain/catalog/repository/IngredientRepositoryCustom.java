package kr.inventory.domain.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.inventory.domain.catalog.entity.Ingredient;

public interface IngredientRepositoryCustom {
	Optional<Ingredient> findMostSimilarIngredient(Long storeId, String productName);

}
