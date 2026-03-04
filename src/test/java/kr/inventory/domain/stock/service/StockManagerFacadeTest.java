package kr.inventory.domain.stock.service;

import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.exception.SalesOrderErrorCode;
import kr.inventory.domain.sales.exception.SalesOrderException;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.stock.service.command.StockDeductionRequest; // Request import 추가
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

import static org.assertj.core.api.Assertions.assertThatThrownBy; // AssertJ 예외 검증 추가
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
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private StockService stockService;

    @Mock
    private StoreAccessValidator storeAccessValidator;

    private final Long internalStoreId = 100L;
    private final Long salesOrderId = 200L;

    @Test
    @DisplayName("재고 차감 프로세스 성공 - 모든 로직이 정상 호출되어야 한다")
    void processOrderStockDeduction_Success() {
        // given
        SalesOrder salesOrder = mock(SalesOrder.class);
        Map<Long, BigDecimal> usageMap = Map.of(1L, new BigDecimal("2.5"));

        given(salesOrderRepository.findByIdAndStoreStoreIdWithLock(salesOrderId, internalStoreId))
                .willReturn(Optional.of(salesOrder));
        given(salesOrder.isStockProcessed()).willReturn(false);
        given(theoreticalUsageService.calculateOrderUsage(salesOrder)).willReturn(usageMap);

        // 수정: StockDeductionRequest 객체를 인자로 받도록 stub 설정
        given(stockService.deductStockWithFEFO(any(StockDeductionRequest.class)))
                .willReturn(Collections.emptyMap());

        // when
        stockManagerFacade.processOrderStockDeduction(internalStoreId, salesOrderId);

        // then
        verify(theoreticalUsageService).calculateOrderUsage(salesOrder);
        // 수정: 인자 검증 시에도 Request 객체 타입 확인
        verify(stockService).deductStockWithFEFO(any(StockDeductionRequest.class));
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
        verify(stockService, never()).deductStockWithFEFO(any());
        verify(salesOrder, never()).markAsStockProcessed();
    }

    @Test
    @DisplayName("주문이 없으면 SALES_ORDER_NOT_FOUND 예외를 던진다")
    void processOrderStockDeduction_OrderNotFound() {
        // given
        given(salesOrderRepository.findByIdAndStoreStoreIdWithLock(salesOrderId, internalStoreId))
                .willReturn(Optional.empty());

        // when & then (AssertJ를 사용한 깔끔한 예외 검증)
        assertThatThrownBy(() -> stockManagerFacade.processOrderStockDeduction(internalStoreId, salesOrderId))
                .isInstanceOf(SalesOrderException.class)
                .extracting("errorModel") // 또는 getErrorCode() 등 프로젝트 스펙에 맞춰 수정
                .isEqualTo(SalesOrderErrorCode.SALES_ORDER_NOT_FOUND);

        verify(theoreticalUsageService, never()).calculateOrderUsage(any());
        verify(stockService, never()).deductStockWithFEFO(any());
    }

    @Test
    @DisplayName("재고 부족이 발생해도 기록 로직(handleStockShortage)이 실행되고 프로세스는 완료 마킹되어야 한다")
    void processOrderStockDeduction_ShortageOccurs() {
        // given
        SalesOrder salesOrder = mock(SalesOrder.class);
        Map<Long, BigDecimal> usageMap = Map.of(1L, new BigDecimal("2.5"));
        Map<Long, BigDecimal> shortageMap = Map.of(10L, new BigDecimal("1.0"));

        given(salesOrderRepository.findByIdAndStoreStoreIdWithLock(salesOrderId, internalStoreId))
                .willReturn(Optional.of(salesOrder));
        given(salesOrder.isStockProcessed()).willReturn(false);
        given(salesOrder.getSalesOrderId()).willReturn(salesOrderId);
        given(theoreticalUsageService.calculateOrderUsage(salesOrder)).willReturn(usageMap);

        // 수정: Request 객체 대응
        given(stockService.deductStockWithFEFO(any(StockDeductionRequest.class)))
                .willReturn(shortageMap);

        // when
        stockManagerFacade.processOrderStockDeduction(internalStoreId, salesOrderId);

        // then
        verify(stockService).deductStockWithFEFO(any(StockDeductionRequest.class));
        verify(salesOrder).markAsStockProcessed();
    }
}