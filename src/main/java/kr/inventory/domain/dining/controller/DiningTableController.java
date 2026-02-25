package kr.inventory.domain.dining.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.dining.controller.dto.request.DiningTableCreateRequest;
import kr.inventory.domain.dining.controller.dto.response.DiningTableResponse;
import kr.inventory.domain.dining.controller.dto.request.DiningTableUpdateRequest;
import kr.inventory.domain.dining.service.DiningTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "테이블(DiningTable)", description = "매장 테이블 관리 API")
@RestController
@RequestMapping("/api/tables/{storePublicId}")
@RequiredArgsConstructor
public class DiningTableController {

    private final DiningTableService diningTableService;

    @Operation(summary = "테이블 생성", description = "새로운 테이블을 등록합니다.")
    @PostMapping
    public ResponseEntity<UUID> createTable(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid DiningTableCreateRequest request) {
        return ResponseEntity.ok(diningTableService.createTable(principal.getUserId(), storePublicId, request));
    }

    @Operation(summary = "테이블 목록 조회", description = "매장의 모든 테이블을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<DiningTableResponse>> getTables(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(diningTableService.getTables(principal.getUserId(), storePublicId));
    }

    @Operation(summary = "테이블 상세 조회", description = "특정 테이블의 상세 정보를 조회합니다.")
    @GetMapping("/{tablePublicId}")
    public ResponseEntity<DiningTableResponse> getTable(
            @PathVariable UUID storePublicId,
            @PathVariable UUID tablePublicId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(diningTableService.getTable(principal.getUserId(), storePublicId, tablePublicId));
    }

    @Operation(summary = "테이블 수정", description = "테이블 정보를 수정합니다.")
    @PutMapping("/{tablePublicId}")
    public ResponseEntity<Void> updateTable(
            @PathVariable UUID storePublicId,
            @PathVariable UUID tablePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid DiningTableUpdateRequest request) {
        diningTableService.updateTable(principal.getUserId(), storePublicId, tablePublicId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "테이블 삭제", description = "테이블을 삭제합니다.")
    @DeleteMapping("/{tablePublicId}")
    public ResponseEntity<Void> deleteTable(
            @PathVariable UUID storePublicId,
            @PathVariable UUID tablePublicId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        diningTableService.deleteTable(principal.getUserId(), storePublicId, tablePublicId);
        return ResponseEntity.noContent().build();
    }
}