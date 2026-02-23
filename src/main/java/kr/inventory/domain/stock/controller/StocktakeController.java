package kr.inventory.domain.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.StocktakeCreateRequest;
import kr.inventory.domain.stock.controller.dto.StocktakeSheetResponse;
import kr.inventory.domain.stock.service.StocktakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "재고 실사(Stocktake)", description = "매장 재고 실사 관련 기능을 담당하는 API입니다.")
@RestController
@RequestMapping("/api/stocktakes/{storePublicId}")
@RequiredArgsConstructor
public class StocktakeController {
    private final StocktakeService stocktakeService;

    @Operation(
            summary = "재고 실사 시트 목록 조회",
            description = "특정 매장의 모든 재고 실사 시트 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<List<StocktakeSheetResponse>> getSheets(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(stocktakeService.getStocktakeSheets(principal.getUserId(), storePublicId));
    }

    @Operation(
            summary = "재고 실사 시트 생성",
            description = "특정 매장의 재고 조사를 위한 새로운 실사 시트를 생성합니다."
    )
    @PostMapping
    public ResponseEntity<Long> createSheet(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid StocktakeCreateRequest request) {
        return ResponseEntity.ok(stocktakeService.createStocktakeSheet(principal.getUserId(), storePublicId, request));
    }

    @Operation(
            summary = "재고 실사 확정",
            description = "입력된 실사 수량을 바탕으로 장부상 재고를 업데이트하고 조사를 종료합니다."
    )
    @PostMapping("/{sheetId}/confirm")
    public ResponseEntity<Void> confirmSheet(
            @PathVariable UUID storePublicId,
            @PathVariable Long sheetId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        stocktakeService.confirmSheet(principal.getUserId(), storePublicId, sheetId);
        return ResponseEntity.noContent().build();
    }
}