package kr.inventory.domain.store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.store.controller.dto.request.*;
import kr.inventory.domain.store.controller.dto.response.*;
import kr.inventory.domain.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores")
@Tag(name = "매장(Store)", description = "매장관리 API")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @Operation(summary = "매장 등록")
    @PostMapping
    public ResponseEntity<StoreCreateResponse> createStore(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody StoreCreateRequest request
    ) {
        StoreCreateResponse response = storeService.createStore(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "내 소속 매장 목록 조회")
    @GetMapping
    public ResponseEntity<List<MyStoreResponse>> getMyStores(
        @AuthenticationPrincipal CustomUserDetails principal
    ) {
        List<MyStoreResponse> stores = storeService.getMyStores(principal.getUserId());
        return ResponseEntity.ok(stores);
    }

    @Operation(summary = "매장 단건 조회")
    @GetMapping("/{storePublicId}")
    public ResponseEntity<MyStoreResponse> getStoreByPublicId(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID storePublicId
    ) {
        MyStoreResponse response = storeService.getStoreByPublicId(principal.getUserId(), storePublicId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "매장 상호명 변경")
    @PatchMapping("/{storePublicId}/name")
    public ResponseEntity<MyStoreResponse> updateStoreName(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @Valid @RequestBody StoreNameUpdateRequest request
    ) {
        MyStoreResponse response = storeService.updateStoreName(principal.getUserId(), storePublicId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "대표 매장 설정")
    @PostMapping("/{storePublicId}/default")
    public ResponseEntity<Void> setDefaultStore(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId
    ) {
        storeService.setDefaultStore(principal.getUserId(), storePublicId);
        return ResponseEntity.ok().build();
    }
}
