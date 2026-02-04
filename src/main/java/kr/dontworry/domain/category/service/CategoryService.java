package kr.dontworry.domain.category.service;

import kr.dontworry.domain.category.controller.dto.CategoryCreateRequest;
import kr.dontworry.domain.category.controller.dto.CategoryResponse;
import kr.dontworry.domain.category.controller.dto.CategoryUpdateRequest;
import kr.dontworry.domain.category.entity.Category;
import kr.dontworry.domain.category.entity.enums.CategoryStatus;
import kr.dontworry.domain.category.exception.CategoryErrorCode;
import kr.dontworry.domain.category.exception.CategoryException;
import kr.dontworry.domain.category.repository.CategoryRepository;
import kr.dontworry.domain.ledger.entity.Ledger;
import kr.dontworry.domain.ledger.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final LedgerRepository ledgerRepository;

    public CategoryResponse createCategory(CategoryCreateRequest dto) {
        Ledger ledger = ledgerRepository.findById(dto.ledgerId())
                .orElseThrow();

        Category newCategory = Category.createCustom(
                ledger,
                dto.name(),
                dto.icon(),
                dto.color(),
                dto.sortOrder()
        );

        Category savedCategory = categoryRepository.save(newCategory);
        return CategoryResponse.from(savedCategory);
    }

    public List<CategoryResponse> getCategoriesByLedger(Long ledgerId) {
        List<Category> categories = categoryRepository.findByLedger_LedgerId(ledgerId);
        return categories.stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> getActiveCategoriesByLedger(Long ledgerId) {
        List<Category> categories = categoryRepository.findByLedger_LedgerIdAndStatus(ledgerId, CategoryStatus.ACTIVE);
        return categories.stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        category.update(request.name(), request.icon(), request.color(), request.sortOrder());
        
        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.from(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        // TODO: 카테고리 삭제 방법은 토의 후 결정
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        
        categoryRepository.delete(category);
    }

    @Transactional
    public CategoryResponse activateCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        
        category.activate();
        
        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.from(updatedCategory);
    }

    @Transactional
    public CategoryResponse deactivateCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        
        category.deactivate();
        
        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.from(updatedCategory);
    }
}