package kr.inventory.domain.sales.service;

import com.fasterxml.jackson.databind.JsonNode;
import kr.inventory.domain.reference.entity.Menu;
import kr.inventory.domain.reference.entity.enums.MenuStatus;
import kr.inventory.domain.reference.repository.MenuRepository;
import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.entity.TableSession;
import kr.inventory.domain.dining.entity.enums.TableSessionStatus;
import kr.inventory.domain.dining.repository.TableSessionRepository;
import kr.inventory.domain.dining.service.TokenSupport;
import kr.inventory.domain.sales.controller.dto.request.SalesOrderCreateRequest;
import kr.inventory.domain.sales.controller.dto.request.SalesOrderItemRequest;
import kr.inventory.domain.sales.controller.dto.response.SalesOrderResponse;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
import kr.inventory.domain.sales.exception.SalesOrderErrorCode;
import kr.inventory.domain.sales.exception.SalesOrderException;
import kr.inventory.domain.sales.repository.SalesOrderItemRepository;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.stock.service.StockService;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final TableSessionRepository tableSessionRepository;
    private final MenuRepository menuRepository;
    private final StockService stockService;
    private final StoreAccessValidator storeAccessValidator;

    @Transactional
    public SalesOrderResponse createOrder(
            String sessionToken,
            String idempotencyKey,
            SalesOrderCreateRequest request
    ) {
        // 1. sessionToken 해시 → TableSession 조회
        String sessionTokenHash = TokenSupport.sha256Hex(sessionToken);
        TableSession session = tableSessionRepository
                .findBySessionTokenHashAndStatus(sessionTokenHash, TableSessionStatus.ACTIVE)
                .orElseThrow(() -> new SalesOrderException(SalesOrderErrorCode.INVALID_SESSION));

        // 2. 세션 만료 확인
        if (session.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new SalesOrderException(SalesOrderErrorCode.SESSION_EXPIRED);
        }

        // 3. TableSession → DiningTable → Store 추출 (publicId → id 변환!)
        DiningTable table = session.getTable();
        Store store = table.getStore();
        Long storeId = store.getStoreId();  // publicId → id!

        // 4. Idempotency Key 중복 확인 (중복 결제 방지!)
        salesOrderRepository.findByStoreStoreIdAndIdempotencyKey(storeId, idempotencyKey)
                .ifPresent(existingOrder -> {
                    throw new SalesOrderException(SalesOrderErrorCode.DUPLICATE_IDEMPOTENCY_KEY);
                });

        // 5. menuPublicId → Menu 조회
        List<UUID> menuPublicIds = request.items().stream()
                .map(SalesOrderItemRequest::menuPublicId)
                .toList();

        List<Menu> menus = menuRepository.findByMenuPublicIdIn(menuPublicIds);

        if (menus.size() != menuPublicIds.size()) {
            throw new SalesOrderException(SalesOrderErrorCode.MENU_NOT_FOUND);
        }

        // 6. Menu 상태 검증 (ACTIVE만!)
        menus.forEach(menu -> {
            if (menu.getStatus() != MenuStatus.ACTIVE) {
                throw new SalesOrderException(SalesOrderErrorCode.MENU_NOT_ACTIVE);
            }
        });

        // 7. menuPublicId → Menu Map (빠른 조회)
        Map<UUID, Menu> menuMap = menus.stream()
                .collect(Collectors.toMap(Menu::getMenuPublicId, Function.identity()));

        // 8. 주문 생성
        SalesOrder salesOrder = SalesOrder.create(
                store,
                table,
                session,
                idempotencyKey,
                SalesOrderType.DINE_IN
        );
        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        // 9. 주문 항목 생성 (가격 스냅샷!)
        List<SalesOrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SalesOrderItemRequest itemRequest : request.items()) {
            Menu menu = menuMap.get(itemRequest.menuPublicId());
            SalesOrderItem item = SalesOrderItem.create(savedOrder, menu, itemRequest.quantity());
            items.add(item);
            totalAmount = totalAmount.add(item.getSubtotal());
        }

        salesOrderItemRepository.saveAll(items);
        savedOrder.updateTotalAmount(totalAmount);

        // 10. 재고 차감 (실패 시 전체 롤백!)
        Map<Long, BigDecimal> usageMap = calculateIngredientUsage(items);

        if (!usageMap.isEmpty()) {
            Map<Long, BigDecimal> shortageMap = stockService.deductStockWithFEFO(storeId, usageMap);

            if (!shortageMap.isEmpty()) {
                throw new SalesOrderException(SalesOrderErrorCode.INSUFFICIENT_STOCK);
            }
        }

        // 11. COMPLETED 상태 설정
        savedOrder.updateCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        savedOrder.markAsStockProcessed();

        return SalesOrderResponse.from(savedOrder, items);
    }

    public SalesOrderResponse getOrder(UUID orderPublicId, Long userId, UUID storePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        SalesOrder order = salesOrderRepository.findByOrderPublicIdWithItems(orderPublicId, storeId)
                .orElseThrow(() -> new SalesOrderException(SalesOrderErrorCode.SALES_ORDER_NOT_FOUND));

        List<SalesOrderItem> items = salesOrderItemRepository
                .findBySalesOrderSalesOrderId(order.getSalesOrderId());

        return SalesOrderResponse.from(order, items);
    }

    public List<SalesOrderResponse> getStoreOrders(Long userId, UUID storePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        return salesOrderRepository.findStoreOrders(storeId);
    }

    /**
     * 환불 처리 (관리자용)
     */
    @Transactional
    public SalesOrderResponse refundOrder(UUID orderPublicId, Long userId, UUID storePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        SalesOrder order = salesOrderRepository.findByOrderPublicIdAndStoreStoreId(orderPublicId, storeId)
                .orElseThrow(() -> new SalesOrderException(SalesOrderErrorCode.SALES_ORDER_NOT_FOUND));

        if (order.getStatus() != SalesOrderStatus.COMPLETED) {
            throw new SalesOrderException(SalesOrderErrorCode.ORDER_NOT_REFUNDABLE);
        }

        if (order.getStatus() == SalesOrderStatus.REFUNDED) {
            throw new SalesOrderException(SalesOrderErrorCode.ORDER_ALREADY_REFUNDED);
        }

        order.updateStatus(SalesOrderStatus.REFUNDED);
        order.updateRefundedAt(OffsetDateTime.now(ZoneOffset.UTC));

        List<SalesOrderItem> items = salesOrderItemRepository
                .findBySalesOrderSalesOrderId(order.getSalesOrderId());

        return SalesOrderResponse.from(order, items);
    }

    /**
     * 재료별 필요 수량 계산
     */
    private Map<Long, BigDecimal> calculateIngredientUsage(List<SalesOrderItem> items) {
        Map<Long, BigDecimal> usageMap = new HashMap<>();

        for (SalesOrderItem item : items) {
            Menu menu = item.getMenu();
            JsonNode ingredientsJson = menu.getIngredientsJson();

            if (ingredientsJson == null || ingredientsJson.isEmpty()) {
                continue;
            }

            if (ingredientsJson.has("ingredients") && ingredientsJson.get("ingredients").isArray()) {
                for (JsonNode ingredientNode : ingredientsJson.get("ingredients")) {
                    Long ingredientId = ingredientNode.get("ingredientId").asLong();
                    BigDecimal quantity = new BigDecimal(ingredientNode.get("quantity").asText());

                    BigDecimal totalQuantity = quantity.multiply(BigDecimal.valueOf(item.getQuantity()));

                    usageMap.merge(ingredientId, totalQuantity, BigDecimal::add);
                }
            }
        }

        return usageMap;
    }
}




































































































































































































































































































































