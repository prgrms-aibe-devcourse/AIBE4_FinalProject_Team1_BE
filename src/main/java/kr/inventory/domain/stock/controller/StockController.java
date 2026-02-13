package kr.inventory.domain.stock.controller;

import jakarta.validation.Valid;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.stock.controller.dto.StockRequestDto;
import kr.inventory.domain.stock.service.StockManagerFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockManagerFacade stockManagerFacade;
    private final SalesOrderRepository salesOrderRepository;

    @PostMapping("/deduct")
    public ResponseEntity<StockRequestDto.DeductionResponse> deductStock(
            @RequestBody @Valid StockRequestDto.OrderDeductionRequest request
    ) {
        stockManagerFacade.processOrderStockDeduction(request);

        return ResponseEntity.ok(new StockRequestDto.DeductionResponse(
                request.salesOrderId(),
                "SUCCESS",
                "재고 차감 처리가 완료되었습니다."
        ));
    }
}