package kr.inventory.domain.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.request.StockTakeCreateRequest;
import kr.inventory.domain.stock.controller.dto.request.StockTakeItemsDraftUpdateRequest;
import kr.inventory.domain.stock.controller.dto.response.StockTakeDetailResponse;
import kr.inventory.domain.stock.controller.dto.response.StockTakeSheetResponse;
import kr.inventory.domain.stock.service.StockTakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "재고 실사(StockTake)", description = "매장 재고 실사 관리 API")
@RestController
@RequestMapping("/api/stocktakes/{storePublicId}")
@RequiredArgsConstructor
public class StockTakeController {
	private final StockTakeService stockTakeService;

	@Operation(
		summary = "재고 실사 시트 목록 조회",
		description = "특정 매장의 모든 재고 실사 시트 목록을 조회합니다."
	)
	@GetMapping
	public ResponseEntity<List<StockTakeSheetResponse>> getSheets(
		@PathVariable UUID storePublicId,
		@AuthenticationPrincipal CustomUserDetails principal) {
		return ResponseEntity.ok(stockTakeService.getStockTakeSheets(principal.getUserId(), storePublicId));
	}

    @Operation(
            summary = "재고 실사 시트 상세 조회",
            description = "특정 실사 시트의 상세 정보와 포함된 항목들을 조회합니다."
    )
    @GetMapping("/{sheetPublicId}")
    public ResponseEntity<StockTakeDetailResponse> getSheetDetail(
            @PathVariable UUID storePublicId,
            @PathVariable UUID sheetPublicId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(stockTakeService.getStockTakeSheetDetail(principal.getUserId(), storePublicId, sheetPublicId));
    }

	@Operation(
		summary = "재고 실사 시트 생성",
		description = "특정 매장의 재고 조사를 위한 새로운 실사 시트를 생성합니다."
	)
	@PostMapping
	public ResponseEntity<UUID> createSheet(
		@PathVariable UUID storePublicId,
		@AuthenticationPrincipal CustomUserDetails principal,
		@RequestBody @Valid StockTakeCreateRequest request) {
		return ResponseEntity.ok(stockTakeService.createStockTakeSheet(principal.getUserId(), storePublicId, request));
	}

    @Operation(
            summary = "재고 실사 항목 임시저장(수정)",
            description = "특정 실사 시트의 항목 수량(stockTakeQty)을 수정하여 임시저장합니다. (CONFIRMED 상태는 수정 불가)"
    )
    @PatchMapping("/{sheetPublicId}/items")
    public ResponseEntity<Void> saveDraftItems(
            @PathVariable UUID storePublicId,
            @PathVariable UUID sheetPublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid StockTakeItemsDraftUpdateRequest request
    ) {
        stockTakeService.updateDraftItems(principal.getUserId(), storePublicId, sheetPublicId, request);
        return ResponseEntity.noContent().build();
    }

	@Operation(
		summary = "재고 실사 확정",
		description = "입력된 실사 수량을 바탕으로 장부상 재고를 업데이트하고 조사를 종료합니다."
	)
	@PostMapping("/{sheetPublicId}/confirm")
	public ResponseEntity<Void> confirmSheet(
		@PathVariable UUID storePublicId,
		@PathVariable UUID sheetPublicId,
		@AuthenticationPrincipal CustomUserDetails principal) {
		stockTakeService.confirmSheet(principal.getUserId(), storePublicId, sheetPublicId);
		return ResponseEntity.noContent().build();
	}
}