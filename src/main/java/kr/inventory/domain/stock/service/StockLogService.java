package kr.inventory.domain.stock.service;

import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;
import kr.inventory.domain.stock.controller.dto.request.StockLogSearchCondition;
import kr.inventory.domain.stock.controller.dto.response.StockLogResponse;
import kr.inventory.domain.stock.entity.StockLog;
import kr.inventory.domain.stock.repository.StockLogRepository;
import kr.inventory.domain.stock.service.command.StockDeductionLogCommand;
import kr.inventory.domain.stock.service.command.StockInboundLogCommand;
import kr.inventory.domain.stock.service.command.StockWasteCommand;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StockLogService {
	private final StockLogRepository stockLogRepository;
	private final StoreAccessValidator storeAccessValidator;
	private final StoreRepository storeRepository;

	public void logInbound(StockInboundLogCommand stockInboundLogCommand) {
		stockLogRepository.save(StockLog.createInboundLog(stockInboundLogCommand));
	}

	public void logDeduction(StockDeductionLogCommand command) {
		stockLogRepository.save(StockLog.createDeductionLog(command));
	}

	public void logWaste(StockWasteCommand command) {
		stockLogRepository.save(StockLog.createWasteLog(command));
	}

	@Transactional(readOnly = true)
	public Page<StockLogResponse> getStockLogs(
		Long userId,
		UUID storePublicId,
		StockLogSearchCondition condition,
		Pageable pageable
	) {
		Store store = storeRepository.findById(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
			.orElseThrow(() -> new EntityNotFoundException("매장을 찾을 수 없습니다."));

		return stockLogRepository.searchStockLog(store.getStoreId(), condition, pageable);
	}
}
