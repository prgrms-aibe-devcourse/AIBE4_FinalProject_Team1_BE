package kr.inventory.domain.sales.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.entity.TableSession;
import kr.inventory.domain.dining.entity.enums.TableSessionStatus;
import kr.inventory.domain.dining.repository.TableSessionRepository;
import kr.inventory.domain.reference.entity.Menu;
import kr.inventory.domain.reference.repository.MenuRepository;
import kr.inventory.domain.sales.controller.dto.request.SalesOrderCreateRequest;
import kr.inventory.domain.sales.controller.dto.request.SalesOrderItemRequest;
import kr.inventory.domain.sales.controller.dto.response.SalesOrderResponse;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

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
    private Menu menu2;

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

        menu2 = Menu.create(store, "된장찌개", new BigDecimal("7000"), null);
        ReflectionTestUtils.setField(menu2, "menuId", 2L);
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
        given(menuRepository.findByMenuPublicIdIn(anyList()))
                .willReturn(List.of(menu1));

        SalesOrder savedOrder = SalesOrder.create(store, table, session, idempotencyKey, SalesOrderType.DINE_IN);
        ReflectionTestUtils.setField(savedOrder, "salesOrderId", 100L);

        // 수정: save 대신 saveAndFlush 모킹
        given(salesOrderRepository.saveAndFlush(any(SalesOrder.class)))
                .willReturn(savedOrder);

        // when
        SalesOrderResponse response = salesOrderService.createOrder(sessionToken, idempotencyKey, request);

        // then
        assertThat(response).isNotNull();
        verify(salesOrderRepository).saveAndFlush(any(SalesOrder.class));
        // Facade가 호출되었는지 확인
        verify(stockManagerFacade).processOrderStockDeduction(eq(1L), eq(100L));
    }

    @Test
    @DisplayName("주문 생성 성공 - 재고 차감 성공 시나리오")
    void givenValidRequestWithIngredients_whenCreateOrder_thenSuccessWithStockDeduction() throws Exception {
        // given
        String sessionToken = "test-token";
        String idempotencyKey = "idempotency-123";
        JsonNode ingredientsJson = objectMapper.readTree("{\"ingredients\": [{\"ingredientId\": 1, \"quantity\": 0.5}]}");
        Menu menuWithIngredients = Menu.create(store, "김치찌개", new BigDecimal("8000"), ingredientsJson);
        ReflectionTestUtils.setField(menuWithIngredients, "menuId", 3L);

        SalesOrderCreateRequest request = new SalesOrderCreateRequest(List.of(new SalesOrderItemRequest(menuWithIngredients.getMenuPublicId(), 1)));

        given(tableSessionRepository.findBySessionTokenHashAndStatus(anyString(), eq(TableSessionStatus.ACTIVE)))
                .willReturn(Optional.of(session));
        given(salesOrderRepository.findByStoreStoreIdAndIdempotencyKey(anyLong(), eq(idempotencyKey)))
                .willReturn(Optional.empty());
        given(menuRepository.findByMenuPublicIdIn(anyList()))
                .willReturn(List.of(menuWithIngredients));

        SalesOrder savedOrder = SalesOrder.create(store, table, session, idempotencyKey, SalesOrderType.DINE_IN);
        ReflectionTestUtils.setField(savedOrder, "salesOrderId", 100L);

        given(salesOrderRepository.saveAndFlush(any(SalesOrder.class)))
                .willReturn(savedOrder);

        // when
        SalesOrderResponse response = salesOrderService.createOrder(sessionToken, idempotencyKey, request);

        // then
        assertThat(response).isNotNull();
        verify(stockManagerFacade).processOrderStockDeduction(anyLong(), anyLong());
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족 시 예외 발생")
    void givenInsufficientStock_whenCreateOrder_thenThrowException() throws Exception {
        // given
        String sessionToken = "test-token";
        String idempotencyKey = "idempotency-123";
        Menu menuWithIngredients = Menu.create(store, "김치찌개", new BigDecimal("8000"), objectMapper.readTree("{\"ingredients\": []}"));
        SalesOrderCreateRequest request = new SalesOrderCreateRequest(List.of(new SalesOrderItemRequest(menuWithIngredients.getMenuPublicId(), 1)));

        given(tableSessionRepository.findBySessionTokenHashAndStatus(anyString(), eq(TableSessionStatus.ACTIVE)))
                .willReturn(Optional.of(session));
        given(salesOrderRepository.findByStoreStoreIdAndIdempotencyKey(anyLong(), eq(idempotencyKey)))
                .willReturn(Optional.empty());
        given(menuRepository.findByMenuPublicIdIn(anyList()))
                .willReturn(List.of(menuWithIngredients));

        SalesOrder savedOrder = SalesOrder.create(store, table, session, idempotencyKey, SalesOrderType.DINE_IN);
        ReflectionTestUtils.setField(savedOrder, "salesOrderId", 100L);
        given(salesOrderRepository.saveAndFlush(any(SalesOrder.class))).willReturn(savedOrder);

        // 수정: Facade에서 재고 부족 예외를 던지도록 설정 (Facade가 예외를 던지는 구조일 경우)
        willThrow(new SalesOrderException(SalesOrderErrorCode.INSUFFICIENT_STOCK))
                .given(stockManagerFacade).processOrderStockDeduction(anyLong(), anyLong());

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

        assertThatThrownBy(() -> salesOrderService.createOrder("invalid", "key", new SalesOrderCreateRequest(List.of())))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining(SalesOrderErrorCode.INVALID_SESSION.getMessage());
    }

    @Test
    @DisplayName("주문 생성 실패 - 세션 만료")
    void givenExpiredSession_whenCreateOrder_thenThrowException() {
        TableSession expiredSession = TableSession.create(table, null, "hash", OffsetDateTime.now(ZoneOffset.UTC).minusHours(1), OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        given(tableSessionRepository.findBySessionTokenHashAndStatus(anyString(), eq(TableSessionStatus.ACTIVE)))
                .willReturn(Optional.of(expiredSession));

        assertThatThrownBy(() -> salesOrderService.createOrder("token", "key", new SalesOrderCreateRequest(List.of())))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining(SalesOrderErrorCode.SESSION_EXPIRED.getMessage());
    }
}