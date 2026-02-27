package kr.inventory.domain.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.response.StockDeductionResponse;
import kr.inventory.domain.stock.controller.dto.request.StockOrderDeductionRequest;
import kr.inventory.domain.stock.service.StockManagerFacade;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "재고(Stock)", description = "재고 관련 기능을 담당하는 API입니다.")
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

	private final StockManagerFacade stockManagerFacade;

	@Operation(
		summary = "주문 재고 차감",
		description = "주문 정보를 바탕으로 해당 매장의 상품 재고를 차감합니다."
	)
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

		return ResponseEntity.ok(
			StockDeductionResponse.from(request.salesOrderId(), "재고 차감 처리가 완료되었습니다.")
		);
	}
}
