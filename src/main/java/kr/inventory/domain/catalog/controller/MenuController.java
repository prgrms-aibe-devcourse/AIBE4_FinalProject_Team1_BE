package kr.inventory.domain.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.catalog.controller.dto.MenuCreateRequest;
import kr.inventory.domain.catalog.controller.dto.MenuResponse;
import kr.inventory.domain.catalog.controller.dto.MenuUpdateRequest;
import kr.inventory.domain.catalog.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "메뉴(Menu)", description = "매장 메뉴 관리 API")
@RestController
@RequestMapping("/api/menus/{storePublicId}")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "메뉴 생성", description = "새로운 메뉴를 등록합니다.")
    @PostMapping
    public ResponseEntity<UUID> createMenu(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid MenuCreateRequest request) {
        return ResponseEntity.ok(menuService.createMenu(principal.getUserId(), storePublicId, request));
    }

    @Operation(summary = "메뉴 목록 조회", description = "매장의 모든 메뉴를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<MenuResponse>> getMenus(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(menuService.getMenus(principal.getUserId(), storePublicId));
    }

    @Operation(summary = "메뉴 상세 조회", description = "특정 메뉴의 상세 정보를 조회합니다.")
    @GetMapping("/{menuPublicId}")
    public ResponseEntity<MenuResponse> getMenu(
            @PathVariable UUID storePublicId,
            @PathVariable UUID menuPublicId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(menuService.getMenu(principal.getUserId(), storePublicId, menuPublicId));
    }

    @Operation(summary = "메뉴 수정", description = "메뉴 정보를 수정합니다.")
    @PutMapping("/{menuPublicId}")
    public ResponseEntity<Void> updateMenu(
            @PathVariable UUID storePublicId,
            @PathVariable UUID menuPublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid MenuUpdateRequest request) {
        menuService.updateMenu(principal.getUserId(), storePublicId, menuPublicId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "메뉴 삭제", description = "메뉴를 삭제합니다.")
    @DeleteMapping("/{menuPublicId}")
    public ResponseEntity<Void> deleteMenu(
            @PathVariable UUID storePublicId,
            @PathVariable UUID menuPublicId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        menuService.deleteMenu(principal.getUserId(), storePublicId, menuPublicId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "매장 손님용 메뉴 목록 조회", description = "매장의 모든 메뉴를 조회합니다.")
    @GetMapping("/customer")
    public ResponseEntity<List<MenuResponse>> customerGetMenus(
            @PathVariable UUID storePublicId) {
        return ResponseEntity.ok(menuService.customerGetMenus(storePublicId));
    }
}
