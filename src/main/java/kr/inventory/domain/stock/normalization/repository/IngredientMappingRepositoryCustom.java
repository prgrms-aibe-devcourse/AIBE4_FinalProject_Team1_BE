package kr.inventory.domain.stock.normalization.repository;

import kr.inventory.domain.reference.entity.IngredientMapping;

import java.util.Optional;

public interface IngredientMappingRepositoryCustom {

    Optional<IngredientMapping> findActiveStoreLevelMapping(Long storeId, String normalizedRawKey);
}
