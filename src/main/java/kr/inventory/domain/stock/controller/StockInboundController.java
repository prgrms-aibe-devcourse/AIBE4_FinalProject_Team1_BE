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

@RestController
@RequestMapping("/api/inbounds/{storePublicId}")
@Tag(name = "입고(Stock Inbound)", description = "재고 입고 관리 API")
@RequiredArgsConstructor
public class StockInboundController {

	private final StockInboundService stockInboundService;

	@Operation(summary = "입고 등록")
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

	@Operation(summary = "입고 단건 조회")
	@GetMapping("/{inboundId}")
	public ResponseEntity<StockInboundResponse> getInbound(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable("storePublicId") UUID storePublicId,
		@PathVariable("inboundId") UUID inboundPublicId
	) {
		StockInboundResponse response = stockInboundService.getInbound(inboundPublicId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "입고 목록 조회")
	@GetMapping
	public ResponseEntity<Page<StockInboundResponse>> getInbounds(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable("storePublicId") Long storePublicId,
		@PageableDefault(size = 20) Pageable pageable
	) {
		Page<StockInboundResponse> response = stockInboundService.getInbounds(storePublicId, pageable);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "입고 확정")
	@PostMapping("/{inboundId}/confirm")
	public ResponseEntity<Void> confirmInbound(
		@PathVariable("inboundId") UUID inboundPublicId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		stockInboundService.confirmInbound(inboundPublicId, userDetails.getUserId());
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "입고 삭제")
	@DeleteMapping("/{inboundId}")
	public ResponseEntity<Void> deleteInbound(
		@PathVariable("inboundId") UUID inboundPublicId
	) {
		stockInboundService.deleteInbound(inboundPublicId);
		return ResponseEntity.ok().build();
	}
}
