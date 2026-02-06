package kr.dontworry.domain.category.service;

import kr.dontworry.domain.category.controller.dto.CategoryCreateRequest;
import kr.dontworry.domain.category.controller.dto.CategoryOrderRequest;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

        Integer maxOrder = categoryRepository.findMaxSortOrderByLedgerId(dto.ledgerId());
        int nextOrder = (maxOrder == null) ? 0 : maxOrder + 1;

        Category newCategory = Category.createCustom(ledger, dto.name(), dto.icon(), dto.color(), nextOrder);

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

        category.update(dto.name(), dto.icon(), dto.color());
        
        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.from(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        Category category = getCategoryWithOwnership(categoryId, userId);
        Integer deletedOrder = category.getSortOrder();
        Long ledgerId = category.getLedger().getLedgerId();

        category.remove();

        if (!category.isDefault() && deletedOrder != -1) {
            categoryRepository.shiftOrdersForward(ledgerId, deletedOrder);
        }
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

    public void reorderCategories(Long userId, List<CategoryOrderRequest> dto){
        List<UUID> publicIds = dto.stream()
                .map(CategoryOrderRequest::publicId)
                .toList();

        List<Category> categories = categoryRepository.findAllByPublicIdInWithLedger(publicIds);

        categories.forEach(category -> category.validateOwnership(userId));

        Map<UUID, Integer> orderMap = dto.stream()
                .collect(Collectors.toMap(CategoryOrderRequest::publicId, CategoryOrderRequest::sortOrder));

        for (Category category : categories) {
            category.changeSortOrder(orderMap.get(category.getPublicId()));
        }
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