package kr.inventory.domain.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inventory.domain.analytics.controller.dto.request.MenuRankingSearchRequest;
import kr.inventory.domain.analytics.controller.dto.request.SalesPeakSearchRequest;
import kr.inventory.domain.analytics.controller.dto.request.SalesSummarySearchRequest;
import kr.inventory.domain.analytics.controller.dto.request.SalesTrendSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.MenuRankingResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesPeakResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesSummaryResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesTrendResponse;
import kr.inventory.domain.analytics.service.SalesAnalyticsService;
import kr.inventory.domain.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics/{storePublicId}/sales")
@Tag(name = "매출 분석(Sales Analytics)", description = "매출 분석 대시보드용 집계 API")
@RequiredArgsConstructor
public class SalesAnalyticsController {

    private final SalesAnalyticsService salesAnalyticsService;

    @Operation(summary = "매출 추이 조회", description = "일/주/월 단위 매출 추이를 조회합니다.")
    @GetMapping("/trend")
    public ResponseEntity<List<SalesTrendResponse>> getSalesTrend(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @ModelAttribute SalesTrendSearchRequest request
    ) {
        List<SalesTrendResponse> response = salesAnalyticsService.getSalesTrend(
                principal.getUserId(), storePublicId,
                request.from(), request.to(), request.interval());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "요일×시간대 피크 조회", description = "요일과 시간대별 주문 집중도를 조회합니다.")
    @GetMapping("/peak")
    public ResponseEntity<List<SalesPeakResponse>> getSalesPeak(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @ModelAttribute SalesPeakSearchRequest request
    ) {
        List<SalesPeakResponse> response = salesAnalyticsService.getSalesPeak(
                principal.getUserId(), storePublicId,
                request.from(), request.to());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "메뉴 TOP N 조회", description = "판매량 기준 상위 메뉴를 조회합니다.")
    @GetMapping("/menu-ranking")
    public ResponseEntity<List<MenuRankingResponse>> getMenuRanking(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @ModelAttribute MenuRankingSearchRequest request
    ) {
        List<MenuRankingResponse> response = salesAnalyticsService.getMenuRanking(
                principal.getUserId(), storePublicId,
                request.from(), request.to(), request.topN());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "매출 요약 조회", description = "총 매출, 객단가 등 요약 지표를 조회합니다.")
    @GetMapping("/summary")
    public ResponseEntity<SalesSummaryResponse> getSalesSummary(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @ModelAttribute SalesSummarySearchRequest request
    ) {
        SalesSummaryResponse response = salesAnalyticsService.getSalesSummary(
                principal.getUserId(), storePublicId,
                request.from(), request.to(), request.interval());

        return ResponseEntity.ok(response);
    }
}

