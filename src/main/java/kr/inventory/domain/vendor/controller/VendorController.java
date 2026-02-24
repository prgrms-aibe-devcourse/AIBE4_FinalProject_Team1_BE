package kr.inventory.domain.vendor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.vendor.controller.dto.VendorCreateRequest;
import kr.inventory.domain.vendor.controller.dto.VendorResponse;
import kr.inventory.domain.vendor.controller.dto.VendorUpdateRequest;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.vendor.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Vendor", description = "거래처 관리")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @Operation(summary = "거래처 등록", description = "새로운 거래처를 등록합니다")
    @PostMapping("/stores/{storeId}/vendors")
    public ResponseEntity<VendorResponse> createVendor(
            @PathVariable Long storeId,
            @Valid @RequestBody VendorCreateRequest request
    ) {
        VendorResponse response = vendorService.createVendor(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "거래처 목록 조회", description = "매장의 거래처 목록을 조회합니다")
    @GetMapping("/stores/{storeId}/vendors")
    public ResponseEntity<List<VendorResponse>> getVendorsByStore(
            @PathVariable Long storeId,
            @RequestParam(required = false) VendorStatus status
    ) {
        List<VendorResponse> response = vendorService.getVendorsByStore(storeId, status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "거래처 상세 조회", description = "거래처 상세 정보를 조회합니다")
    @GetMapping("/vendors/{vendorId}")
    public ResponseEntity<VendorResponse> getVendor(
            @PathVariable Long vendorId
    ) {
        VendorResponse response = vendorService.getVendor(vendorId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "거래처 수정", description = "거래처 정보를 수정합니다")
    @PatchMapping("/vendors/{vendorId}")
    public ResponseEntity<VendorResponse> updateVendor(
            @PathVariable Long vendorId,
            @Valid @RequestBody VendorUpdateRequest request
    ) {
        VendorResponse response = vendorService.updateVendor(vendorId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "거래처 삭제(비활성화)", description = "거래처를 비활성화합니다")
    @DeleteMapping("/vendors/{vendorId}")
    public ResponseEntity<Void> deactivateVendor(
            @PathVariable Long vendorId
    ) {
        vendorService.deactivateVendor(vendorId);
        return ResponseEntity.noContent().build();
    }
}
