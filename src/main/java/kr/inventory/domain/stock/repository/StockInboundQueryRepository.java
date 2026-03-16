package kr.inventory.domain.stock.repository;

import kr.inventory.domain.stock.service.command.StockInboundSummary;

import java.util.List;
import java.util.UUID;

public interface StockInboundQueryRepository {
    List<StockInboundSummary> findInboundSummaries(
            UUID storePublicId,
            String keyword,
            int limit
    );
}
