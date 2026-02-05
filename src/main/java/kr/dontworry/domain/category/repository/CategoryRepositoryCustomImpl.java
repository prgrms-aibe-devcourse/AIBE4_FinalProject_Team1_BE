package kr.dontworry.domain.category.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

import static kr.dontworry.domain.category.entity.QCategory.category;

@RequiredArgsConstructor
public class CategoryRepositoryCustomImpl implements CategoryRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Long> findCategoryIdByPublicId(UUID publicId) {
        return Optional.ofNullable(
                queryFactory
                        .select(category.categoryId)
                        .from(category)
                        .where(category.publicId.eq(publicId))
                        .fetchOne()
        );
    }
}
