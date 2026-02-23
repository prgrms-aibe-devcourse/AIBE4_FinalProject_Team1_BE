package kr.inventory.domain.stock.controller;

import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.StockDeductionResponse;
import kr.inventory.domain.stock.controller.dto.StockOrderDeductionRequest;
import kr.inventory.domain.stock.service.StockManagerFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockManagerFacade stockManagerFacade;

    @PostMapping("/{storePublicId}/deduct")
    public ResponseEntity<StockDeductionResponse> deductStock(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("storePublicId") UUID publicId,
            @RequestBody @Valid StockOrderDeductionRequest request
    ) {
        stockManagerFacade.processOrderStockDeduction(
                principal.getUserId(),
                publicId,
                request
        );

        return ResponseEntity.ok(new StockDeductionResponse(
                request.salesOrderId(),
                "SUCCESS",
                "재고 차감 처리가 완료되었습니다."
        ));
    }
}