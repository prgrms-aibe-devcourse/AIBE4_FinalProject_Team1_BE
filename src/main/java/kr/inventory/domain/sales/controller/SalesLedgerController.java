package kr.inventory.domain.sales.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.sales.controller.dto.request.SalesLedgerSearchRequest;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderDetailResponse;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderSummaryResponse;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerTotalSummaryResponse;
import kr.inventory.domain.sales.service.SalesLedgerService;
import kr.inventory.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sales/{storePublicId}/orders")
@Tag(name = "매출 내역(Sales Ledger)", description = "매출 내역 조회 API")
public class SalesLedgerController {

    private final SalesLedgerService salesLedgerService;

    @GetMapping
    @Operation(summary = "매출 내역 목록 조회", description = "기간/상태/주문유형 기준으로 매출 내역을 조회합니다.")
    public ResponseEntity<PageResponse<SalesLedgerOrderSummaryResponse>> getSalesLedgerOrders(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @Valid @ModelAttribute SalesLedgerSearchRequest request,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<SalesLedgerOrderSummaryResponse> response = salesLedgerService.getSalesLedgerOrders(
                principal.getUserId(),
                storePublicId,
                request,
                pageable
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @Operation(summary = "매출 내역 요약 조회", description = "기간/상태/주문유형 기준으로 매출 합계 데이터를 조회합니다.")
    public ResponseEntity<SalesLedgerTotalSummaryResponse> getSalesLedgerTotalSummary(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @Valid @ModelAttribute SalesLedgerSearchRequest request
    ) {
        SalesLedgerTotalSummaryResponse response = salesLedgerService.getSalesLedgerTotalSummary(
                principal.getUserId(),
                storePublicId,
                request
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderPublicId}")
    @Operation(summary = "매출 내역 상세 조회", description = "주문 단위의 매출 상세 내역을 조회합니다.")
    public ResponseEntity<SalesLedgerOrderDetailResponse> getSalesLedgerOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @PathVariable UUID orderPublicId
    ) {
        SalesLedgerOrderDetailResponse response = salesLedgerService.getSalesLedgerOrder(
                principal.getUserId(),
                storePublicId,
                orderPublicId
        );

        return ResponseEntity.ok(response);
    }
}
