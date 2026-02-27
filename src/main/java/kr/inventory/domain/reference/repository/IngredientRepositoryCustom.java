package kr.inventory.domain.reference.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.inventory.domain.reference.entity.Ingredient;

public interface IngredientRepositoryCustom {
	Optional<Ingredient> findMostSimilarIngredient(Long storeId, String productName);

}
