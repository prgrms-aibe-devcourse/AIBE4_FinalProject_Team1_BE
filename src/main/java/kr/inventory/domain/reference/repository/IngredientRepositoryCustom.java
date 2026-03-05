package kr.inventory.domain.reference.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;

public interface IngredientRepositoryCustom {
	Optional<Ingredient> findMostSimilarIngredient(Long storeId, String productName);

	List<IngredientCandidate> findTopNSimilarIngredients(Long storeId, String normalizedQuery, int limit);

	Optional<Ingredient> findByIngredientPublicIdAndStatusNotWithStore(UUID publicId, IngredientStatus status);

	record IngredientCandidate(Ingredient ingredient, Double score) {}
}
