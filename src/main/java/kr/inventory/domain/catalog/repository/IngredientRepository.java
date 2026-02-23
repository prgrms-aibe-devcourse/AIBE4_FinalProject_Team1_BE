package kr.inventory.domain.catalog.repository;

import kr.inventory.domain.catalog.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    List<Ingredient> findAllByStoreStoreIdAndIngredientIdIn(Long storeId, List<Long> ingredientIds);
    List<Ingredient> findAllByStoreStoreId(Long storeId);
    Optional<Ingredient> findByPublicId(UUID publicId);
}
