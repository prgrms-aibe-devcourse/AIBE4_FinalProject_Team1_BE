package kr.inventory.domain.reference.repository;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientRepositoryCustom {
	Optional<Ingredient> findMostSimilarIngredient(Long storeId, String productName);

	List<IngredientCandidate> findTopNSimilarIngredients(Long storeId, String normalizedQuery, int limit);

	Optional<Ingredient> findByIngredientPublicIdAndStatusNotWithStore(UUID publicId, IngredientStatus status);

    Page<Ingredient> searchByStoreIdAndName(Long storeId, String name, IngredientStatus excludedStatus, Pageable pageable);

	record IngredientCandidate(Ingredient ingredient, Double score) {}
}
