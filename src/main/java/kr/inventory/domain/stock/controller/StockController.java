package kr.inventory.domain.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.request.StockSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.StockBatchResponse;
import kr.inventory.domain.stock.controller.dto.response.StockSummaryResponse;
import kr.inventory.domain.stock.service.StockQueryService;
import kr.inventory.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "재고(Stock)", description = "재고 차감 API")
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {
	private final StockQueryService stockQueryService;

	@Operation(
		summary = "재고 조회",
		description = "해당 매장의 재고를 조회합니다."
	)
	@GetMapping("/{storePublicId}/stocks")
	public ResponseEntity<PageResponse<StockSummaryResponse>> getStockSummaries(
		@AuthenticationPrincipal CustomUserDetails principal,
		@PathVariable("storePublicId") UUID StorePublicId,
		StockSearchRequest condition,
		Pageable pageable
	) {
		Page<StockSummaryResponse> summaryResponseList = stockQueryService.getStoreStockList(principal.getUserId(),
			StorePublicId, condition, pageable);

		return ResponseEntity.ok(PageResponse.from(summaryResponseList));
	}

	@Operation(
		summary = "재고 상세 조회",
		description = "해당 매장의 재고 배치별로 상세 조회합니다."
	)
	@GetMapping("/{storePublicId}/{ingredientPublicId}/batches")
	public ResponseEntity<List<StockBatchResponse>> getIngredientBatches(
		@AuthenticationPrincipal CustomUserDetails principal,
		@PathVariable("storePublicId") UUID storePublicId,
		@PathVariable("ingredientPublicId") UUID ingredientPublicId
	) {
		List<StockBatchResponse> batchResponsesList = stockQueryService.getIngredientBatchDetails(principal.getUserId(),
			storePublicId, ingredientPublicId);

		return ResponseEntity.ok(batchResponsesList);
	}

}