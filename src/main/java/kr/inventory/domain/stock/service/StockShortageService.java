package kr.inventory.domain.stock.service;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.stock.controller.dto.request.StockShortageSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.StockShortageGroupResponse;
import kr.inventory.domain.stock.controller.dto.response.StockShortageItemResponse;
import kr.inventory.domain.stock.entity.StockShortage;
import kr.inventory.domain.stock.repository.StockShortageRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockShortageService {

    private final StockShortageRepository stockShortageRepository;
    private final IngredientRepository ingredientRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final StoreAccessValidator storeAccessValidator;

    @Transactional
    public void recordShortages(Long storeId, Long salesOrderId,
                                Map<Long, BigDecimal> usageMap,
                                Map<Long, BigDecimal> shortageMap) {

        List<StockShortage> shortages = shortageMap.entrySet().stream()
                .map(entry -> {
                    Long ingredientId = entry.getKey();
                    BigDecimal shortageAmount = entry.getValue();
                    BigDecimal requiredAmount = usageMap.get(ingredientId);

                    return StockShortage.createPending(
                            storeId,
                            salesOrderId,
                            ingredientId,
                            requiredAmount,
                            shortageAmount
                    );
                })
                .toList();

        stockShortageRepository.saveAll(shortages);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockShortageGroupResponse> getShortagesGroupedByOrder(
            Long userId,
            UUID storePublicId,
            StockShortageSearchRequest searchRequest,
            Pageable pageable
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        Page<Long> salesOrderIdsPage = stockShortageRepository.findDistinctSalesOrderIdsByStoreId(
                storeId,
                searchRequest,
                pageable
        );

        List<Long> salesOrderIds = salesOrderIdsPage.getContent();

        if (salesOrderIds.isEmpty()) {
            return new PageResponse<>(
                    Collections.emptyList(),
                    salesOrderIdsPage.getNumber(),
                    salesOrderIdsPage.getSize(),
                    salesOrderIdsPage.getTotalElements(),
                    salesOrderIdsPage.getTotalPages(),
                    salesOrderIdsPage.hasNext()
            );
        }

        List<StockShortage> shortages = stockShortageRepository.findAllBySalesOrderIds(
                salesOrderIds,
                searchRequest
        );

        Map<Long, SalesOrder> salesOrderMap = salesOrderRepository.findAllById(salesOrderIds).stream()
                .collect(Collectors.toMap(SalesOrder::getSalesOrderId, Function.identity()));

        List<Long> ingredientIds = shortages.stream()
                .map(StockShortage::getIngredientId)
                .distinct()
                .toList();

        Map<Long, Ingredient> ingredientMap = ingredientRepository.findAllById(ingredientIds).stream()
                .collect(Collectors.toMap(Ingredient::getIngredientId, Function.identity()));

        Map<Long, List<StockShortage>> groupedByOrder = shortages.stream()
                .collect(Collectors.groupingBy(StockShortage::getSalesOrderId));

        List<StockShortageGroupResponse> content = salesOrderIds.stream()
                .map(orderId -> {
                    SalesOrder order = salesOrderMap.get(orderId);

                    List<StockShortageItemResponse> itemResponses = groupedByOrder
                            .getOrDefault(orderId, Collections.emptyList())
                            .stream()
                            .map(shortage -> {
                                Ingredient ingredient = ingredientMap.get(shortage.getIngredientId());
                                return StockShortageItemResponse.from(shortage, ingredient);
                            })
                            .toList();

                    return StockShortageGroupResponse.from(order, itemResponses);
                })
                .toList();

        return new PageResponse<>(
                content,
                salesOrderIdsPage.getNumber(),
                salesOrderIdsPage.getSize(),
                salesOrderIdsPage.getTotalElements(),
                salesOrderIdsPage.getTotalPages(),
                salesOrderIdsPage.hasNext()
        );
    }
}
