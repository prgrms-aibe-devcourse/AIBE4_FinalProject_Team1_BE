package kr.inventory.domain.stock.service;

import kr.inventory.domain.stock.entity.StockLog;
import kr.inventory.domain.stock.repository.StockLogRepository;
import kr.inventory.domain.stock.service.command.StockDeductionLogCommand;
import kr.inventory.domain.stock.service.command.StockInboundLogCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StockLogService {
	private final StockLogRepository stockLogRepository;

	public void logInbound(StockInboundLogCommand stockInboundLogCommand) {
		stockLogRepository.save(StockLog.createInboundLog(stockInboundLogCommand));
	}

    public void logDeduction(StockDeductionLogCommand command) {
        stockLogRepository.save(StockLog.createDeductionLog(command));
    }
}
