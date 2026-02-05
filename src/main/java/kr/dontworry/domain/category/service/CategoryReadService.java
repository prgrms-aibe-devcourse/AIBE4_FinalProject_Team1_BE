package kr.dontworry.domain.category.service;

import kr.dontworry.domain.category.exception.CategoryErrorCode;
import kr.dontworry.domain.category.exception.CategoryException;
import kr.dontworry.domain.category.repository.CategoryRepository;
import kr.dontworry.global.common.PublicIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryReadService implements PublicIdResolver {
    private final CategoryRepository categoryRepository;

    @Override
    public Long resolveInternalId(UUID publicId) {
        return categoryRepository.findCategoryIdByPublicId(publicId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }
}
