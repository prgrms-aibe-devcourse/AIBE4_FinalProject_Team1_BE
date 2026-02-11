package kr.inventory.domain.stock.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.repository.SalesOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TheoreticalUsageService {
    private record RecipeItem(
            Long ingredientId,
            BigDecimal qty,
            String unit
    ){}

    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ObjectMapper objectMapper;

    public Map<Long, BigDecimal> calculateOrderUsage(SalesOrder salesOrder) {
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderId(salesOrder.getSalesOrderId());

        Map<Long, BigDecimal> totalUsageMap = getTotalUsage(items);
        return totalUsageMap;
    }

    private Map<Long, BigDecimal> getTotalUsage(List<SalesOrderItem> items){
        Map<Long, BigDecimal> totalUsageMap = new HashMap<>();

        for(SalesOrderItem item : items){
            if(item.getMenu() == null || item.getMenu().getIngredientsJson() == null){
                continue;
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
        try {
            return objectMapper.convertValue(jsonSource, new TypeReference<List<RecipeItem>>() {});
        } catch(Exception e){
            return Collections.emptyList();
        }
    }
}
