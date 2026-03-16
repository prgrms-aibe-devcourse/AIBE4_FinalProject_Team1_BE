package kr.inventory.domain.reference.service;

import kr.inventory.domain.reference.controller.dto.request.IngredientCreateRequest;
import kr.inventory.domain.reference.controller.dto.request.IngredientSearchRequest;
import kr.inventory.domain.reference.controller.dto.response.IngredientResponse;
import kr.inventory.domain.reference.controller.dto.request.IngredientUpdateRequest;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import kr.inventory.domain.reference.exception.IngredientErrorCode;
import kr.inventory.domain.reference.exception.IngredientException;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.normalization.model.InboundSpecExtractor;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        Optional<InboundSpecExtractor.Spec> spec = specExtractor.extract(request.name());

        Ingredient ingredient = spec.map(value -> {
                    BigDecimal unitSize = resolveIngredientUnitSize(value);
                    if (unitSize == null) {
                        return Ingredient.create(
                                store,
                                value.baseName(),
                                value.unit(),
                                request.lowStockThreshold()
                        );
                    }

                    return Ingredient.create(
                            store,
                            value.baseName(),
                            value.unit(),
                            request.lowStockThreshold(),
                            unitSize
                    );
                })
                .orElseGet(() -> Ingredient.create(
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

    public PageResponse<IngredientResponse> getIngredientsPage(
            Long userId,
            UUID storePublicId,
            IngredientSearchRequest searchRequest,
            Pageable pageable
    ){
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        Page<IngredientResponse> page = ingredientRepository
                .searchByStoreIdAndName(storeId, searchRequest.name(), IngredientStatus.DELETED, pageable)
                .map(IngredientResponse::from);

        return PageResponse.from(page);
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

    private BigDecimal resolveIngredientUnitSize(InboundSpecExtractor.Spec spec) {
        if (spec == null || spec.unit() == IngredientUnit.EA) {
            return null;
        }

        if (spec.unitSize() == null || spec.unitSize().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return spec.unitSize();
    }
}
