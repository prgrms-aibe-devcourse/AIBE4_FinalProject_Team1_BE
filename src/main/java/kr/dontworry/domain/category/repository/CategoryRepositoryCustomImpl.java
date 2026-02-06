package kr.dontworry.domain.category.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.dontworry.domain.category.entity.Category;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

import static kr.dontworry.domain.category.entity.QCategory.category;
import static kr.dontworry.domain.ledger.entity.QLedger.ledger;

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

    @Override
    public Optional<Category> findByIdWithLedger(Long categoryId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(category)
                        .join(category.ledger, ledger).fetchJoin()
                        .where(category.categoryId.eq(categoryId))
                        .fetchOne()
        );
    }
}
