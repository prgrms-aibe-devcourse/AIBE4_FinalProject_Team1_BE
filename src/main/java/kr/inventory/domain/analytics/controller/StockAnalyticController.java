package kr.inventory.domain.analytics.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inventory.domain.analytics.controller.dto.response.StockAnalyticResponse;
import kr.inventory.domain.analytics.service.StockAnalyticService;
import kr.inventory.domain.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;

@Tag(name = "재고 분석", description = "재고, 폐기 분석 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock-analysis/{storePublicId}")
public class StockAnalyticController {

	private final StockAnalyticService stockAnalyticService;

	@Operation(summary = "매장별 통합 재고 분석 조회", description = "현재고 현황과 누적 폐기 데이터를 통합하여 분석 결과를 반환합니다.")
	@GetMapping
	public ResponseEntity<List<StockAnalyticResponse>> getStockAnalysis(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable UUID storePublicId
	) {
		List<StockAnalyticResponse> responses = stockAnalyticService.getIntegratedAnalysis(userDetails.getUserId(),
			storePublicId);

		return ResponseEntity.ok(responses);
	}
}
