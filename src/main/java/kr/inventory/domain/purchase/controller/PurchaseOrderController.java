package kr.inventory.domain.purchase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.purchase.constant.PurchaseOrderConstant;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderCreateRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderSearchRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderUpdateRequest;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderDetailResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderSummaryResponse;
import kr.inventory.domain.purchase.service.PurchaseOrderService;
import kr.inventory.global.config.PurchaseApiDocs;
import kr.inventory.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/purchase-orders/{storePublicId}")
@Tag(name = "발주(Purchase Order)", description = "발주서 생성/조회/수정/취소/PDF API")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @Operation(summary = "발주서 생성", description = PurchaseApiDocs.CREATE_DRAFT)
    @PostMapping
    public ResponseEntity<PurchaseOrderDetailResponse> create(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PurchaseOrderCreateRequest request)
    {
            PurchaseOrderDetailResponse response = purchaseOrderService.create(principal.getUserId(),storePublicId, request);
            return ResponseEntity.ok(response);
    }

    @Operation(summary = "발주서 목록 조회", description = PurchaseApiDocs.LIST)
    @GetMapping
    public ResponseEntity<PageResponse<PurchaseOrderSummaryResponse>> getList(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @ModelAttribute PurchaseOrderSearchRequest searchRequest,
            @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable)
    {
            PageResponse<PurchaseOrderSummaryResponse> response = purchaseOrderService.getPurchaseOrders(
                    principal.getUserId(),
                    storePublicId,
                    searchRequest,
                    pageable);

            return ResponseEntity.ok(response);
    }

    @Operation(summary = "발주서 상세 조회", description = PurchaseApiDocs.DETAIL)
    @GetMapping("/{purchaseOrderPublicId}")
    public ResponseEntity<PurchaseOrderDetailResponse> getDetail(
            @PathVariable UUID storePublicId,
            @PathVariable UUID purchaseOrderPublicId,
            @AuthenticationPrincipal CustomUserDetails principal)
    {
            PurchaseOrderDetailResponse response = purchaseOrderService.getPurchaseOrder(
                    principal.getUserId(),
                    storePublicId,
                    purchaseOrderPublicId);

            return ResponseEntity.ok(response);
    }

    @Operation(summary = "발주서 수정", description = PurchaseApiDocs.UPDATE_DRAFT)
    @PutMapping("/{purchaseOrderPublicId}")
    public ResponseEntity<PurchaseOrderDetailResponse> update(
            @PathVariable UUID storePublicId,
            @PathVariable UUID purchaseOrderPublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody PurchaseOrderUpdateRequest request)
    {
            PurchaseOrderDetailResponse response = purchaseOrderService.update(
                    principal.getUserId(),
                    storePublicId,
                    purchaseOrderPublicId,
                    request);

            return ResponseEntity.ok(response);
    }

    @Operation(summary = "발주서 취소", description = PurchaseApiDocs.CANCEL)
    @PostMapping("/{purchaseOrderPublicId}/cancel")
    public ResponseEntity<PurchaseOrderDetailResponse> cancel(
            @PathVariable UUID storePublicId,
            @PathVariable UUID purchaseOrderPublicId,
            @AuthenticationPrincipal CustomUserDetails principal)
    {
            PurchaseOrderDetailResponse response = purchaseOrderService.cancel(
                    principal.getUserId(),
                    storePublicId,
                    purchaseOrderPublicId);

            return ResponseEntity.ok(response);
    }

    @Operation(summary = "발주서 PDF 다운로드", description = PurchaseApiDocs.DOWNLOAD_PDF)
    @GetMapping("/{purchaseOrderPublicId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID storePublicId,
            @PathVariable UUID purchaseOrderPublicId,
            @AuthenticationPrincipal CustomUserDetails principal)
    {
            byte[] pdfBytes = purchaseOrderService.downloadPdf(principal.getUserId(), storePublicId, purchaseOrderPublicId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(PurchaseOrderConstant.PDF_FILENAME_PREFIX
                                    + purchaseOrderPublicId
                                    + PurchaseOrderConstant.PDF_FILENAME_EXTENSION)
                            .build());

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
