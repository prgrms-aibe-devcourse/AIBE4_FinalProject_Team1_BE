package kr.inventory.domain.reference.service;

import kr.inventory.domain.reference.controller.dto.IngredientCreateRequest;
import kr.inventory.domain.reference.controller.dto.IngredientResponse;
import kr.inventory.domain.reference.controller.dto.IngredientUpdateRequest;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.exception.IngredientErrorCode;
import kr.inventory.domain.reference.exception.IngredientException;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final StoreRepository storeRepository;
    private final StoreAccessValidator storeAccessValidator;

    @Transactional
    public UUID createIngredient(Long userId, UUID storePublicId, IngredientCreateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        Ingredient ingredient = Ingredient.create(store, request.name(), request.unit(), request.lowStockThreshold());
        ingredientRepository.save(ingredient);
        return ingredient.getIngredientPublicId();
    }

    public List<IngredientResponse> getIngredients(Long userId, UUID storePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        return ingredientRepository.findAllByStoreStoreId(storeId).stream()
                .map(IngredientResponse::from)
                .toList();
    }

    public IngredientResponse getIngredient(Long userId, UUID storePublicId, UUID ingredientPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        Ingredient ingredient = getValidIngredient(ingredientPublicId, storeId);

        return IngredientResponse.from(ingredient);
    }

    @Transactional
    public void updateIngredient(Long userId, UUID storePublicId, UUID ingredientPublicId, IngredientUpdateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        Ingredient ingredient = getValidIngredient(ingredientPublicId, storeId);

        ingredient.update(request.name(), request.unit(), request.lowStockThreshold(), request.status());
    }

    @Transactional
    public void deleteIngredient(Long userId, UUID storePublicId, UUID ingredientPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        Ingredient ingredient = getValidIngredient(ingredientPublicId, storeId);

        ingredientRepository.delete(ingredient);
    }

    private Ingredient getValidIngredient(UUID ingredientPublicId, Long storeId) {
        Ingredient ingredient = ingredientRepository.findByIngredientPublicId(ingredientPublicId)
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));

        validateIngredientBelongsToStore(ingredient, storeId);

        return ingredient;
    }

    private void validateIngredientBelongsToStore(Ingredient ingredient, Long storeId){
        if (!ingredient.getStore().getStoreId().equals(storeId)) {
            throw new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND);
        }
    }
}
