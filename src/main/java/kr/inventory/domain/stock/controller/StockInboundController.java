package kr.inventory.domain.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.request.IngredientConfirmRequest;
import kr.inventory.domain.stock.controller.dto.request.ManualInboundRequest;
import kr.inventory.domain.stock.controller.dto.request.StockInboundSearchRequest;
import kr.inventory.domain.stock.controller.dto.request.UpdateNormalizationRequest;
import kr.inventory.domain.stock.controller.dto.response.*;
import kr.inventory.domain.stock.normalization.service.IngredientResolutionService;
import kr.inventory.domain.stock.normalization.service.ProductNormalizationService;
import kr.inventory.domain.stock.service.StockInboundService;
import kr.inventory.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "입고(Inbound)", description = "입고 API")
@RestController
@RequestMapping("/api/stores/{storePublicId}/inbounds")
@RequiredArgsConstructor
public class StockInboundController {

    private final StockInboundService stockInboundService;
    private final IngredientResolutionService ingredientResolutionService;
    private final ProductNormalizationService productNormalizationService;

    @Operation(summary = "수기 입고 등록")
    @PostMapping
    public ResponseEntity<StockInboundResponse> createManualInbound(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID storePublicId,
        @RequestBody @Valid ManualInboundRequest request
    ) {
        StockInboundResponse response = stockInboundService.createManualInbound(principal.getUserId(), storePublicId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "입고 확정")
    @PostMapping("/{inboundPublicId}/confirm")
    public ResponseEntity<Void> confirmInbound(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID storePublicId,
        @PathVariable UUID inboundPublicId
    ) {
        stockInboundService.confirmInbound(principal.getUserId(), storePublicId, inboundPublicId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "입고 아이템 전체 재료 정규화")
    @PostMapping("/{inboundPublicId}/items/ingredient-mapping/resolve")
    public ResponseEntity<BulkResolveResponse> resolveAllIngredientMapping(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @PathVariable UUID inboundPublicId
    ) {
        BulkResolveResponse response = ingredientResolutionService.resolveAllForInbound(
                principal.getUserId(),
                storePublicId,
                inboundPublicId
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "입고 아이템 재료 매핑 확정 (기존 재료 선택 또는 새 재료 생성)")
    @PutMapping("/{inboundPublicId}/items/{inboundItemPublicId}/ingredient-mapping")
    public ResponseEntity<IngredientConfirmResponse> confirmIngredientMapping(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @PathVariable UUID inboundPublicId,
            @PathVariable UUID inboundItemPublicId,
            @RequestBody IngredientConfirmRequest request
    ) {
        IngredientConfirmResponse response = ingredientResolutionService.confirm(
                principal.getUserId(),
                storePublicId,
                inboundPublicId,
                inboundItemPublicId,
                request
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "입고 아이템 수량 정규화 업데이트 (재료 매핑 없이 정규화 정보만 저장)")
    @PutMapping("/{inboundPublicId}/items/{inboundItemPublicId}/normalization")
    public ResponseEntity<Void> updateInboundItemNormalization(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @PathVariable UUID inboundPublicId,
            @PathVariable UUID inboundItemPublicId,
            @RequestBody UpdateNormalizationRequest request
    ) {
        stockInboundService.updateItemNormalization(
                principal.getUserId(),
                storePublicId,
                inboundPublicId,
                inboundItemPublicId,
                request
        );
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "입고 아이템 전체 상품명 정규화")
    @PostMapping("/{inboundPublicId}/items/product-name/normalize")
    public ResponseEntity<BulkProductNormalizeResponse> normalizeAllProductNames(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @PathVariable UUID inboundPublicId
    ) {
        BulkProductNormalizeResponse response = productNormalizationService.normalizeAllForInbound(
                principal.getUserId(),
                storePublicId,
                inboundPublicId
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "입고 목록 조회")
    @GetMapping
    public ResponseEntity<PageResponse<StockInboundListResponse>> getInbounds(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @ModelAttribute StockInboundSearchRequest searchRequest,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<StockInboundListResponse> response = stockInboundService.getInbounds(
                principal.getUserId(),
                storePublicId,
                searchRequest,
                pageable
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "입고 상세 조회")
    @GetMapping("/{inboundPublicId}")
    public ResponseEntity<StockInboundResponse> getInbound(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @PathVariable UUID inboundPublicId
    ) {
        StockInboundResponse response = stockInboundService.getInbound(principal.getUserId(), storePublicId, inboundPublicId);
        return ResponseEntity.ok(response);
    }
}
