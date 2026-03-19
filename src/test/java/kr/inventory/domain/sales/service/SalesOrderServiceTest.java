package kr.inventory.domain.sales.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.entity.TableSession;
import kr.inventory.domain.dining.entity.enums.TableSessionStatus;
import kr.inventory.domain.dining.repository.TableSessionRepository;
import kr.inventory.domain.reference.entity.Menu;
import kr.inventory.domain.reference.entity.enums.MenuStatus;
import kr.inventory.domain.reference.repository.MenuRepository;
import kr.inventory.domain.sales.controller.dto.request.SalesOrderCreateRequest;
import kr.inventory.domain.sales.controller.dto.request.SalesOrderItemRequest;
import kr.inventory.domain.sales.controller.dto.request.SalesOrderSearchRequest;
import kr.inventory.domain.sales.controller.dto.response.SalesOrderResponse;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
import kr.inventory.global.common.PageResponse;
import kr.inventory.domain.sales.exception.SalesOrderErrorCode;
import kr.inventory.domain.sales.exception.SalesOrderException;
import kr.inventory.domain.sales.repository.SalesOrderItemRepository;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.stock.service.StockManagerFacade;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.service.StoreAccessValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("주문 서비스 테스트")
class SalesOrderServiceTest {

    @InjectMocks
    private SalesOrderService salesOrderService;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;

    @Mock
    private TableSessionRepository tableSessionRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private StockManagerFacade stockManagerFacade;

    @Mock
    private StoreAccessValidator storeAccessValidator;

    private ObjectMapper objectMapper;
    private Store store;
    private DiningTable table;
    private TableSession session;
    private Menu menu1;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        store = Store.create("테스트 매장", "1234567890");
        ReflectionTestUtils.setField(store, "storeId", 1L);

        table = DiningTable.create(store, "T1");
        ReflectionTestUtils.setField(table, "tableId", 1L);

        session = TableSession.create(
                table, null, "hashed-token",
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(2)
        );
        ReflectionTestUtils.setField(session, "tableSessionId", 1L);

        menu1 = Menu.create(store, "김치찌개", new BigDecimal("8000"), null);
        ReflectionTestUtils.setField(menu1, "menuId", 1L);
    }

    @Test
    @DisplayName("주문 생성 성공 - ingredientsJson 없음 (재고 차감 로직 호출 확인)")
    void givenValidRequest_whenCreateOrder_thenSuccess() {
        // given
        String sessionToken = "test-token";
        String idempotencyKey = "idempotency-123";
        SalesOrderItemRequest item1 = new SalesOrderItemRequest(menu1.getMenuPublicId(), 2);
        SalesOrderCreateRequest request = new SalesOrderCreateRequest(List.of(item1));

        given(tableSessionRepository.findBySessionTokenHashAndStatus(anyString(), eq(TableSessionStatus.ACTIVE)))
                .willReturn(Optional.of(session));

        given(salesOrderRepository.findByStoreStoreIdAndIdempotencyKey(anyLong(), eq(idempotencyKey)))
                .willReturn(Optional.empty());

        given(menuRepository.findByMenuPublicIdInAndStatusNot(
                ArgumentMatchers.<UUID>anyList(),
                eq(MenuStatus.DELETED)
        )).willReturn(List.of(menu1));

        SalesOrder savedOrder = SalesOrder.create(store, table, session, idempotencyKey, SalesOrderType.DINE_IN);
        ReflectionTestUtils.setField(savedOrder, "salesOrderId", 100L);

        given(salesOrderRepository.save(any(SalesOrder.class)))
                .willReturn(savedOrder);

        given(salesOrderItemRepository.saveAll(anyList()))
                .willReturn(List.of());

        // when
        SalesOrderResponse response = salesOrderService.createOrder(sessionToken, idempotencyKey, request);

        // then
        assertThat(response).isNotNull();
        verify(salesOrderRepository).save(any(SalesOrder.class));
        verify(salesOrderItemRepository).saveAll(anyList());
        verify(stockManagerFacade).processOrderStockDeduction(eq(savedOrder), anyList());
    }

    @Test
    @DisplayName("주문 생성 성공 - 재고 차감 성공 시나리오")
    void givenValidRequestWithIngredients_whenCreateOrder_thenSuccessWithStockDeduction() throws Exception {
        // given
        String sessionToken = "test-token";
        String idempotencyKey = "idempotency-123";

        JsonNode ingredientsJson =
                objectMapper.readTree("{\"ingredients\": [{\"ingredientId\": 1, \"quantity\": 0.5}]}");

        Menu menuWithIngredients = Menu.create(store, "김치찌개", new BigDecimal("8000"), ingredientsJson);
        ReflectionTestUtils.setField(menuWithIngredients, "menuId", 3L);

        SalesOrderCreateRequest request = new SalesOrderCreateRequest(
                List.of(new SalesOrderItemRequest(menuWithIngredients.getMenuPublicId(), 1))
        );

        given(tableSessionRepository.findBySessionTokenHashAndStatus(anyString(), eq(TableSessionStatus.ACTIVE)))
                .willReturn(Optional.of(session));

        given(salesOrderRepository.findByStoreStoreIdAndIdempotencyKey(anyLong(), eq(idempotencyKey)))
                .willReturn(Optional.empty());

        given(menuRepository.findByMenuPublicIdInAndStatusNot(
                ArgumentMatchers.<UUID>anyList(),
                eq(MenuStatus.DELETED)
        )).willReturn(List.of(menuWithIngredients));

        SalesOrder savedOrder = SalesOrder.create(store, table, session, idempotencyKey, SalesOrderType.DINE_IN);
        ReflectionTestUtils.setField(savedOrder, "salesOrderId", 100L);

        given(salesOrderRepository.save(any(SalesOrder.class)))
                .willReturn(savedOrder);

        given(salesOrderItemRepository.saveAll(anyList()))
                .willReturn(List.of());

        // when
        salesOrderService.createOrder(sessionToken, idempotencyKey, request);

        // then
        verify(salesOrderRepository).save(any(SalesOrder.class));
        verify(salesOrderItemRepository).saveAll(anyList());
        verify(stockManagerFacade).processOrderStockDeduction(any(SalesOrder.class), anyList());
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족 시 예외 발생")
    void givenInsufficientStock_whenCreateOrder_thenThrowException() throws Exception {
        // given
        String sessionToken = "test-token";
        String idempotencyKey = "idempotency-123";

        Menu menuWithIngredients = Menu.create(
                store,
                "김치찌개",
                new BigDecimal("8000"),
                objectMapper.readTree("{\"ingredients\": []}")
        );

        SalesOrderCreateRequest request = new SalesOrderCreateRequest(
                List.of(new SalesOrderItemRequest(menuWithIngredients.getMenuPublicId(), 1))
        );

        given(tableSessionRepository.findBySessionTokenHashAndStatus(anyString(), eq(TableSessionStatus.ACTIVE)))
                .willReturn(Optional.of(session));

        given(salesOrderRepository.findByStoreStoreIdAndIdempotencyKey(anyLong(), eq(idempotencyKey)))
                .willReturn(Optional.empty());

        // ✅ 변경: StatusNot 반영
        given(menuRepository.findByMenuPublicIdInAndStatusNot(
                ArgumentMatchers.<UUID>anyList(),
                eq(MenuStatus.DELETED)
        )).willReturn(List.of(menuWithIngredients));

        SalesOrder savedOrder = SalesOrder.create(store, table, session, idempotencyKey, SalesOrderType.DINE_IN);
        given(salesOrderRepository.save(any(SalesOrder.class)))
                .willReturn(savedOrder);

        given(salesOrderItemRepository.saveAll(anyList()))
                .willReturn(List.of());

        // Facade의 바뀐 시그니처에 맞춰 예외 발생 stubbing
        willThrow(new SalesOrderException(SalesOrderErrorCode.INSUFFICIENT_STOCK))
                .given(stockManagerFacade).processOrderStockDeduction(any(SalesOrder.class), anyList());

        // when & then
        assertThatThrownBy(() -> salesOrderService.createOrder(sessionToken, idempotencyKey, request))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining(SalesOrderErrorCode.INSUFFICIENT_STOCK.getMessage());
    }

    @Test
    @DisplayName("주문 생성 실패 - 유효하지 않은 세션")
    void givenInvalidSession_whenCreateOrder_thenThrowException() {
        given(tableSessionRepository.findBySessionTokenHashAndStatus(anyString(), eq(TableSessionStatus.ACTIVE)))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                salesOrderService.createOrder("invalid", "key", new SalesOrderCreateRequest(List.of()))
        )
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining(SalesOrderErrorCode.INVALID_SESSION.getMessage());
    }

    @Test
    @DisplayName("주문 목록 조회 성공 - 필터링 적용")
    void givenValidSearchRequest_whenGetStoreOrders_thenReturnFilteredOrders() {
        // given
        Long userId = 1L;
        UUID storePublicId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC);

        SalesOrderSearchRequest request = new SalesOrderSearchRequest(
                from,
                to,
                SalesOrderStatus.COMPLETED,
                new BigDecimal("10000"),
                new BigDecimal("50000")
        );

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
                .willReturn(1L);

        SalesOrder order = SalesOrder.create(store, table, session, "key1", SalesOrderType.DINE_IN);
        ReflectionTestUtils.setField(order, "salesOrderId", 1L);
        ReflectionTestUtils.setField(order, "totalAmount", new BigDecimal("23000"));

        Page<SalesOrder> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);
        given(salesOrderRepository.findStoreOrders(eq(1L), eq(request), any(Pageable.class)))
                .willReturn(orderPage);

        given(salesOrderItemRepository.findBySalesOrderSalesOrderIdIn(anyList()))
                .willReturn(Collections.emptyList());

        // when
        PageResponse<SalesOrderResponse> response = salesOrderService.getStoreOrders(
                userId, storePublicId, request, PageRequest.of(0, 20)
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(1);
        verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
        verify(salesOrderRepository).findStoreOrders(eq(1L), eq(request), any(Pageable.class));
    }

    @Test
    @DisplayName("주문 목록 조회 실패 - 날짜 범위 유효하지 않음 (from > to)")
    void givenInvalidDateRange_whenGetStoreOrders_thenThrowException() {
        // given
        Long userId = 1L;
        UUID storePublicId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);

        SalesOrderSearchRequest request = new SalesOrderSearchRequest(from, to, null, null, null);

        // when & then
        assertThatThrownBy(() ->
                salesOrderService.getStoreOrders(userId, storePublicId, request, PageRequest.of(0, 20))
        )
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining(SalesOrderErrorCode.INVALID_SEARCH_PERIOD.getMessage());
    }

    @Test
    @DisplayName("주문 목록 조회 실패 - 금액 범위 유효하지 않음 (amountMin > amountMax)")
    void givenInvalidAmountRange_whenGetStoreOrders_thenThrowException() {
        // given
        Long userId = 1L;
        UUID storePublicId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC);

        SalesOrderSearchRequest request = new SalesOrderSearchRequest(
                from,
                to,
                null,
                new BigDecimal("50000"),
                new BigDecimal("10000")
        );

        // when & then
        assertThatThrownBy(() ->
                salesOrderService.getStoreOrders(userId, storePublicId, request, PageRequest.of(0, 20))
        )
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining(SalesOrderErrorCode.INVALID_AMOUNT_RANGE.getMessage());
    }
}