package kr.inventory.domain.stock.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.request.WasteRequest;
import kr.inventory.domain.stock.controller.dto.request.WasteSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.WasteResponse;
import kr.inventory.domain.stock.service.WasteService;
import kr.inventory.global.common.PageResponse;
import lombok.RequiredArgsConstructor;

@Tag(name = "폐기(disposal)", description = "폐기 관련 기능을 담당하는 API입니다.")
@RestController
@RequestMapping("/api/disposal/{storePublicId}")
@RequiredArgsConstructor
public class WasteController {

	private final WasteService wasteService;

	@Operation(
		summary = "폐기 등록",
		description = "사용자의 입력값을 바탕으로 해당 매장의 상품 폐기를 등록합니다."
	)
	@PostMapping
	public ResponseEntity<Void> recordWaste(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable UUID storePublicId,
		@RequestBody @Valid WasteRequest request
	) {
		wasteService.recordWaste(userDetails.getUserId(), storePublicId, request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@Operation(
		summary = "폐기목록 조회",
		description = "사용자가 등록한 해당 매장의 상품 폐기목록을 조회합니다."
	)
	@GetMapping
	public ResponseEntity<PageResponse<WasteResponse>> getWasteRecords(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable UUID storePublicId,
		WasteSearchRequest condition,
		Pageable pageable
	) {
		Page<WasteResponse> responses = wasteService.getWasteRecords(userDetails.getUserId(), storePublicId, condition,
			pageable);
		return ResponseEntity.ok(PageResponse.from(responses));
	}
}
