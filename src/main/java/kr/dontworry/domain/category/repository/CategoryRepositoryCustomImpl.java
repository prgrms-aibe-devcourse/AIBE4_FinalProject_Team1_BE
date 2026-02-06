package kr.dontworry.domain.category.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import kr.dontworry.domain.category.entity.Category;
import kr.dontworry.domain.category.entity.enums.CategoryStatus;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static kr.dontworry.domain.category.entity.QCategory.category;
import static kr.dontworry.domain.ledger.entity.QLedger.ledger;

@RequiredArgsConstructor
public class CategoryRepositoryCustomImpl implements CategoryRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

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

    @Override
    public List<Category> findAllByPublicIdInWithLedger(List<UUID> publicIds) {
        return queryFactory
                .selectFrom(category)
                .join(category.ledger, ledger).fetchJoin()
                .where(category.publicId.in(publicIds))
                .fetch();
    }

    @Override
    public Integer findMaxSortOrderByLedgerId(Long ledgerId) {
        return queryFactory
                .select(category.sortOrder.max().coalesce(-1))
                .from(category)
                .where(category.ledger.ledgerId.eq(ledgerId))
                .fetchOne();
    }

    @Override
    public void shiftOrdersForward(Long ledgerId, Integer startOrder) {
        long affectedRows = queryFactory
                .update(category)
                .set(category.sortOrder, category.sortOrder.subtract(1))
                .where(
                        category.ledger.ledgerId.eq(ledgerId),
                        category.sortOrder.gt(startOrder),
                        category.status.ne(CategoryStatus.DELETED)
                )
                .execute();

        if (affectedRows > 0) {
            em.flush();
            em.clear();
        }
    }
}
