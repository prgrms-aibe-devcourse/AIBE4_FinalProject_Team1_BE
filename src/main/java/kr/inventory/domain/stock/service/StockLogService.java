package kr.inventory.domain.stock.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockLog;
import kr.inventory.domain.stock.repository.StockLogRepository;
import kr.inventory.domain.stock.service.command.StockLogCommand;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StockLogService {
	private final StockLogRepository stockLogRepository;

	public void logInbound(StockLogCommand stockLogCommand) {
		stockLogRepository.save(StockLog.createInboundLog(stockLogCommand));

	}
}
