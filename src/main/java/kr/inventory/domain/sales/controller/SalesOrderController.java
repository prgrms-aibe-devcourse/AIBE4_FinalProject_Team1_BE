package kr.inventory.domain.sales.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.sales.controller.dto.request.SalesOrderCreateRequest;
import kr.inventory.domain.sales.controller.dto.response.SalesOrderResponse;
import kr.inventory.domain.sales.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "주문(Sales Order)", description = "주문관리API")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @Operation(summary = "주문 생성 (결제 처리)", description = "주문 생성 및 재고 차감을 동시에 처리합니다.")
    @PostMapping
    public ResponseEntity<SalesOrderResponse> createOrder(
            @CookieValue(name = "sessionToken") String sessionToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SalesOrderCreateRequest request
    ) {
        SalesOrderResponse response = salesOrderService.createOrder(
                sessionToken,
                idempotencyKey,
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "매장 주문 목록 조회 (관리자)", description = "매장의 모든 주문을 조회합니다.")
    @GetMapping("/{storePublicId}")
    public ResponseEntity<List<SalesOrderResponse>> getStoreOrders(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId
    ) {
        List<SalesOrderResponse> response = salesOrderService.getStoreOrders(
                principal.getUserId(),
                storePublicId
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "주문 상세 조회 (관리자)", description = "특정 주문의 상세 정보를 조회합니다.")
    @GetMapping("/{storePublicId}/{orderPublicId}")
    public ResponseEntity<SalesOrderResponse> getOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @PathVariable UUID orderPublicId
    ) {
        SalesOrderResponse response = salesOrderService.getOrder(
                orderPublicId,
                principal.getUserId(),
                storePublicId
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "환불 처리 (관리자)", description = "주문을 환불 처리합니다. 재고는 복구되지 않습니다.")
    @PostMapping("/{storePublicId}/{orderPublicId}/refund")
    public ResponseEntity<SalesOrderResponse> refundOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @PathVariable UUID orderPublicId
    ) {
        SalesOrderResponse response = salesOrderService.refundOrder(
                orderPublicId,
                principal.getUserId(),
                storePublicId
        );

        return ResponseEntity.ok(response);
    }
}















































































































































































































































































































































