package kr.inventory.domain.stock.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.inventory.domain.stock.controller.dto.request.WasteRequest;
import kr.inventory.domain.stock.controller.dto.request.WasteSearchCondition;
import kr.inventory.domain.stock.controller.dto.response.WasteResponse;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.WasteRecord;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.WasteRecordRepository;
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

@Service
@RequiredArgsConstructor
public class WasteService {
	private final WasteRecordRepository wasteRecordRepository;
	private final StoreAccessValidator storeAccessValidator;
	private final StoreRepository storeRepository;
	private final IngredientStockBatchRepository batchRepository;
	private final UserRepository userRepository;

	@Transactional
	public void recordWaste(Long userId, UUID storePublicId, WasteRequest request) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		Store store = storeRepository.findById(storeId).orElseThrow(() -> new StoreException(
				StoreErrorCode.STORE_NOT_FOUND));

		User user = userRepository.findById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

		for (WasteRequest.WasteItem item : request.items()) {
			IngredientStockBatch batch = batchRepository
					.findByStore_StoreIdAndBatchPublicId(storeId, item.stockBatchId())
					.orElseThrow(() -> new StockException(
							StockErrorCode.INGREDIENT_NOT_FOUND));

			batch.decreaseQuantity(item.quantity());

			WasteRecord record = WasteRecord.create(
					store,
					batch,
					batch.getIngredient(),
					item.quantity(),
					item.reason(),
					item.quantity().multiply(batch.getUnitCost()),
					user,
					item.wasteDate());
			wasteRecordRepository.save(record);
		}
	}

	@Transactional(readOnly = true)
	public Page<WasteResponse> getWasteRecords(Long userId, UUID storePublicId, WasteSearchCondition condition,
			Pageable pageable) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		return wasteRecordRepository.searchWasteRecords(storeId, condition, pageable).map(WasteResponse::from);
	}
}
