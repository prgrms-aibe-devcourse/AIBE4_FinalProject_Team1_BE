package kr.inventory.domain.stock.controller;

import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.StocktakeDto;
import kr.inventory.domain.stock.service.StocktakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/stocktakes/{storePublicId}")
@RequiredArgsConstructor
public class StocktakeController {
    private final StocktakeService stocktakeService;

    @PostMapping
    public ResponseEntity<Long> createSheet(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid StocktakeDto.CreateRequest request) {
        return ResponseEntity.ok(stocktakeService.createStocktakeSheet(principal.getUserId(), storePublicId, request));
    }

    @PostMapping("/{sheetId}/confirm")
    public ResponseEntity<Void> confirmSheet(
            @PathVariable UUID storePublicId,
            @PathVariable Long sheetId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        stocktakeService.confirmSheet(principal.getUserId(), storePublicId, sheetId);
        return ResponseEntity.noContent().build();
    }
}
