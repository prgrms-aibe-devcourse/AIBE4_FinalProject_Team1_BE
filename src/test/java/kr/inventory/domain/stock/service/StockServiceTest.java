package kr.inventory.domain.stock.service;

import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.enums.StockBatchStatus;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private IngredientStockBatchRepository batchRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    @DisplayName("FEFO 정책에 따라 유통기한이 빠른 배치부터 차례대로 차감된다.")
    void deductStock_FEFO_Success() {
        // given
        Long ingredientId = 100L;
        BigDecimal usageAmount = new BigDecimal("150");

        IngredientStockBatch batch1 = createBatch(1L, 100, LocalDate.now().plusDays(5));
        IngredientStockBatch batch2 = createBatch(2L, 100, LocalDate.now().plusDays(10));

        when(batchRepository.findByIngredient_IngredientIdAndStatusAndRemainingQuantityGreaterThanOrderByExpirationDateAscCreatedAtAsc(
                eq(ingredientId), eq(StockBatchStatus.OPEN), eq(BigDecimal.ZERO)))
                .thenReturn(List.of(batch1, batch2));

        // when
        Map<Long, BigDecimal> shortageMap = stockService.deductStockWithFEFO(Map.of(ingredientId, usageAmount));

        // then
        assertThat(shortageMap).isEmpty();
        assertThat(batch1.getRemainingQuantity()).isEqualByComparingTo("0");
        assertThat(batch1.getStatus()).isEqualTo(StockBatchStatus.CLOSED);
        assertThat(batch2.getRemainingQuantity()).isEqualByComparingTo("50");
        assertThat(batch2.getStatus()).isEqualTo(StockBatchStatus.OPEN);
    }

    @Test
    @DisplayName("재고가 부족한 경우 가능한 만큼만 차감하고 부족분을 반환한다")
    void deductStock_Shortage_Handled() {
        // given
        Long ingredientId = 200L;
        BigDecimal usageAmount = new BigDecimal("300");

        IngredientStockBatch batch1 = createBatch(1L, 100, LocalDate.now().plusDays(1));
        IngredientStockBatch batch2 = createBatch(2L, 150, LocalDate.now().plusDays(2));

        when(batchRepository.findByIngredient_IngredientIdAndStatusAndRemainingQuantityGreaterThanOrderByExpirationDateAscCreatedAtAsc(
                any(), any(), any()))
                .thenReturn(List.of(batch1, batch2));

        // when
        Map<Long, BigDecimal> shortageMap = stockService.deductStockWithFEFO(Map.of(ingredientId, usageAmount));

        // then
        assertThat(shortageMap).containsKey(ingredientId);
        assertThat(shortageMap.get(ingredientId)).isEqualByComparingTo("50");

        assertThat(batch1.getRemainingQuantity()).isEqualByComparingTo("0");
        assertThat(batch2.getRemainingQuantity()).isEqualByComparingTo("0");
        assertThat(batch1.getStatus()).isEqualTo(StockBatchStatus.CLOSED);
        assertThat(batch2.getStatus()).isEqualTo(StockBatchStatus.CLOSED);
    }

    private IngredientStockBatch createBatch(Long id, int qty, LocalDate expiry) {
        IngredientStockBatch batch = BeanUtils.instantiateClass(IngredientStockBatch.class);

        ReflectionTestUtils.setField(batch, "batchId", id);
        ReflectionTestUtils.setField(batch, "remainingQuantity", new BigDecimal(qty));
        ReflectionTestUtils.setField(batch, "initialQuantity", new BigDecimal(qty));
        ReflectionTestUtils.setField(batch, "expirationDate", expiry);
        ReflectionTestUtils.setField(batch, "status", StockBatchStatus.OPEN);

        return batch;
    }
}