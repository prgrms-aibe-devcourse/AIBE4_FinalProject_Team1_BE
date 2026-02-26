package kr.inventory.domain.stock.service;

import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.enums.SalesOrderChannel;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.stock.controller.dto.request.StockOrderDeductionRequest;
import kr.inventory.domain.store.service.StoreAccessValidator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
	@Mock
	private StoreAccessValidator storeAccessValidator;

	private final Long userId = 1L;
	private final UUID storePublicId = UUID.randomUUID();
	private final Long internalStoreId = 100L;
	private final Long salesOrderId = 200L;

	@Test
	@DisplayName("재고 차감 프로세스 성공 - 모든 로직이 정상 호출되어야 한다")
	void processOrderStockDeduction_Success() {
		// given
		StockOrderDeductionRequest request =
			new StockOrderDeductionRequest(internalStoreId, salesOrderId);

		SalesOrder salesOrder = spy(SalesOrder.create(null, "EXT-001", OffsetDateTime.now(), SalesOrderChannel.POS));

		given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(internalStoreId);
		given(salesOrderRepository.findByIdAndStoreStoreIdWithLock(salesOrderId, internalStoreId))
			.willReturn(Optional.of(salesOrder));

		Map<Long, BigDecimal> usageMap = Map.of(1L, new BigDecimal("2.5"));
		given(theoreticalUsageService.calculateOrderUsage(salesOrder)).willReturn(usageMap);
		given(stockService.deductStockWithFEFO(internalStoreId, usageMap)).willReturn(Collections.emptyMap());

		// when
		stockManagerFacade.processOrderStockDeduction(userId, storePublicId, request);

		// then
		verify(salesOrder).markAsStockProcessed();
		verify(stockService).deductStockWithFEFO(internalStoreId, usageMap);
		assertTrue(salesOrder.isStockProcessed());
	}

	@Test
	@DisplayName("이미 처리된 주문인 경우 조기 리턴되어야 한다")
	void processOrderStockDeduction_AlreadyProcessed() {
		// given
		StockOrderDeductionRequest request =
			new StockOrderDeductionRequest(internalStoreId, salesOrderId);

		SalesOrder salesOrder = SalesOrder.create(null, "EXT-001", OffsetDateTime.now(), SalesOrderChannel.POS);
		salesOrder.markAsStockProcessed();

		given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(internalStoreId);
		given(salesOrderRepository.findByIdAndStoreStoreIdWithLock(salesOrderId, internalStoreId))
			.willReturn(Optional.of(salesOrder));

		// when
		stockManagerFacade.processOrderStockDeduction(userId, storePublicId, request);

		// then
		verify(theoreticalUsageService, never()).calculateOrderUsage(any());
		verify(stockService, never()).deductStockWithFEFO(any(), any());
	}
}