package kr.inventory.domain.purchase.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.purchase.constant.PurchaseOrderConstant;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderCreateRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderUpdateRequest;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderDetailResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderSummaryResponse;
import kr.inventory.domain.purchase.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Order", description = "발주서 생성/조회/상태변경 API")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    @Operation(summary = "발주서 초안 생성")
    public ResponseEntity<PurchaseOrderDetailResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PurchaseOrderCreateRequest request
    ) {
        PurchaseOrderDetailResponse response = purchaseOrderService.createDraft(principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "발주서 목록 조회")
    public ResponseEntity<List<PurchaseOrderSummaryResponse>> getList(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam Long storeId
    ) {
        List<PurchaseOrderSummaryResponse> response = purchaseOrderService.getPurchaseOrders(principal.getUserId(), storeId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{purchaseOrderId}")
    @Operation(summary = "발주서 상세 조회")
    public ResponseEntity<PurchaseOrderDetailResponse> getDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long purchaseOrderId
    ) {
        PurchaseOrderDetailResponse response = purchaseOrderService.getPurchaseOrder(principal.getUserId(), purchaseOrderId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{purchaseOrderId}")
    @Operation(summary = "발주서 초안 수정")
    public ResponseEntity<PurchaseOrderDetailResponse> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long purchaseOrderId,
            @Valid @RequestBody PurchaseOrderUpdateRequest request
    ) {
        PurchaseOrderDetailResponse response = purchaseOrderService.updateDraft(principal.getUserId(), purchaseOrderId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{purchaseOrderId}/submit")
    @Operation(summary = "발주서 제출")
    public ResponseEntity<PurchaseOrderDetailResponse> submit(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long purchaseOrderId
    ) {
        PurchaseOrderDetailResponse response = purchaseOrderService.submit(principal.getUserId(), purchaseOrderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{purchaseOrderId}/confirm")
    @Operation(summary = "발주서 확정")
    public ResponseEntity<PurchaseOrderDetailResponse> confirm(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long purchaseOrderId
    ) {
        PurchaseOrderDetailResponse response = purchaseOrderService.confirm(principal.getUserId(), purchaseOrderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{purchaseOrderId}/cancel")
    @Operation(summary = "발주서 취소")
    public ResponseEntity<PurchaseOrderDetailResponse> cancel(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long purchaseOrderId
    ) {
        PurchaseOrderDetailResponse response = purchaseOrderService.cancel(principal.getUserId(), purchaseOrderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{purchaseOrderId}/pdf")
    @Operation(summary = "발주서 PDF 다운로드")
    public ResponseEntity<byte[]> downloadPdf(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long purchaseOrderId
    ) {
        byte[] pdfBytes = purchaseOrderService.downloadPdf(principal.getUserId(), purchaseOrderId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(PurchaseOrderConstant.PDF_FILENAME_PREFIX + purchaseOrderId + PurchaseOrderConstant.PDF_FILENAME_EXTENSION)
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
