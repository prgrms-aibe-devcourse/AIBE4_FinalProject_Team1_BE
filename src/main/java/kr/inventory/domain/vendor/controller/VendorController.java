package kr.inventory.domain.vendor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.vendor.controller.dto.VendorCreateRequest;
import kr.inventory.domain.vendor.controller.dto.VendorResponse;
import kr.inventory.domain.vendor.controller.dto.VendorUpdateRequest;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.vendor.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "거래처(Vendor)", description = "거래처 관리 API")
@RestController
@RequestMapping("/api/vendors/{storePublicId}")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @Operation(summary = "거래처 등록", description = "새로운 거래처를 등록합니다")
    @PostMapping
    public ResponseEntity<VendorResponse> createVendor(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody VendorCreateRequest request
    ) {
        VendorResponse response = vendorService.createVendor(principal.getUserId(), storePublicId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "거래처 목록 조회", description = "매장의 거래처 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<List<VendorResponse>> getVendorsByStore(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false, defaultValue = "ACTIVE") VendorStatus status
    ) {
        List<VendorResponse> response = vendorService.getVendorsByStore(principal.getUserId(), storePublicId, status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "거래처 상세 조회", description = "거래처 상세 정보를 조회합니다")
    @GetMapping("/{vendorPublicId}")
    public ResponseEntity<VendorResponse> getVendor(
            @PathVariable UUID storePublicId,
            @PathVariable UUID vendorPublicId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        VendorResponse response = vendorService.getVendor(storePublicId, vendorPublicId, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "거래처 수정", description = "거래처 정보를 수정합니다")
    @PatchMapping("/{vendorPublicId}")
    public ResponseEntity<VendorResponse> updateVendor(
            @PathVariable UUID storePublicId,
            @PathVariable UUID vendorPublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody VendorUpdateRequest request
    ) {
        VendorResponse response = vendorService.updateVendor(storePublicId, vendorPublicId, principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "거래처 삭제(비활성화)", description = "거래처를 비활성화합니다")
    @DeleteMapping("/{vendorPublicId}")
    public ResponseEntity<Void> deactivateVendor(
            @PathVariable UUID storePublicId,
            @PathVariable UUID vendorPublicId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        vendorService.deactivateVendor(storePublicId, vendorPublicId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
