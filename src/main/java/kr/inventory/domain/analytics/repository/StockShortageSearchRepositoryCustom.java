package kr.inventory.domain.analytics.repository;

import java.util.List;

import kr.inventory.domain.analytics.controller.dto.request.ESStockShortageSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.StockShortageSummaryResponse;

public interface StockShortageSearchRepositoryCustom {
	List<StockShortageSummaryResponse> getShortageSummary(Long storeId, ESStockShortageSearchRequest request);
}
