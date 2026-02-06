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
import kr.dontworry.domain.ledger.exception.LedgerErrorCode;
import kr.dontworry.domain.ledger.exception.LedgerException;
import kr.dontworry.domain.ledger.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final LedgerRepository ledgerRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest dto, Long userId) {
        Ledger ledger = ledgerRepository.findById(dto.ledgerId())
                .orElseThrow(() -> new LedgerException(LedgerErrorCode.LEDGER_NOT_FOUND));

        ledger.validateOwner(userId);

        Category newCategory = Category.createCustom(
                ledger,
                dto.name(),
                dto.icon(),
                dto.color(),
                dto.sortOrder()
        );

        return CategoryResponse.from(categoryRepository.save(newCategory));
    }

    public List<CategoryResponse> getCategoriesByLedger(Long ledgerId, Long userId) {
        validateLedgerAccess(ledgerId, userId);

        List<Category> categories = categoryRepository.findByLedger_LedgerIdOrderBySortOrderAsc(ledgerId);

        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public List<CategoryResponse> getActiveCategoriesByLedger(Long ledgerId, Long userId) {
        validateLedgerAccess(ledgerId, userId);

        List<Category> categories = categoryRepository.findByLedger_LedgerIdAndStatusOrderBySortOrderAsc(ledgerId, CategoryStatus.ACTIVE);

        return categories.stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest dto, Long userId) {
        Category category = getCategoryWithOwnership(categoryId, userId);

        if (category.isDefault() && !category.getName().equals(dto.name())) {
            throw new CategoryException(CategoryErrorCode.CANNOT_EDIT_DEFAULT_CATEGORY_NAME);
        }

        category.update(dto.name(), dto.icon(), dto.color(), dto.sortOrder());
        
        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.from(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        Category category = getCategoryWithOwnership(categoryId, userId);
        category.remove();
    }

    @Transactional
    public CategoryResponse activateCategory(Long categoryId, Long userId) {
        Category category = getCategoryWithOwnership(categoryId, userId);
        category.activate();
        
        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.from(updatedCategory);
    }

    @Transactional
    public CategoryResponse deactivateCategory(Long categoryId, Long userId) {
        Category category = getCategoryWithOwnership(categoryId, userId);
        category.deactivate();
        
        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.from(updatedCategory);
    }

    private void validateLedgerAccess(Long ledgerId, Long userId) {
        Ledger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new LedgerException(LedgerErrorCode.LEDGER_NOT_FOUND));
        ledger.validateOwner(userId);
    }

    private Category getCategoryWithOwnership(Long categoryId, Long userId) {
        Category category = categoryRepository.findByIdWithLedger(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        category.validateOwnership(userId);
        return category;
    }
}