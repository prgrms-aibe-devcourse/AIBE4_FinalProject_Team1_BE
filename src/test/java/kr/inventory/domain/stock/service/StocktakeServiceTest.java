package kr.inventory.domain.stock.service;

import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.catalog.repository.IngredientRepository;
import kr.inventory.domain.stock.controller.dto.StocktakeCreateRequest;
import kr.inventory.domain.stock.controller.dto.StocktakeItemRequest;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.Stocktake;
import kr.inventory.domain.stock.entity.StocktakeSheet;
import kr.inventory.domain.stock.entity.enums.StocktakeStatus;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StocktakeRepository;
import kr.inventory.domain.stock.repository.StocktakeSheetRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StocktakeServiceTest {

    @Mock
    private StocktakeRepository stocktakeRepository;
    @Mock private IngredientStockBatchRepository batchRepository;
    @Mock private IngredientRepository ingredientRepository;
    @Mock private StocktakeSheetRepository sheetRepository;
    @Mock private StoreAccessValidator storeAccessValidator;

    @InjectMocks
    private StocktakeService stocktakeService;

    private final Long userId = 1L;
    private final Long storeId = 10L;
    private final UUID storePublicId = UUID.randomUUID();

    @Test
    @DisplayName("실사 시트를 생성하면 시트와 항목들이 정상적으로 저장된다.")
    void createStocktakeSheet_Success() {
        // given
        StocktakeItemRequest itemReq = new StocktakeItemRequest(100L, new BigDecimal("50.0"));
        StocktakeCreateRequest request = new StocktakeCreateRequest("정기 실사", List.of(itemReq));
        Ingredient ingredient = createIngredient(100L);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(ingredientRepository.findAllByStoreStoreIdAndIngredientIdIn(eq(storeId), anyList())).willReturn(List.of(ingredient));

        // when
        stocktakeService.createStocktakeSheet(userId, storePublicId, request);

        // then
        verify(sheetRepository, times(1)).save(any(StocktakeSheet.class));
        verify(stocktakeRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("실사 확정 시, 실제 수량이 장부보다 많으면 새로운 조정 배치가 생성된다.")
    void confirmSheet_Surplus_CreatesAdjustmentBatch() {
        // given
        Long sheetId = 1L;
        StocktakeSheet sheet = createSheet(sheetId, storeId);
        Ingredient ingredient = createIngredient(100L);
        Stocktake stocktakeItem = createStocktakeItem(sheet, ingredient, new BigDecimal("150.0"));
        IngredientStockBatch batch = createBatch(storeId, ingredient, new BigDecimal("100.0"));

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(sheetRepository.findByIdAndStoreIdWithLock(sheetId, storeId)).willReturn(Optional.of(sheet));
        given(stocktakeRepository.findBySheet(sheet)).willReturn(List.of(stocktakeItem));
        given(batchRepository.findAvailableBatchesByStoreWithLock(eq(storeId), anyList())).willReturn(List.of(batch));
        given(batchRepository.findLatestUnitCostByStoreAndIngredient(storeId, 100L)).willReturn(Optional.of(new BigDecimal("1200")));

        // when
        stocktakeService.confirmSheet(userId, storePublicId, sheetId);

        // then
        assertThat(batch.getRemainingQuantity()).isEqualByComparingTo("100.0");
        verify(batchRepository, times(1)).save(any(IngredientStockBatch.class));
        assertThat(sheet.getStatus()).isEqualTo(StocktakeStatus.CONFIRMED);
    }

    @Test
    @DisplayName("실사 확정 시, 실제 수량이 장부보다 적으면 기존 배치의 수량이 차감된다.")
    void confirmSheet_Deficit_UpdatesExistingBatches() {
        // given
        Long sheetId = 1L;
        StocktakeSheet sheet = createSheet(sheetId, storeId);
        Ingredient ingredient = createIngredient(200L);
        Stocktake stocktakeItem = createStocktakeItem(sheet, ingredient, new BigDecimal("30.0"));
        IngredientStockBatch batch = createBatch(storeId, ingredient, new BigDecimal("100.0"));

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(sheetRepository.findByIdAndStoreIdWithLock(sheetId, storeId)).willReturn(Optional.of(sheet));
        given(stocktakeRepository.findBySheet(sheet)).willReturn(List.of(stocktakeItem));
        given(batchRepository.findAvailableBatchesByStoreWithLock(eq(storeId), anyList())).willReturn(List.of(batch));

        // when
        stocktakeService.confirmSheet(userId, storePublicId, sheetId);

        // then
        assertThat(batch.getRemainingQuantity()).isEqualByComparingTo("30.0");
        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 확정된 시트를 다시 확정하려 하면 예외가 발생한다.")
    void confirmSheet_AlreadyConfirmed_ThrowsException() {
        // given
        Long sheetId = 1L;
        StocktakeSheet sheet = createSheet(sheetId, storeId);
        sheet.confirm();

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(sheetRepository.findByIdAndStoreIdWithLock(sheetId, storeId)).willReturn(Optional.of(sheet));

        // when & then
        assertThatThrownBy(() -> stocktakeService.confirmSheet(userId, storePublicId, sheetId))
                .isInstanceOf(StockException.class)
                .hasMessageContaining("이미 확정된 시트입니다.");
    }

    // --- Helper Methods ---
    private Ingredient createIngredient(Long id) {
        Ingredient ingredient = BeanUtils.instantiateClass(Ingredient.class);
        ReflectionTestUtils.setField(ingredient, "ingredientId", id);

        kr.inventory.domain.store.entity.Store mockStore = BeanUtils.instantiateClass(kr.inventory.domain.store.entity.Store.class);
        ReflectionTestUtils.setField(mockStore, "storeId", this.storeId);
        ReflectionTestUtils.setField(ingredient, "store", mockStore);

        return ingredient;
    }

    private StocktakeSheet createSheet(Long id, Long storeId) {
        StocktakeSheet sheet = StocktakeSheet.create(storeId, "Test Sheet");
        ReflectionTestUtils.setField(sheet, "sheetId", id);
        return sheet;
    }

    private Stocktake createStocktakeItem(StocktakeSheet sheet, Ingredient ingredient, BigDecimal qty) {
        return Stocktake.createDraft(sheet, ingredient, qty);
    }

    private IngredientStockBatch createBatch(Long storeId, Ingredient ingredient, BigDecimal qty) {
        IngredientStockBatch batch = IngredientStockBatch.create(ingredient, null, qty);
        ReflectionTestUtils.setField(batch, "storeId", storeId);
        ReflectionTestUtils.setField(batch, "initialQuantity", qty);
        return batch;
    }
}