package kr.inventory.domain.stock.controller;

import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.stock.controller.dto.StockRequestDto;
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
    private final SalesOrderRepository salesOrderRepository;

    @PostMapping("/{storePublicId}/deduct")
    public ResponseEntity<StockRequestDto.DeductionResponse> deductStock(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable("storePublicId") UUID publicId,
            @RequestBody @Valid StockRequestDto.OrderDeductionRequest request
    ) {
        stockManagerFacade.processOrderStockDeduction(
                principal.getUserId(),
                publicId,
                request
        );

        return ResponseEntity.ok(new StockRequestDto.DeductionResponse(
                request.salesOrderId(),
                "SUCCESS",
                "재고 차감 처리가 완료되었습니다."
        ));
    }
}