package kr.inventory.domain.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.request.StockTakeConfirmRequest;
import kr.inventory.domain.stock.controller.dto.request.StockTakeDraftSaveRequest;
import kr.inventory.domain.stock.controller.dto.request.StockTakeSheetCreateRequest;
import kr.inventory.domain.stock.controller.dto.request.StockTakeSheetSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.StockTakeDetailResponse;
import kr.inventory.domain.stock.controller.dto.response.StockTakeSheetResponse;
import kr.inventory.domain.stock.service.StockTakeService;
import kr.inventory.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "재고 실사(StockTake)", description = "매장 재고 실사 관리 API")
@RestController
@RequestMapping("/api/stocktakes/{storePublicId}")
@RequiredArgsConstructor
public class StockTakeController {
	private final StockTakeService stockTakeService;

    @Operation(
            summary = "재고 실사 시트 목록 조회",
            description = "특정 매장의 재고 실사 시트 목록을 페이징 및 검색 조건으로 조회합니다."
    )
    @GetMapping
    public ResponseEntity<PageResponse<StockTakeSheetResponse>> getSheets(
            @PathVariable UUID storePublicId,
            StockTakeSheetSearchRequest request,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(
                stockTakeService.getStockTakeSheets(
                        principal.getUserId(),
                        storePublicId,
                        request,
                        pageable
                )
        );
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
		description = "특정 매장의 재고 실사 시트를 생성하고, 생성 시점의 장부 재고(snapshot)를 저장합니다."
	)
	@PostMapping
	public ResponseEntity<UUID> createStockTakeSheet(
		@PathVariable UUID storePublicId,
		@AuthenticationPrincipal CustomUserDetails principal,
		@RequestBody @Valid StockTakeSheetCreateRequest request) {
		return ResponseEntity.ok(stockTakeService.createStockTakeSheet(principal.getUserId(), storePublicId, request));
	}

    @Operation(
            summary = "재고 실사 초안 저장",
            description = "특정 실사 시트의 현재 작성 상태를 초안으로 저장합니다. (CONFIRMED 상태는 수정 불가)"
    )
    @PutMapping("/{sheetPublicId}/draft")
    public ResponseEntity<Void> saveStockTakeDraft(
            @PathVariable UUID storePublicId,
            @PathVariable UUID sheetPublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid StockTakeDraftSaveRequest request
    ) {
        stockTakeService.saveStockTakeDraft(principal.getUserId(), storePublicId, sheetPublicId, request);
        return ResponseEntity.noContent().build();
    }

	@Operation(
		summary = "재고 실사 확정",
		description = "현재 입력된 최종 실사 수량을 기준으로 재고를 조정하고 실사 시트를 확정합니다."
	)
	@PostMapping("/{sheetPublicId}/confirm")
	public ResponseEntity<Void> confirmStockTakeSheet(
		@PathVariable UUID storePublicId,
		@PathVariable UUID sheetPublicId,
		@AuthenticationPrincipal CustomUserDetails principal,
        @RequestBody @Valid StockTakeConfirmRequest request
    ) {
		stockTakeService.confirmStockTakeSheet(principal.getUserId(), storePublicId, sheetPublicId, request);
		return ResponseEntity.noContent().build();
	}
}