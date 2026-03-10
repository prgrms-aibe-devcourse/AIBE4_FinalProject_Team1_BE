package kr.inventory.domain.stock.service;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.controller.dto.request.StockTakeConfirmRequest;
import kr.inventory.domain.stock.controller.dto.request.StockTakeItemQuantityRequest;
import kr.inventory.domain.stock.controller.dto.request.StockTakeSheetCreateRequest;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockTake;
import kr.inventory.domain.stock.entity.StockTakeSheet;
import kr.inventory.domain.stock.entity.enums.StockTakeStatus;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StockTakeRepository;
import kr.inventory.domain.stock.repository.StockTakeSheetRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockTakeServiceTest {

    @Mock
    private StockTakeRepository stockTakeRepository;
    @Mock
    private IngredientStockBatchRepository ingredientStockBatchRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private StockTakeSheetRepository stockTakeSheetRepository;
    @Mock
    private StoreAccessValidator storeAccessValidator;
    @Mock
    private StockLogService stockLogService;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private UserRepository userRepository;

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

        StockTakeItemQuantityRequest itemReq =
                new StockTakeItemQuantityRequest(ingredientPublicId, new BigDecimal("50.0"));
        StockTakeSheetCreateRequest request =
                new StockTakeSheetCreateRequest("정기 실사", List.of(itemReq));

        Ingredient ingredient = createIngredient(100L, ingredientPublicId);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(ingredientRepository.findAllByStoreStoreIdAndIngredientPublicIdInAndStatusNot(
                eq(storeId),
                anyList(),
                eq(IngredientStatus.DELETED)
        )).willReturn(List.of(ingredient));
        given(ingredientStockBatchRepository.findAvailableBatchesByStoreWithLock(eq(storeId), anyList()))
                .willReturn(List.of());

        // when
        UUID result = stockTakeService.createStockTakeSheet(userId, storePublicId, request);

        // then
        assertThat(result).isNotNull();
        verify(stockTakeSheetRepository, times(1)).save(any(StockTakeSheet.class));
        verify(stockTakeRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("실사 확정 시, 실제 수량이 장부보다 많으면 새로운 조정 배치가 생성된다.")
    void confirmStockTakeSheet_Surplus_CreatesAdjustmentBatch() {
        // given
        UUID ingredientPublicId = UUID.randomUUID();

        StockTakeSheet sheet = createSheet(sheetPublicId, storeId);
        Ingredient ingredient = createIngredient(100L, ingredientPublicId);
        Store store = createStore(storeId);
        User user = createUser(userId);

        StockTake stockTakeItem = createStockTakeItem(
                sheet,
                ingredient,
                new BigDecimal("100.0"),
                new BigDecimal("150.0")
        );

        StockTakeConfirmRequest request = new StockTakeConfirmRequest(
                "정기 실사 확정",
                List.of(new StockTakeItemQuantityRequest(ingredientPublicId, new BigDecimal("150.0")))
        );

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(storeRepository.getReferenceById(storeId)).willReturn(store);
        given(userRepository.getReferenceById(userId)).willReturn(user);
        given(stockTakeSheetRepository.findBySheetPublicIdAndStoreIdWithLock(sheetPublicId, storeId))
                .willReturn(Optional.of(sheet));
        given(stockTakeRepository.findAllBySheetAndIngredientPublicIdsWithLock(eq(sheet), anyList()))
                .willReturn(List.of(stockTakeItem));
        given(stockTakeRepository.findBySheet(sheet)).willReturn(List.of(stockTakeItem));
        given(ingredientStockBatchRepository.findAvailableBatchesByStoreWithLock(eq(storeId), anyList()))
                .willReturn(List.of());
        given(ingredientStockBatchRepository.findLatestUnitCostByStoreAndIngredient(storeId, 100L))
                .willReturn(Optional.of(new BigDecimal("1200")));

        // when
        stockTakeService.confirmStockTakeSheet(userId, storePublicId, sheetPublicId, request);

        // then
        verify(ingredientStockBatchRepository, times(1)).save(any(IngredientStockBatch.class));
        assertThat(sheet.getStatus()).isEqualTo(StockTakeStatus.CONFIRMED);
    }

    @Test
    @DisplayName("실사 확정 시, 실제 수량이 장부보다 적으면 기존 배치의 수량이 차감된다.")
    void confirmStockTakeSheet_Deficit_UpdatesExistingBatches() {
        // given
        UUID ingredientPublicId = UUID.randomUUID();

        StockTakeSheet sheet = createSheet(sheetPublicId, storeId);
        Ingredient ingredient = createIngredient(200L, ingredientPublicId);
        Store store = createStore(storeId);
        User user = createUser(userId);

        StockTake stockTakeItem = createStockTakeItem(
                sheet,
                ingredient,
                new BigDecimal("100.0"),
                new BigDecimal("30.0")
        );
        IngredientStockBatch batch = createBatch(ingredient, new BigDecimal("100.0"));

        StockTakeConfirmRequest request = new StockTakeConfirmRequest(
                "정기 실사 확정",
                List.of(new StockTakeItemQuantityRequest(ingredientPublicId, new BigDecimal("30.0")))
        );

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(storeRepository.getReferenceById(storeId)).willReturn(store);
        given(userRepository.getReferenceById(userId)).willReturn(user);
        given(stockTakeSheetRepository.findBySheetPublicIdAndStoreIdWithLock(sheetPublicId, storeId))
                .willReturn(Optional.of(sheet));
        given(stockTakeRepository.findAllBySheetAndIngredientPublicIdsWithLock(eq(sheet), anyList()))
                .willReturn(List.of(stockTakeItem));
        given(stockTakeRepository.findBySheet(sheet)).willReturn(List.of(stockTakeItem));
        given(ingredientStockBatchRepository.findAvailableBatchesByStoreWithLock(eq(storeId), anyList()))
                .willReturn(List.of(batch));

        // when
        stockTakeService.confirmStockTakeSheet(userId, storePublicId, sheetPublicId, request);

        // then
        assertThat(batch.getRemainingQuantity()).isEqualByComparingTo("30.0");
        verify(ingredientStockBatchRepository, never()).save(any());
        assertThat(sheet.getStatus()).isEqualTo(StockTakeStatus.CONFIRMED);
    }

    @Test
    @DisplayName("이미 확정된 시트를 다시 확정하려 하면 예외가 발생한다.")
    void confirmStockTakeSheet_AlreadyConfirmed_ThrowsException() {
        // given
        StockTakeSheet sheet = createSheet(sheetPublicId, storeId);
        sheet.confirm();

        StockTakeConfirmRequest request = new StockTakeConfirmRequest(
                "재확정 시도",
                List.of()
        );

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(stockTakeSheetRepository.findBySheetPublicIdAndStoreIdWithLock(sheetPublicId, storeId))
                .willReturn(Optional.of(sheet));

        // when & then
        assertThatThrownBy(() ->
                stockTakeService.confirmStockTakeSheet(userId, storePublicId, sheetPublicId, request)
        ).isInstanceOf(StockException.class);
    }

    private Ingredient createIngredient(Long id, UUID ingredientPublicId) {
        Ingredient ingredient = BeanUtils.instantiateClass(Ingredient.class);
        ReflectionTestUtils.setField(ingredient, "ingredientId", id);
        ReflectionTestUtils.setField(ingredient, "ingredientPublicId", ingredientPublicId);
        ReflectionTestUtils.setField(ingredient, "name", "Test Ingredient");

        Store mockStore = BeanUtils.instantiateClass(Store.class);
        ReflectionTestUtils.setField(mockStore, "storeId", this.storeId);
        ReflectionTestUtils.setField(ingredient, "store", mockStore);

        return ingredient;
    }

    private Store createStore(Long storeId) {
        Store store = BeanUtils.instantiateClass(Store.class);
        ReflectionTestUtils.setField(store, "storeId", storeId);
        return store;
    }

    private User createUser(Long userId) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private StockTakeSheet createSheet(UUID publicId, Long storeId) {
        StockTakeSheet sheet = StockTakeSheet.create(storeId, "Test Sheet");
        ReflectionTestUtils.setField(sheet, "sheetPublicId", publicId);
        ReflectionTestUtils.setField(sheet, "sheetId", 1L);
        return sheet;
    }

    private StockTake createStockTakeItem(
            StockTakeSheet sheet,
            Ingredient ingredient,
            BigDecimal theoreticalQty,
            BigDecimal stockTakeQty
    ) {
        return StockTake.createDraft(sheet, ingredient, theoreticalQty, stockTakeQty);
    }

    private IngredientStockBatch createBatch(Ingredient ingredient, BigDecimal qty) {
        IngredientStockBatch batch = IngredientStockBatch.createAdjustment(ingredient, qty, BigDecimal.ZERO, null);
        ReflectionTestUtils.setField(batch, "remainingQuantity", qty);
        return batch;
    }
}