package kr.inventory.ai.stock.service;

import kr.inventory.ai.stock.tool.dto.request.GetStockBatchesByIngredientToolRequest;
import kr.inventory.ai.stock.tool.dto.response.GetStockBatchesByIngredientToolResponse;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.exception.IngredientErrorCode;
import kr.inventory.domain.reference.exception.IngredientException;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockBatchAiQueryService {

    private final StoreAccessValidator storeAccessValidator;
    private final IngredientRepository ingredientRepository;
    private final IngredientStockBatchRepository ingredientStockBatchRepository;

    public GetStockBatchesByIngredientToolResponse getStockBatchesByIngredient(
            Long userId,
            UUID storePublicId,
            GetStockBatchesByIngredientToolRequest request
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        Ingredient ingredient = ingredientRepository.findOneByKeyword(
                        storeId,
                        request.normalizedKeyword()
                )
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));

        List<IngredientStockBatch> batches = ingredientStockBatchRepository.findOpenBatchesByIngredient(
                storeId,
                ingredient.getIngredientId()
        );

        return GetStockBatchesByIngredientToolResponse.of(ingredient, batches);
    }
}