package kr.dontworry.domain.category.repository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepositoryCustom {
    Optional<Long> findCategoryIdByPublicId(UUID publicId);
}
