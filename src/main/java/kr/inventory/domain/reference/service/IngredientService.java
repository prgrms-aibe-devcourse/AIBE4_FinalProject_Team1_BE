package kr.inventory.domain.reference.service;

import kr.inventory.domain.reference.controller.dto.request.IngredientCreateRequest;
import kr.inventory.domain.reference.controller.dto.response.IngredientResponse;
import kr.inventory.domain.reference.controller.dto.request.IngredientUpdateRequest;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.exception.IngredientErrorCode;
import kr.inventory.domain.reference.exception.IngredientException;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.normalization.model.InboundSpecExtractor;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final StoreRepository storeRepository;
    private final StoreAccessValidator storeAccessValidator;
    private final InboundSpecExtractor specExtractor;

    @Transactional
    public IngredientResponse createIngredient(Long userId, UUID storePublicId, IngredientCreateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        // 입고 원문에서 규격 추출 시도
        Optional<InboundSpecExtractor.Spec> spec = specExtractor.extract(request.name());

        Ingredient ingredient;
        // 규격 추출 성공: baseName, unit, unitSize 사용
        // 규격 추출 실패: 기존 방식대로 생성 (unitSize=null)
        ingredient = spec.map(value -> Ingredient.create(
                store,
                value.baseName(),
                value.unit(),
                request.lowStockThreshold(),
                value.unitSize()
        )).orElseGet(() -> Ingredient.create(
                store,
                request.name(),
                request.unit(),
                request.lowStockThreshold()
        ));

        ingredientRepository.save(ingredient);
        return IngredientResponse.from(ingredient);
    }

    public List<IngredientResponse> getIngredients(Long userId, UUID storePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        return ingredientRepository.findAllByStoreStoreIdAndStatusNot(storeId, IngredientStatus.DELETED).stream()
                .map(IngredientResponse::from)
                .toList();
    }

    public IngredientResponse getIngredient(Long userId, UUID storePublicId, UUID ingredientPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Ingredient ingredient = getValidIngredient(ingredientPublicId, storeId);
        return IngredientResponse.from(ingredient);
    }

    @Transactional
    public IngredientResponse updateIngredient(Long userId, UUID storePublicId, UUID ingredientPublicId, IngredientUpdateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Ingredient ingredient = getValidIngredient(ingredientPublicId, storeId);
        ingredient.update(request.name(), request.unit(), request.lowStockThreshold(), request.status());
        return IngredientResponse.from(ingredient);
    }

    @Transactional
    public void deleteIngredient(Long userId, UUID storePublicId, UUID ingredientPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Ingredient ingredient = getValidIngredient(ingredientPublicId, storeId);
        ingredient.delete();
    }

    private Ingredient getValidIngredient(UUID ingredientPublicId, Long storeId) {
        return ingredientRepository
                .findByIngredientPublicIdAndStoreStoreIdAndStatusNot(
                        ingredientPublicId,
                        storeId,
                        IngredientStatus.DELETED
                )
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));
    }
}
