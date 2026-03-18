package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.service.command.StockInboundSummary;

import java.time.OffsetDateTime;
import java.util.List;

public interface StockInboundQueryRepository {
    List<StockInboundSummary> findInboundSummaries(
            Long storeId,
            String keyword,
            OffsetDateTime from,
            OffsetDateTime to,
            Integer limit
    );
}
