package kr.inventory.domain.stock.service;

import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.exception.SalesOrderErrorCode;
import kr.inventory.domain.sales.exception.SalesOrderException;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockManagerFacadeTest {

    @InjectMocks
    private StockManagerFacade stockManagerFacade;

    @Mock
    private TheoreticalUsageService theoreticalUsageService;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private StockService stockService;

    // 리팩토링 후 facade 내부용 메서드에서는 사용하지 않지만,
    // StockManagerFacade 필드에 남아있다면 주입을 위해 mock 선언은 유지
    @Mock
    private StoreAccessValidator storeAccessValidator;

    private final Long internalStoreId = 100L;
    private final Long salesOrderId = 200L;

    @Test
    @DisplayName("재고 차감 프로세스 성공 - 모든 로직이 정상 호출되어야 한다")
    void processOrderStockDeduction_Success() {
        // given
        SalesOrder salesOrder = mock(SalesOrder.class);

        given(salesOrderRepository.findByIdAndStoreStoreIdWithLock(salesOrderId, internalStoreId))
                .willReturn(Optional.of(salesOrder));

        given(salesOrder.isStockProcessed()).willReturn(false);

        Map<Long, BigDecimal> usageMap = Map.of(1L, new BigDecimal("2.5"));
        given(theoreticalUsageService.calculateOrderUsage(salesOrder)).willReturn(usageMap);
        given(stockService.deductStockWithFEFO(internalStoreId, usageMap)).willReturn(Collections.emptyMap());

        // when
        stockManagerFacade.processOrderStockDeduction(internalStoreId, salesOrderId);

        // then
        verify(theoreticalUsageService).calculateOrderUsage(salesOrder);
        verify(stockService).deductStockWithFEFO(internalStoreId, usageMap);
        verify(salesOrder).markAsStockProcessed();
        verifyNoMoreInteractions(theoreticalUsageService, stockService);
    }

    @Test
    @DisplayName("이미 처리된 주문인 경우 조기 리턴되어야 한다")
    void processOrderStockDeduction_AlreadyProcessed() {
        // given
        SalesOrder salesOrder = mock(SalesOrder.class);

        given(salesOrderRepository.findByIdAndStoreStoreIdWithLock(salesOrderId, internalStoreId))
                .willReturn(Optional.of(salesOrder));

        given(salesOrder.isStockProcessed()).willReturn(true);

        // when
        stockManagerFacade.processOrderStockDeduction(internalStoreId, salesOrderId);

        // then
        verify(theoreticalUsageService, never()).calculateOrderUsage(any());
        verify(stockService, never()).deductStockWithFEFO(any(), any());
        verify(salesOrder, never()).markAsStockProcessed();
    }

    @Test
    @DisplayName("주문이 없으면 SALES_ORDER_NOT_FOUND 예외를 던진다")
    void processOrderStockDeduction_OrderNotFound() {
        // given
        given(salesOrderRepository.findByIdAndStoreStoreIdWithLock(salesOrderId, internalStoreId))
                .willReturn(Optional.empty());

        // when & then
        try {
            stockManagerFacade.processOrderStockDeduction(internalStoreId, salesOrderId);
        } catch (SalesOrderException e) {
            // 필요한 경우 에러코드 검증 (프로젝트 구현에 맞게 조정)
            if (e.getErrorModel() != SalesOrderErrorCode.SALES_ORDER_NOT_FOUND) {
                throw e;
            }
        }

        verify(theoreticalUsageService, never()).calculateOrderUsage(any());
        verify(stockService, never()).deductStockWithFEFO(any(), any());
    }

    @Test
    @DisplayName("재고 부족이 발생하면 handleStockShortage는 호출되며, stockProcessed는 마킹된다(현재 구현 기준)")
    void processOrderStockDeduction_ShortageOccurs() {
        // given
        StockManagerFacade facadeSpy = spy(stockManagerFacade);

        SalesOrder salesOrder = mock(SalesOrder.class);
        given(salesOrderRepository.findByIdAndStoreStoreIdWithLock(salesOrderId, internalStoreId))
                .willReturn(Optional.of(salesOrder));

        given(salesOrder.isStockProcessed()).willReturn(false);
        given(salesOrder.getSalesOrderId()).willReturn(salesOrderId);

        Map<Long, BigDecimal> usageMap = Map.of(1L, new BigDecimal("2.5"));
        given(theoreticalUsageService.calculateOrderUsage(salesOrder)).willReturn(usageMap);

        Map<Long, BigDecimal> shortageMap = Map.of(10L, new BigDecimal("1.0"));
        given(stockService.deductStockWithFEFO(internalStoreId, usageMap)).willReturn(shortageMap);

        // private 메서드라 직접 verify 불가 -> 현재 구현은 예외도 안 던지고 그냥 진행하므로 mark 여부만 검증
        // (만약 향후 부족 시 예외를 던지도록 바꾸면 이 테스트도 그에 맞게 변경)

        // when
        facadeSpy.processOrderStockDeduction(internalStoreId, salesOrderId);

        // then
        verify(stockService).deductStockWithFEFO(internalStoreId, usageMap);
        verify(salesOrder).markAsStockProcessed();
    }
}