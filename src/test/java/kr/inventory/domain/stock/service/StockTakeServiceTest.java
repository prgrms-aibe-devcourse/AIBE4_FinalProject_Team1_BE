package kr.inventory.domain.stock.service;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.controller.dto.request.StockTakeSheetCreateRequest;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockTake;
import kr.inventory.domain.stock.entity.StockTakeSheet;
import kr.inventory.domain.stock.entity.enums.StockTakeStatus;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StockTakeRepository;
import kr.inventory.domain.stock.repository.StockTakeSheetRepository;
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
class StockTakeServiceTest {

    @Mock
    private StockTakeRepository stockTakeRepository;
    @Mock
    private IngredientStockBatchRepository batchRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private StockTakeSheetRepository sheetRepository;
    @Mock
    private StoreAccessValidator storeAccessValidator;

    @InjectMocks
    private StockTakeService stockTakeService;

    private final Long userId = 1L;
    private final Long storeId = 10L;
    private final UUID storePublicId = UUID.randomUUID();
    private final UUID sheetPublicId = UUID.randomUUID();

    @Test
    @DisplayName("실사 시트를 생성하면 시트와 항목들이 정상적으로 저장된다.")
    void createStockTakeSheet_Success() {
        // given
        UUID ingredientPublicId = UUID.randomUUID();
        StockTakeDraftSaveItemRequest itemReq = new StockTakeDraftSaveItemRequest(ingredientPublicId, new BigDecimal("50.0"));
        StockTakeSheetCreateRequest request = new StockTakeSheetCreateRequest("정기 실사", List.of(itemReq));
        Ingredient ingredient = createIngredient(100L);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(ingredientRepository.findAllByStoreStoreIdAndIngredientIdInAndStatusNot(
                eq(storeId),
                anyList(),
                eq(IngredientStatus.DELETED)
        )).willReturn(List.of(ingredient));

        // when
        stockTakeService.createStockTakeSheet(userId, storePublicId, request);

        // then
        verify(sheetRepository, times(1)).save(any(StockTakeSheet.class));
        verify(stockTakeRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("실사 확정 시, 실제 수량이 장부보다 많으면 새로운 조정 배치가 생성된다.")
    void confirmSheet_Surplus_CreatesAdjustmentBatch() {
        // given
        StockTakeSheet sheet = createSheet(sheetPublicId, storeId);
        Ingredient ingredient = createIngredient(100L);
        StockTake stockTakeItem = createStockTakeItem(sheet, ingredient, new BigDecimal("150.0"));
        IngredientStockBatch batch = createBatch(storeId, ingredient, new BigDecimal("100.0"));

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        // 수정한 부분: findBySheetPublicIdAndStoreIdWithLock 호출 모킹
        given(sheetRepository.findBySheetPublicIdAndStoreIdWithLock(sheetPublicId, storeId)).willReturn(Optional.of(sheet));
        given(stockTakeRepository.findBySheet(sheet)).willReturn(List.of(stockTakeItem));
        given(batchRepository.findAvailableBatchesByStoreWithLock(eq(storeId), anyList())).willReturn(List.of(batch));
        given(batchRepository.findLatestUnitCostByStoreAndIngredient(storeId, 100L)).willReturn(
                Optional.of(new BigDecimal("1200")));

        // when
        stockTakeService.confirmSheet(userId, storePublicId, sheetPublicId);

        // then
        assertThat(batch.getRemainingQuantity()).isEqualByComparingTo("100.0");
        verify(batchRepository, times(1)).save(any(IngredientStockBatch.class));
        assertThat(sheet.getStatus()).isEqualTo(StockTakeStatus.CONFIRMED);
    }

    @Test
    @DisplayName("실사 확정 시, 실제 수량이 장부보다 적으면 기존 배치의 수량이 차감된다.")
    void confirmSheet_Deficit_UpdatesExistingBatches() {
        // given
        StockTakeSheet sheet = createSheet(sheetPublicId, storeId);
        Ingredient ingredient = createIngredient(200L);
        StockTake stockTakeItem = createStockTakeItem(sheet, ingredient, new BigDecimal("30.0"));
        IngredientStockBatch batch = createBatch(storeId, ingredient, new BigDecimal("100.0"));

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(sheetRepository.findBySheetPublicIdAndStoreIdWithLock(sheetPublicId, storeId)).willReturn(Optional.of(sheet));
        given(stockTakeRepository.findBySheet(sheet)).willReturn(List.of(stockTakeItem));
        given(batchRepository.findAvailableBatchesByStoreWithLock(eq(storeId), anyList())).willReturn(List.of(batch));

        // when
        stockTakeService.confirmSheet(userId, storePublicId, sheetPublicId);

        // then
        assertThat(batch.getRemainingQuantity()).isEqualByComparingTo("30.0");
        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 확정된 시트를 다시 확정하려 하면 예외가 발생한다.")
    void confirmSheet_AlreadyConfirmed_ThrowsException() {
        // given
        StockTakeSheet sheet = createSheet(sheetPublicId, storeId);
        sheet.confirm();

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(sheetRepository.findBySheetPublicIdAndStoreIdWithLock(sheetPublicId, storeId)).willReturn(Optional.of(sheet));

        // when & then
        assertThatThrownBy(() -> stockTakeService.confirmSheet(userId, storePublicId, sheetPublicId))
                .isInstanceOf(StockException.class);
        // .hasMessageContaining("이미 확정된 시트입니다."); // StockErrorCode에 따라 메시지 검증
    }

    // --- Helper Methods ---
    private Ingredient createIngredient(Long id) {
        Ingredient ingredient = BeanUtils.instantiateClass(Ingredient.class);
        ReflectionTestUtils.setField(ingredient, "ingredientId", id);
        ReflectionTestUtils.setField(ingredient, "name", "Test Ingredient");

        kr.inventory.domain.store.entity.Store mockStore = BeanUtils.instantiateClass(
                kr.inventory.domain.store.entity.Store.class);
        ReflectionTestUtils.setField(mockStore, "storeId", this.storeId);
        ReflectionTestUtils.setField(ingredient, "store", mockStore);

        return ingredient;
    }

    private StockTakeSheet createSheet(UUID publicId, Long storeId) {
        StockTakeSheet sheet = StockTakeSheet.create(storeId, "Test Sheet");
        // Reflection을 사용하여 sheetPublicId 주입
        ReflectionTestUtils.setField(sheet, "sheetPublicId", publicId);
        // 내부 ID(Long)도 필요한 경우 함께 주입
        ReflectionTestUtils.setField(sheet, "sheetId", 1L);
        return sheet;
    }

    private StockTake createStockTakeItem(StockTakeSheet sheet, Ingredient ingredient, BigDecimal qty) {
        return StockTake.createDraft(sheet, ingredient, qty);
    }

    private IngredientStockBatch createBatch(Long storeId, Ingredient ingredient, BigDecimal qty) {
        IngredientStockBatch batch = IngredientStockBatch.createAdjustment(ingredient, qty, BigDecimal.ZERO);
        ReflectionTestUtils.setField(batch, "remainingQuantity", qty); // createAdjustment 시 설정되지만 명시적 확인
        return batch;
    }
}