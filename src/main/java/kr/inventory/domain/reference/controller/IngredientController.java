package kr.inventory.domain.reference.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.reference.controller.dto.request.IngredientCreateRequest;
import kr.inventory.domain.reference.controller.dto.request.IngredientUpdateRequest;
import kr.inventory.domain.reference.controller.dto.response.IngredientResponse;
import kr.inventory.domain.reference.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "식재료(Ingredient)", description = "매장 식재료 관리 API")
@RestController
@RequestMapping("/api/stores/{storePublicId}/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @Operation(summary = "식재료 생성", description = "새로운 식재료를 등록합니다.")
    @PostMapping
    public ResponseEntity<IngredientResponse> createIngredient(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid IngredientCreateRequest request) {
        IngredientResponse response = ingredientService.createIngredient(principal.getUserId(), storePublicId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "식재료 목록 조회", description = "매장의 모든 식재료를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<IngredientResponse>> getIngredients(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<IngredientResponse> responses = ingredientService.getIngredients(principal.getUserId(), storePublicId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "식재료 상세 조회", description = "특정 식재료의 상세 정보를 조회합니다.")
    @GetMapping("/{ingredientPublicId}")
    public ResponseEntity<IngredientResponse> getIngredient(
            @PathVariable UUID storePublicId,
            @PathVariable UUID ingredientPublicId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        IngredientResponse response = ingredientService.getIngredient(principal.getUserId(), storePublicId, ingredientPublicId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "식재료 수정", description = "식재료 정보를 수정합니다.")
    @PutMapping("/{ingredientPublicId}")
    public ResponseEntity<IngredientResponse> updateIngredient(
            @PathVariable UUID storePublicId,
            @PathVariable UUID ingredientPublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid IngredientUpdateRequest request) {
        IngredientResponse response = ingredientService.updateIngredient(principal.getUserId(), storePublicId, ingredientPublicId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "식재료 삭제", description = "식재료를 삭제합니다.")
    @DeleteMapping("/{ingredientPublicId}")
    public ResponseEntity<Void> deleteIngredient(
            @PathVariable UUID storePublicId,
            @PathVariable UUID ingredientPublicId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ingredientService.deleteIngredient(principal.getUserId(), storePublicId, ingredientPublicId);
        return ResponseEntity.noContent().build();
    }
}
