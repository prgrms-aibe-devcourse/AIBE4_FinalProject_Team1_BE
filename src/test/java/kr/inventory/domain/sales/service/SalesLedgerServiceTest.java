package kr.inventory.domain.sales.service;

import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.sales.controller.dto.request.SalesLedgerSearchRequest;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderDetailResponse;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderSummaryResponse;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
import kr.inventory.domain.sales.exception.SalesOrderErrorCode;
import kr.inventory.domain.sales.exception.SalesOrderException;
import kr.inventory.domain.sales.repository.SalesOrderItemRepository;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.service.StoreAccessValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("매출 내역 서비스 테스트")
class SalesLedgerServiceTest {

    @InjectMocks
    private SalesLedgerService salesLedgerService;

    @Mock
    private StoreAccessValidator storeAccessValidator;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;

    private SalesOrder salesOrder;

    @BeforeEach
    void setUp() {
        Store store = Store.create("테스트 매장", "1234567890");
        ReflectionTestUtils.setField(store, "storeId", 1L);

        DiningTable table = DiningTable.create(store, "T1");
        ReflectionTestUtils.setField(table, "tableId", 1L);

        salesOrder = SalesOrder.create(store, table, null, "idempotency-1", SalesOrderType.DINE_IN);
        ReflectionTestUtils.setField(salesOrder, "salesOrderId", 100L);
        ReflectionTestUtils.setField(salesOrder, "status", SalesOrderStatus.COMPLETED);
        salesOrder.updateTotalAmount(new BigDecimal("15000"));
    }

    @Test
    @DisplayName("매출 내역 목록 조회 성공")
    void givenValidRequest_whenGetSalesLedgerOrders_thenReturnPage() {
        Long userId = 10L;
        UUID storePublicId = UUID.randomUUID();
        SalesLedgerSearchRequest request = new SalesLedgerSearchRequest(
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(1),
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                SalesOrderType.DINE_IN
        );

        Page<SalesOrder> page = new PageImpl<>(List.of(salesOrder), PageRequest.of(0, 20), 1);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(1L);
        given(salesOrderRepository.findSalesLedgerOrders(eq(1L), any(), any(), eq(null), eq(SalesOrderType.DINE_IN), any()))
                .willReturn(page);
        SalesOrderItem mockedItem = mockItem(salesOrder);
        given(salesOrderItemRepository.findBySalesOrderSalesOrderIdIn(List.of(100L))).willReturn(List.of(mockedItem));

        Page<SalesLedgerOrderSummaryResponse> result = salesLedgerService.getSalesLedgerOrders(
                userId,
                storePublicId,
                request,
                PageRequest.of(0, 20)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).itemCount()).isEqualTo(1);
        assertThat(result.getContent().get(0).netAmount()).isEqualTo(new BigDecimal("15000"));
    }

    @Test
    @DisplayName("매출 내역 목록 조회 실패 - 기간 역전")
    void givenInvalidPeriod_whenGetSalesLedgerOrders_thenThrowException() {
        Long userId = 10L;
        UUID storePublicId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime to = from.minusDays(1);

        SalesLedgerSearchRequest request = new SalesLedgerSearchRequest(
                from,
                to,
                SalesOrderStatus.COMPLETED,
                SalesOrderType.DINE_IN
        );

        assertThatThrownBy(() -> salesLedgerService.getSalesLedgerOrders(userId, storePublicId, request, PageRequest.of(0, 20)))
                .isInstanceOf(SalesOrderException.class)
                .extracting("errorModel")
                .isEqualTo(SalesOrderErrorCode.INVALID_SALES_LEDGER_PERIOD);
    }

    @Test
    @DisplayName("매출 내역 상세 조회 성공")
    void givenValidOrderPublicId_whenGetSalesLedgerOrder_thenReturnDetail() {
        Long userId = 10L;
        UUID storePublicId = UUID.randomUUID();
        UUID orderPublicId = salesOrder.getOrderPublicId();

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(1L);
        given(salesOrderRepository.findByOrderPublicIdWithItems(orderPublicId, 1L)).willReturn(Optional.of(salesOrder));
        given(salesOrderItemRepository.findBySalesOrderSalesOrderId(100L)).willReturn(List.of());

        SalesLedgerOrderDetailResponse response = salesLedgerService.getSalesLedgerOrder(userId, storePublicId, orderPublicId);

        assertThat(response.orderPublicId()).isEqualTo(orderPublicId);
        assertThat(response.itemCount()).isEqualTo(0);
    }

    private SalesOrderItem mockItem(SalesOrder salesOrder) {
        SalesOrderItem item = org.mockito.Mockito.mock(SalesOrderItem.class);
        org.mockito.Mockito.when(item.getSalesOrder()).thenReturn(salesOrder);
        return item;
    }
}
