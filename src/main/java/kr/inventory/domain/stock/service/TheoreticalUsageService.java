package kr.inventory.domain.stock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.repository.SalesOrderItemRepository;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TheoreticalUsageService {
    record RecipeItem(
            Long ingredientId,
            BigDecimal qty,
            String unit
    ){}

    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ObjectMapper objectMapper;

    public Map<Long, BigDecimal> calculateOrderUsage(SalesOrder salesOrder) {
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderId(salesOrder.getSalesOrderId());

        return getTotalUsage(items);
    }

    private Map<Long, BigDecimal> getTotalUsage(List<SalesOrderItem> items){
        Map<Long, BigDecimal> totalUsageMap = new HashMap<>();

        for(SalesOrderItem item : items){
            if (item.getMenu() == null) {
                throw new StockException(StockErrorCode.RECIPE_NOT_FOUND);
            }

            List<RecipeItem> recipes = parseRecipe(item.getMenu().getIngredientsJson());
            BigDecimal orderQuantity = BigDecimal.valueOf(item.getQuantity());

            for (RecipeItem recipe : recipes) {
                BigDecimal usageAmount = recipe.qty().multiply(orderQuantity);
                totalUsageMap.merge(recipe.ingredientId(), usageAmount, BigDecimal::add);
            }
        }
        return totalUsageMap;
    }

    private List<RecipeItem> parseRecipe(Object jsonSource){
        if (jsonSource == null) {
            throw new StockException(StockErrorCode.RECIPE_NOT_FOUND);
        }

        try {
            return objectMapper.convertValue(jsonSource, new TypeReference<List<RecipeItem>>() {});
        } catch(Exception e){
            throw new StockException(StockErrorCode.RECIPE_PARSE_ERROR);
        }
    }
}
