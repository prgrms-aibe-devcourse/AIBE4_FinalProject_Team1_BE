package kr.inventory.domain.stock.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.request.StockLogSearchCondition;
import kr.inventory.domain.stock.controller.dto.response.StockLogResponse;
import kr.inventory.domain.stock.controller.dto.response.WastePageResponse;
import kr.inventory.domain.stock.controller.dto.response.WasteResponse;
import kr.inventory.domain.stock.service.StockLogService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stockLogs/{storePublicId}")
public class StockLogController {
	private final StockLogService stockLogService;

	@GetMapping
	public ResponseEntity<WastePageResponse<StockLogResponse>> getStockLogs(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable UUID storePublicId,
		StockLogSearchCondition condition,
		Pageable pageable
	) {
		Page<StockLogResponse> response = stockLogService.getStockLogs(userDetails.getUserId(), storePublicId,
			condition, pageable);
		return ResponseEntity.ok(WastePageResponse.from(response));
	}
}
