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
import kr.inventory.global.config.swagger.PurchaseApiDocs;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/purchase-orders/{storePublicId}")
@RequiredArgsConstructor
@Tag(name = "Purchase Order", description = "발주서 생성/조회/상태변경 API")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    @Operation(summary = "발주서 초안 생성", description = PurchaseApiDocs.CREATE_DRAFT)
    public ResponseEntity<PurchaseOrderDetailResponse> create(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PurchaseOrderCreateRequest request
    ) {
        PurchaseOrderDetailResponse response = purchaseOrderService.createDraft(principal.getUserId(), storePublicId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "발주서 목록 조회", description = PurchaseApiDocs.LIST)
    public ResponseEntity<List<PurchaseOrderSummaryResponse>> getList(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        List<PurchaseOrderSummaryResponse> response = purchaseOrderService.getPurchaseOrders(principal.getUserId(), storePublicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{purchaseOrderPublicId}")
    @Operation(summary = "발주서 상세 조회", description = PurchaseApiDocs.DETAIL)
    public ResponseEntity<PurchaseOrderDetailResponse> getDetail(
            @PathVariable UUID storePublicId,
            @PathVariable UUID purchaseOrderPublicId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        PurchaseOrderDetailResponse response = purchaseOrderService.getPurchaseOrder(principal.getUserId(), storePublicId, purchaseOrderPublicId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{purchaseOrderPublicId}")
    @Operation(summary = "발주서 초안 수정", description = PurchaseApiDocs.UPDATE_DRAFT)
    public ResponseEntity<PurchaseOrderDetailResponse> update(
            @PathVariable UUID storePublicId,
            @PathVariable UUID purchaseOrderPublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PurchaseOrderUpdateRequest request
    ) {
        PurchaseOrderDetailResponse response = purchaseOrderService.updateDraft(principal.getUserId(), storePublicId, purchaseOrderPublicId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{purchaseOrderPublicId}/submit")
    @Operation(summary = "발주서 제출", description = PurchaseApiDocs.SUBMIT)
    public ResponseEntity<PurchaseOrderDetailResponse> submit(
            @PathVariable UUID storePublicId,
            @PathVariable UUID purchaseOrderPublicId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        PurchaseOrderDetailResponse response = purchaseOrderService.submit(principal.getUserId(), storePublicId, purchaseOrderPublicId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{purchaseOrderPublicId}/confirm")
    @Operation(summary = "발주서 확정", description = PurchaseApiDocs.CONFIRM)
    public ResponseEntity<PurchaseOrderDetailResponse> confirm(
            @PathVariable UUID storePublicId,
            @PathVariable UUID purchaseOrderPublicId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        PurchaseOrderDetailResponse response = purchaseOrderService.confirm(principal.getUserId(), storePublicId, purchaseOrderPublicId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{purchaseOrderPublicId}/cancel")
    @Operation(summary = "발주서 취소", description = PurchaseApiDocs.CANCEL)
    public ResponseEntity<PurchaseOrderDetailResponse> cancel(
            @PathVariable UUID storePublicId,
            @PathVariable UUID purchaseOrderPublicId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        PurchaseOrderDetailResponse response = purchaseOrderService.cancel(principal.getUserId(), storePublicId, purchaseOrderPublicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{purchaseOrderPublicId}/pdf")
    @Operation(summary = "발주서 PDF 다운로드", description = PurchaseApiDocs.DOWNLOAD_PDF)
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID storePublicId,
            @PathVariable UUID purchaseOrderPublicId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        byte[] pdfBytes = purchaseOrderService.downloadPdf(principal.getUserId(), storePublicId, purchaseOrderPublicId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(PurchaseOrderConstant.PDF_FILENAME_PREFIX + purchaseOrderPublicId + PurchaseOrderConstant.PDF_FILENAME_EXTENSION)
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
