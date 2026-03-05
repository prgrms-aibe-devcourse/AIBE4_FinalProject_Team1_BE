package kr.inventory.domain.stock.service;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.enums.StockBatchStatus;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.service.command.StockDeductionLogCommand;
import kr.inventory.domain.stock.service.command.StockDeductionRequest;
import kr.inventory.domain.store.entity.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("재고 서비스 단위 테스트")
class StockServiceTest {

    @Mock
    private IngredientStockBatchRepository batchRepository;

    @Mock
    private StockLogService stockLogService; // 로그 서비스 Mock 추가

    @InjectMocks
    private StockService stockService;

    private final Long storeId = 1L;
    private final Long salesOrderId = 2000L;

    @Test
    @DisplayName("FEFO 정책에 따라 유통기한이 빠른 배치부터 차례대로 차감된다.")
    void deductStock_FEFO_Success() {
        // given
        Long ingredientId = 100L;
        BigDecimal usageAmount = new BigDecimal("150");

        // 유통기한이 다른 두 개의 배치 (5일 뒤 만료, 10일 뒤 만료)
        IngredientStockBatch batch1 = createBatch(1L, storeId, ingredientId, 100, LocalDate.now().plusDays(5));
        IngredientStockBatch batch2 = createBatch(2L, storeId, ingredientId, 100, LocalDate.now().plusDays(10));

        when(batchRepository.findAvailableBatchesByStoreWithLock(eq(storeId), anyList()))
                .thenReturn(List.of(batch1, batch2));

        // Request 객체 생성
        StockDeductionRequest request = StockDeductionRequest.of(storeId, salesOrderId, Map.of(ingredientId, usageAmount));

        // when
        Map<Long, BigDecimal> shortageMap = stockService.deductStockWithFEFO(request);

        // then
        assertThat(shortageMap).isEmpty();

        // FEFO 검증: batch1(만료 임박)은 전량 소진(0), batch2는 나머지(50) 차감
        assertThat(batch1.getRemainingQuantity()).isEqualByComparingTo("0");
        assertThat(batch1.getStatus()).isEqualTo(StockBatchStatus.CLOSED);

        assertThat(batch2.getRemainingQuantity()).isEqualByComparingTo("50");
        assertThat(batch2.getStatus()).isEqualTo(StockBatchStatus.OPEN);

        // 로그 기록 검증: 두 개의 배치에서 차감이 일어났으므로 로그도 두 번 호출되어야 함
        verify(stockLogService, times(2)).logDeduction(any(StockDeductionLogCommand.class));
    }

    @Test
    @DisplayName("재고가 부족한 경우 가능한 만큼만 차감하고 부족분을 반환한다")
    void deductStock_Shortage_Handled() {
        // given
        Long ingredientId = 200L;
        BigDecimal usageAmount = new BigDecimal("300");

        IngredientStockBatch batch1 = createBatch(1L, storeId, ingredientId, 100, LocalDate.now().plusDays(1));
        IngredientStockBatch batch2 = createBatch(2L, storeId, ingredientId, 150, LocalDate.now().plusDays(2));

        when(batchRepository.findAvailableBatchesByStoreWithLock(eq(storeId), anyList()))
                .thenReturn(List.of(batch1, batch2));

        StockDeductionRequest request = StockDeductionRequest.of(storeId, salesOrderId, Map.of(ingredientId, usageAmount));

        // when
        Map<Long, BigDecimal> shortageMap = stockService.deductStockWithFEFO(request);

        // then
        assertThat(shortageMap).containsKey(ingredientId);
        // 총 250만 있으므로 50이 부족함
        assertThat(shortageMap.get(ingredientId)).isEqualByComparingTo("50");

        assertThat(batch1.getRemainingQuantity()).isEqualByComparingTo("0");
        assertThat(batch2.getRemainingQuantity()).isEqualByComparingTo("0");

        // 로그 기록 검증
        verify(stockLogService, times(2)).logDeduction(any(StockDeductionLogCommand.class));
    }

    /**
     * Helper 메서드: 테스트용 재고 배치 생성
     * 도메인 로직에서 batch.getIngredient().getStore()를 참조하므로 연관관계를 맺어줌
     */
    private IngredientStockBatch createBatch(Long batchId, Long storeId, Long ingredientId, int qty, LocalDate expiry) {
        // 1. Store 생성 (Id 설정)
        Store store = BeanUtils.instantiateClass(Store.class);
        ReflectionTestUtils.setField(store, "storeId", storeId);

        // 2. Ingredient 생성 및 Store 연결
        Ingredient ingredient = BeanUtils.instantiateClass(Ingredient.class);
        ReflectionTestUtils.setField(ingredient, "ingredientId", ingredientId);
        ReflectionTestUtils.setField(ingredient, "store", store);

        // 3. Batch 생성 및 데이터 세팅
        IngredientStockBatch batch = BeanUtils.instantiateClass(IngredientStockBatch.class);
        ReflectionTestUtils.setField(batch, "batchId", batchId);
        ReflectionTestUtils.setField(batch, "storeId", storeId);
        ReflectionTestUtils.setField(batch, "ingredient", ingredient);
        ReflectionTestUtils.setField(batch, "remainingQuantity", new BigDecimal(qty));
        ReflectionTestUtils.setField(batch, "initialQuantity", new BigDecimal(qty));
        ReflectionTestUtils.setField(batch, "expirationDate", expiry);
        ReflectionTestUtils.setField(batch, "status", StockBatchStatus.OPEN);

        return batch;
    }
}