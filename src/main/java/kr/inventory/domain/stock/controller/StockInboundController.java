package kr.inventory.domain.stock.controller;

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
@RequestMapping("/api/inbounds")
@RequiredArgsConstructor
public class StockInboundController {

	private final StockInboundService stockInboundService;

	@PostMapping
	public ResponseEntity<StockInboundResponse> createInbound(
		@RequestBody @Valid StockInboundRequest request
	) {
		StockInboundResponse response = stockInboundService.createInbound(request);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{inboundId}")
	public ResponseEntity<StockInboundResponse> getInbound(
		@PathVariable("inboundId") UUID inboundPublicId
	) {
		StockInboundResponse response = stockInboundService.getInbound(inboundPublicId);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<Page<StockInboundResponse>> getInbounds(
		@RequestParam("storeId") Long storeId,
		@PageableDefault(size = 20) Pageable pageable
	) {
		Page<StockInboundResponse> response = stockInboundService.getInbounds(storeId, pageable);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{inboundId}/confirm")
	public ResponseEntity<Void> confirmInbound(
		@PathVariable("inboundId") UUID inboundPublicId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		stockInboundService.confirmInbound(inboundPublicId, userDetails.getUserId());
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{inboundId}")
	public ResponseEntity<Void> deleteInbound(
		@PathVariable("inboundId") UUID inboundPublicId
	) {
		stockInboundService.deleteInbound(inboundPublicId);
		return ResponseEntity.ok().build();
	}
}
