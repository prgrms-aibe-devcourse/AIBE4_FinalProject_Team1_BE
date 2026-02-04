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

        return CategoryResponse.from(categoryRepository.save(newCategory));
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
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest dto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        if (category.isDefault() && !category.getName().equals(dto.name())) {
            throw new CategoryException(CategoryErrorCode.CANNOT_EDIT_DEFAULT_CATEGORY_NAME);
        }

        category.update(dto.name(), dto.icon(), dto.color(), dto.sortOrder());
        
        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.from(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        
        category.remove();
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