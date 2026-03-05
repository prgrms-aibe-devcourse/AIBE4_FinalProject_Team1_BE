package kr.inventory.domain.stock.service;

import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.stock.service.command.StockDeductionRequest;
import kr.inventory.domain.store.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("재고 관리 퍼사드 테스트")
class StockManagerFacadeTest {

    @InjectMocks
    private StockManagerFacade stockManagerFacade;

    @Mock
    private TheoreticalUsageService theoreticalUsageService;

    @Mock
    private StockService stockService;

    @Mock
    private StockShortageService stockShortageService;

    private final Long internalStoreId = 100L;
    private final Long salesOrderId = 200L;

    private SalesOrder salesOrder;
    private List<SalesOrderItem> items;

    @BeforeEach
    void setUp() {
        // 공통으로 사용할 Mock 객체 설정
        salesOrder = mock(SalesOrder.class);
        Store store = mock(Store.class);
        items = List.of(mock(SalesOrderItem.class));

        // 가상의 storeId와 salesOrderId 설정
        given(salesOrder.getStore()).willReturn(store);
        given(store.getStoreId()).willReturn(internalStoreId);
        given(salesOrder.getSalesOrderId()).willReturn(salesOrderId);
    }

    @Test
    @DisplayName("재고 차감 프로세스 성공 - 모든 로직이 정상 호출되어야 한다")
    void processOrderStockDeduction_Success() {
        // given
        Map<Long, BigDecimal> usageMap = Map.of(1L, new BigDecimal("2.5"));

        given(salesOrder.isStockProcessed()).willReturn(false);
        // 수정: 매개변수가 (storeId, items)로 변경됨
        given(theoreticalUsageService.calculateOrderUsage(internalStoreId, items)).willReturn(usageMap);
        given(stockService.deductStockWithFEFO(any(StockDeductionRequest.class)))
                .willReturn(Collections.emptyMap());

        // when - 수정: 메서드 시그니처 변경 반영
        stockManagerFacade.processOrderStockDeduction(salesOrder, items);

        // then
        verify(theoreticalUsageService).calculateOrderUsage(internalStoreId, items);
        verify(stockService).deductStockWithFEFO(any(StockDeductionRequest.class));
        verify(salesOrder).markAsStockProcessed();

        // 부족분이 없으므로 기록 서비스는 호출되지 않아야 함
        verifyNoInteractions(stockShortageService);
    }

    @Test
    @DisplayName("이미 처리된 주문인 경우 조기 리턴되어야 한다")
    void processOrderStockDeduction_AlreadyProcessed() {
        // given
        given(salesOrder.isStockProcessed()).willReturn(true);

        // when
        stockManagerFacade.processOrderStockDeduction(salesOrder, items);

        // then
        verify(theoreticalUsageService, never()).calculateOrderUsage(anyLong(), anyList());
        verify(stockService, never()).deductStockWithFEFO(any());
        verify(salesOrder, never()).markAsStockProcessed();
    }

    @Test
    @DisplayName("재고 부족이 발생하면 부족분 기록 서비스가 호출되어야 한다")
    void processOrderStockDeduction_ShortageOccurs() {
        // given
        Map<Long, BigDecimal> usageMap = Map.of(1L, new BigDecimal("2.5"));
        Map<Long, BigDecimal> shortageMap = Map.of(1L, new BigDecimal("1.0"));

        given(salesOrder.isStockProcessed()).willReturn(false);
        given(theoreticalUsageService.calculateOrderUsage(internalStoreId, items)).willReturn(usageMap);
        given(stockService.deductStockWithFEFO(any(StockDeductionRequest.class)))
                .willReturn(shortageMap);

        // when
        stockManagerFacade.processOrderStockDeduction(salesOrder, items);

        // then
        verify(stockShortageService).recordShortages(eq(internalStoreId), eq(salesOrderId), eq(usageMap), eq(shortageMap));
        verify(salesOrder).markAsStockProcessed();
    }
}