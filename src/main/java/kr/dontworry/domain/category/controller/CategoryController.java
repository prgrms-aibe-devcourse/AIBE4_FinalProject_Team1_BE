package kr.dontworry.domain.category.controller;

import jakarta.validation.Valid;
import kr.dontworry.domain.category.controller.dto.CategoryCreateRequest;
import kr.dontworry.domain.category.controller.dto.CategoryResponse;
import kr.dontworry.domain.category.controller.dto.CategoryUpdateRequest;
import kr.dontworry.domain.category.service.CategoryReadService;
import kr.dontworry.domain.category.service.CategoryService;
import kr.dontworry.domain.ledger.service.LedgerReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final LedgerReadService ledgerReadService;
    private final CategoryReadService categoryReadService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ledger/{publicId}")
    public ResponseEntity<List<CategoryResponse>> getCategoriesByLedger(@PathVariable UUID publicId) {
        Long ledgerId = ledgerReadService.resolveInternalId(publicId);

        List<CategoryResponse> responses = categoryService.getCategoriesByLedger(ledgerId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/ledger/{publicId}/active")
    public ResponseEntity<List<CategoryResponse>> getActiveCategoriesByLedger(@PathVariable UUID publicId) {
        Long ledgerId = ledgerReadService.resolveInternalId(publicId);

        List<CategoryResponse> responses = categoryService.getActiveCategoriesByLedger(ledgerId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID publicId,
            @Valid @RequestBody CategoryUpdateRequest request) {
        Long categoryId = categoryReadService.resolveInternalId(publicId);

        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID publicId) {
        Long categoryId = categoryReadService.resolveInternalId(publicId);

        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{publicId}/activate")
    public ResponseEntity<CategoryResponse> activateCategory(@PathVariable UUID publicId) {
        Long categoryId = categoryReadService.resolveInternalId(publicId);

        CategoryResponse response = categoryService.activateCategory(categoryId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{publicId}/deactivate")
    public ResponseEntity<CategoryResponse> deactivateCategory(@PathVariable UUID publicId) {
        Long categoryId = categoryReadService.resolveInternalId(publicId);

        CategoryResponse response = categoryService.deactivateCategory(categoryId);
        return ResponseEntity.ok(response);
    }
}