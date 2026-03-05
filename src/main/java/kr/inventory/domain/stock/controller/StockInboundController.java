package kr.inventory.domain.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.request.StockInboundRequest;
import kr.inventory.domain.stock.controller.dto.response.StockInboundResponse;
import kr.inventory.domain.stock.service.StockInboundService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "입고(Inbound)", description = "입고 관련 기능을 담당하는 API입니다.")

@RestController
@RequestMapping("/api/inbounds/{storePublicId}")
@RequiredArgsConstructor
public class StockInboundController {

	private final StockInboundService stockInboundService;

	@Operation(
		summary = "입고 등록",
		description = "사용자의 입력값을 바탕으로 해당 매장의 상품 입고를 등록합니다."
	)
	@PostMapping
	public ResponseEntity<StockInboundResponse> createInbound(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable("storePublicId") UUID storePublicId,
		@RequestBody @Valid StockInboundRequest request
	) {
		StockInboundResponse response = stockInboundService.createInbound(userDetails.getUserId(), storePublicId,
			request);
		return ResponseEntity.ok(response);
	}

	@Operation(
		summary = "입고 상세조회",
		description = "해당 매장의 입고 상세정보를 조회합니다."
	)
	@GetMapping("/{inboundPublicId}")
	public ResponseEntity<StockInboundResponse> getInbound(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable("storePublicId") UUID storePublicId,
		@PathVariable("inboundPublicId") UUID inboundPublicId
	) {
		StockInboundResponse response = stockInboundService.getInbound(userDetails.getUserId(), storePublicId,
			inboundPublicId);
		return ResponseEntity.ok(response);
	}

	@Operation(
		summary = "입고 목록 조회",
		description = "해당 매장의 입고 목록을 조회합니다."
	)
	@GetMapping
	public ResponseEntity<Page<StockInboundResponse>> getInbounds(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable("storePublicId") UUID storePublicId,
		@PageableDefault(size = 20) Pageable pageable
	) {
		Page<StockInboundResponse> response = stockInboundService.getInbounds(userDetails.getUserId(), storePublicId,
			pageable);
		return ResponseEntity.ok(response);
	}

	@Operation(
		summary = "입고 확정",
		description = "해당 매장의 등록된 입고를 확정해 재고로 배치합니다."
	)
	@PostMapping("/{inboundPublicId}/confirm")
	public ResponseEntity<Void> confirmInbound(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable("storePublicId") UUID storePublicId,
		@PathVariable("inboundPublicId") UUID inboundPublicId
	) {
		stockInboundService.confirmInbound(userDetails.getUserId(), storePublicId, inboundPublicId);
		return ResponseEntity.ok().build();
	}

	@Operation(
		summary = "입고 삭제",
		description = "해당매장의 등록된 입고 정보를 삭제합니다."
	)
	@DeleteMapping("/{inboundPublicId}")
	public ResponseEntity<Void> deleteInbound(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable("storePublicId") UUID storePublicId,
		@PathVariable("inboundPublicId") UUID inboundPublicId
	) {
		stockInboundService.deleteInbound(userDetails.getUserId(), storePublicId, inboundPublicId);
		return ResponseEntity.ok().build();
	}
}
