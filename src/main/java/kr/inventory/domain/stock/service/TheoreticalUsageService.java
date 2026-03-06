package kr.inventory.domain.stock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.Menu;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.repository.SalesOrderItemRepository;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TheoreticalUsageService {
    record RecipeItem(
            UUID ingredientPublicId,
            BigDecimal qty,
            String unit
    ){}

    private final SalesOrderItemRepository salesOrderItemRepository;
    private final IngredientRepository ingredientRepository;
    private final ObjectMapper objectMapper;

    public Map<Long, BigDecimal> calculateOrderUsage(Long storeId, List<SalesOrderItem> items) {
        return getTotalUsage(storeId, items);
    }

    private Map<Long, BigDecimal> getTotalUsage(Long storeId, List<SalesOrderItem> items){
        Map<Long, BigDecimal> totalUsageMap = new HashMap<>();

        for(SalesOrderItem item : items){
            List<RecipeItem> recipes = getRecipes(item);
            BigDecimal orderQuantity = BigDecimal.valueOf(item.getQuantity());

            List<UUID> publicIds = recipes.stream()
                    .map(RecipeItem::ingredientPublicId)
                    .distinct()
                    .toList();

            Map<UUID, Long> publicToId = ingredientRepository
                    .findAllByStoreStoreIdAndIngredientPublicIdInAndStatusNot(storeId, publicIds, IngredientStatus.DELETED)
                    .stream()
                    .collect(Collectors.toMap(Ingredient::getIngredientPublicId, Ingredient::getIngredientId));

            for (RecipeItem recipe : recipes) {
                Long ingredientId = publicToId.get(recipe.ingredientPublicId());
                if (ingredientId == null) {
                    throw new StockException(StockErrorCode.RECIPE_NOT_FOUND);
                }

                BigDecimal usageAmount = recipe.qty().multiply(orderQuantity);
                totalUsageMap.merge(ingredientId, usageAmount, BigDecimal::add);
            }
        }
        return totalUsageMap;
    }

    private List<RecipeItem> getRecipes(SalesOrderItem item){
        return Optional.ofNullable(item.getMenu())
                .map(Menu::getIngredientsJson)
                .map(this::parseRecipe)
                .orElseThrow(() -> new StockException(StockErrorCode.RECIPE_NOT_FOUND));
    }

    private List<RecipeItem> parseRecipe(Object jsonSource){
        try {
            return objectMapper.convertValue(jsonSource, new TypeReference<List<RecipeItem>>() {});
        } catch(Exception e){
            throw new StockException(StockErrorCode.RECIPE_PARSE_ERROR);
        }
    }
}
