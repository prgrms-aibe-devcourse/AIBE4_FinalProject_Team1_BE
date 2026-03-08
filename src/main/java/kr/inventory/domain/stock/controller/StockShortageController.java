package kr.inventory.domain.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.request.StockShortageSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.StockShortageGroupResponse;
import kr.inventory.domain.stock.service.StockShortageService;
import kr.inventory.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "재고 부족(StockShortage)", description = "재고 부족 현황 관리 API")
@RestController
@RequestMapping("/api/stock-shortages/{storePublicId}")
@RequiredArgsConstructor
public class StockShortageController {

    private final StockShortageService stockShortageService;

    @Operation(summary = "재고 부족 목록 조회", description = "매장의 모든 재고 부족 현황을 주문별로 묶어서 조회합니다.")
    @GetMapping
    public ResponseEntity<PageResponse<StockShortageGroupResponse>> getShortages(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @ModelAttribute StockShortageSearchRequest searchRequest,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(stockShortageService.getShortagesGroupedByOrder(principal.getUserId(), storePublicId, searchRequest, pageable));
    }
}
