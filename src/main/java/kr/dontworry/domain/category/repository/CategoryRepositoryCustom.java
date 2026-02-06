package kr.dontworry.domain.category.repository;

import kr.dontworry.domain.category.entity.Category;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepositoryCustom {
    Optional<Long> findCategoryIdByPublicId(UUID publicId);
    Optional<Category> findByIdWithLedger(Long categoryId);
}
