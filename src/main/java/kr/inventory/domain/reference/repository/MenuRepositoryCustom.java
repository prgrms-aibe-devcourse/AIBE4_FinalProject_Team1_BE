package kr.inventory.domain.reference.repository;

import java.util.UUID;

public interface MenuRepositoryCustom {
    boolean existsActiveMenuUsingIngredient(Long storeId, UUID ingredientPublicId);
}
