package kr.inventory.domain.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import kr.inventory.domain.stock.controller.dto.request.WasteRequest;
import kr.inventory.domain.stock.controller.dto.request.WasteSearchCondition;
import kr.inventory.domain.stock.controller.dto.response.WasteResponse;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.entity.WasteRecord;
import kr.inventory.domain.stock.entity.enums.WasteReason;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.WasteRecordRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class WasteServiceTest {

	@InjectMocks
	private WasteService wasteService;

	@Mock
	private WasteRecordRepository wasteRecordRepository;

	@Mock
	private StoreAccessValidator storeAccessValidator;

	@Mock
	private StoreRepository storeRepository;

	@Mock
	private IngredientStockBatchRepository batchRepository;

	@Mock
	private UserRepository userRepository;

	@Test
	@DisplayName("폐기 등록 성공")
	void recordWaste_Success() {
		// given
		Long userId = 1L;
		UUID storePublicId = UUID.randomUUID();
		Long storeId = 10L;
		UUID batchId = UUID.randomUUID();

		WasteRequest request = new WasteRequest(List.of(
				new WasteRequest.WasteItem(
						batchId,
						BigDecimal.TEN,
						WasteReason.EXPIRED,
						OffsetDateTime.now())));

		Store store = mock(Store.class);
		User user = mock(User.class);
		IngredientStockBatch batch = mock(IngredientStockBatch.class);

		given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
		given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(batchRepository.findByStore_StoreIdAndBatchPublicId(storeId, batchId)).willReturn(Optional.of(batch));
		given(batch.getUnitCost()).willReturn(BigDecimal.valueOf(1000));

		// when
		wasteService.recordWaste(userId, storePublicId, request);

		// then
		verify(batch).decreaseQuantity(BigDecimal.TEN);
		verify(wasteRecordRepository).save(any(WasteRecord.class));
	}

	@Test
	@DisplayName("폐기 등록 실패 - 재고 배치 없음")
	void recordWaste_Fail_NoBatch() {
		// given
		Long userId = 1L;
		UUID storePublicId = UUID.randomUUID();
		Long storeId = 10L;
		UUID batchId = UUID.randomUUID();

		WasteRequest request = new WasteRequest(List.of(
				new WasteRequest.WasteItem(
						batchId,
						BigDecimal.TEN,
						WasteReason.EXPIRED,
						OffsetDateTime.now())));

		Store store = mock(Store.class);
		User user = mock(User.class);

		given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
		given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(batchRepository.findByStore_StoreIdAndBatchPublicId(storeId, batchId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> wasteService.recordWaste(userId, storePublicId, request))
				.isInstanceOf(StockException.class)
				.satisfies(ex -> {
					StockException stockEx = (StockException) ex;
					// 💡 getErrorModel() 혹은 부모의 Getter를 호출하여 비교
					assertThat(stockEx.getErrorModel()).isEqualTo(StockErrorCode.INGREDIENT_NOT_FOUND);
				});
	}

	@Test
	@DisplayName("폐기 목록 조회 성공")
	void getWasteRecords_Success() {
		// given
		Long userId = 1L;
		UUID storePublicId = UUID.randomUUID();
		Long storeId = 10L;
		WasteSearchCondition condition = new WasteSearchCondition(null, null, null, null);
		Pageable pageable = Pageable.unpaged();

		WasteRecord record = mock(WasteRecord.class);
		IngredientStockBatch batch = mock(IngredientStockBatch.class);
		StockInboundItem inboundItem = mock(StockInboundItem.class);
		User user = mock(User.class);

		given(record.getStockBatch()).willReturn(batch);
		given(batch.getInboundItem()).willReturn(inboundItem);
		given(inboundItem.getRawProductName()).willReturn("Test Product");
		given(record.getRecordedByUser()).willReturn(user);
		given(user.getName()).willReturn("Test User");

		Page<WasteRecord> recordPage = new PageImpl<>(List.of(record));

		given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
		given(wasteRecordRepository.searchWasteRecords(storeId, condition, pageable)).willReturn(recordPage);

		// when
		Page<WasteResponse> result = wasteService.getWasteRecords(userId, storePublicId, condition, pageable);

		// then
		assertThat(result).isNotEmpty();
		assertThat(result.getContent().get(0).ingredientName()).isEqualTo("Test Product");
	}
}
