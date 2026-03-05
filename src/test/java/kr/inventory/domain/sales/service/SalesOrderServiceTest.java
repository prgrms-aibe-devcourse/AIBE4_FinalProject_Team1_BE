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
        given(menuRepository.findByMenuPublicIdIn(anyList()))
                .willReturn(List.of(menu1));

        SalesOrder savedOrder = SalesOrder.create(store, table, session, idempotencyKey, SalesOrderType.DINE_IN);
        ReflectionTestUtils.setField(savedOrder, "salesOrderId", 100L);

        given(salesOrderRepository.saveAndFlush(any(SalesOrder.class)))
                .willReturn(savedOrder);

        // when
        SalesOrderResponse response = salesOrderService.createOrder(sessionToken, idempotencyKey, request);

        // then
        assertThat(response).isNotNull();
        verify(salesOrderRepository).saveAndFlush(any(SalesOrder.class));

        // 수정: (Long, Long) 대신 (SalesOrder, List) 타입을 받도록 검증 변경
        verify(stockManagerFacade).processOrderStockDeduction(eq(savedOrder), anyList());
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
        salesOrderService.createOrder(sessionToken, idempotencyKey, request);

        // then
        // 수정: 인자 타입 변경 반영
        verify(stockManagerFacade).processOrderStockDeduction(any(SalesOrder.class), anyList());
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
        given(salesOrderRepository.saveAndFlush(any(SalesOrder.class))).willReturn(savedOrder);

        // 수정: Facade의 바뀐 시그니처에 맞춰 예외 발생 stubbing
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

        assertThatThrownBy(() -> salesOrderService.createOrder("invalid", "key", new SalesOrderCreateRequest(List.of())))
                .isInstanceOf(SalesOrderException.class)
                .hasMessageContaining(SalesOrderErrorCode.INVALID_SESSION.getMessage());
    }
}