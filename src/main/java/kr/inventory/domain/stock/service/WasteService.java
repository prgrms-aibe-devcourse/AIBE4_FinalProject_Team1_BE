package kr.inventory.domain.stock.service;

import java.util.UUID;

import kr.inventory.domain.analytics.service.indexing.WasteIndexingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.inventory.domain.stock.controller.dto.request.WasteRequest;
import kr.inventory.domain.stock.controller.dto.request.WasteSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.WasteResponse;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.WasteRecord;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.WasteRecordRepository;
import kr.inventory.domain.stock.service.command.StockWasteCommand;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.exception.UserErrorCode;
import kr.inventory.domain.user.exception.UserException;
import kr.inventory.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class WasteService {
	private final WasteRecordRepository wasteRecordRepository;
	private final StoreAccessValidator storeAccessValidator;
	private final StoreRepository storeRepository;
	private final IngredientStockBatchRepository batchRepository;
	private final UserRepository userRepository;
	private final StockLogService stockLogService;
	private final WasteIndexingService wasteIndexingService;

	@Transactional
	public void recordWaste(Long userId, UUID storePublicId, WasteRequest request) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		Store store = storeRepository.findById(storeId).orElseThrow(() -> new StoreException(
			StoreErrorCode.STORE_NOT_FOUND));

		User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

		for (WasteRequest.WasteItem item : request.items()) {
			processSingleWaste(store, item, user);
		}
	}

	private void processSingleWaste(Store store, WasteRequest.WasteItem item, User currentUser) {
		IngredientStockBatch batch = batchRepository.findByStore_StoreIdAndBatchPublicId(store.getStoreId(),
				item.stockBatchId())
			.orElseThrow(() -> new StockException(
				StockErrorCode.INGREDIENT_NOT_FOUND));

		batch.decreaseQuantity(item.quantity());

		// 3. 폐기(Disposal) 메인 엔티티 생성 및 저장
		WasteRecord wasteRecord = WasteRecord.create(
			store,
			batch,
			batch.getIngredient(),
			item.quantity(),
			item.reason(),
			item.quantity().multiply(batch.getUnitCost()),
			currentUser,
			item.wasteDate()

		);
		wasteRecordRepository.save(wasteRecord);

		try {
			// ES 인덱싱
			wasteIndexingService.index(wasteRecord);
		} catch (Exception e) {
			log.error("[ES] 폐기 인덱싱 실패 wasteId={}", wasteRecord.getWasteId(), e);
		}

		stockLogService.logWaste(new StockWasteCommand(
			store,
			batch.getIngredient(),
			batch,
			item.quantity(),
			batch.getRemainingQuantity(),
			wasteRecord.getWasteId(),
			currentUser
		));
	}

	@Transactional(readOnly = true)
	public Page<WasteResponse> getWasteRecords(Long userId, UUID storePublicId, WasteSearchRequest condition,
		Pageable pageable) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		return wasteRecordRepository.searchWasteRecords(storeId, condition, pageable).map(WasteResponse::from);
	}
}
