package kr.inventory.domain.stock.service;

import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;
import kr.inventory.domain.analytics.service.StockLogIndexingService;
import kr.inventory.domain.stock.controller.dto.request.StockLogSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.StockLogResponse;
import kr.inventory.domain.stock.entity.StockLog;
import kr.inventory.domain.stock.repository.StockLogRepository;
import kr.inventory.domain.stock.service.command.StockDeductionLogCommand;
import kr.inventory.domain.stock.service.command.StockInboundLogCommand;
import kr.inventory.domain.stock.service.command.StockWasteCommand;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockLogService {
	private final StockLogRepository stockLogRepository;
	private final StoreAccessValidator storeAccessValidator;
	private final StoreRepository storeRepository;
	private final StockLogIndexingService stockLogIndexingService;

	public void logInbound(StockInboundLogCommand stockInboundLogCommand) {
		StockLog saved = stockLogRepository.save(StockLog.createInboundLog(stockInboundLogCommand));
		try {
			// ES 인덱싱
			stockLogIndexingService.index(saved);
		} catch (Exception e) {
			log.error("[ES] 재고 로그(입고) 인덱싱 실패 logId={}", saved.getLogId(), e);
		}
	}

	public void logDeduction(StockDeductionLogCommand command) {
		StockLog saved = stockLogRepository.save(StockLog.createDeductionLog(command));
		try {
			// ES 인덱싱
			stockLogIndexingService.index(saved);
		} catch (Exception e) {
			log.error("[ES] 재고 로그(차감) 인덱싱 실패 logId={}", saved.getLogId(), e);
		}
	}

	public void logWaste(StockWasteCommand command) {
		StockLog saved = stockLogRepository.save(StockLog.createWasteLog(command));
		try {
			// ES 인덱싱
			stockLogIndexingService.index(saved);
		} catch (Exception e) {
			log.error("[ES] 재고 로그(폐기) 인덱싱 실패 logId={}", saved.getLogId(), e);
		}
	}

	@Transactional(readOnly = true)
	public Page<StockLogResponse> getStockLogs(
		Long userId,
		UUID storePublicId,
		StockLogSearchRequest condition,
		Pageable pageable
	) {
		Store store = storeRepository.findById(storeAccessValidator.validateAndGetStoreId(userId, storePublicId))
			.orElseThrow(() -> new EntityNotFoundException("매장을 찾을 수 없습니다."));

		return stockLogRepository.searchStockLog(store.getStoreId(), condition, pageable);
	}
}
