package kr.inventory.domain.stock.repository.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kr.inventory.domain.stock.controller.dto.request.StockTakeSheetSearchRequest;
import kr.inventory.domain.stock.entity.StockTakeSheet;
import kr.inventory.domain.stock.repository.StockTakeSheetRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static kr.inventory.domain.stock.entity.QStockTakeSheet.stockTakeSheet;

@RequiredArgsConstructor
public class StockTakeSheetRepositoryImpl implements StockTakeSheetRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<StockTakeSheet> findBySheetPublicIdAndStoreIdWithLock(UUID sheetPublicId, Long storeId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(stockTakeSheet)
                        .where(
                                stockTakeSheet.sheetPublicId.eq(sheetPublicId),
                                stockTakeSheet.storeId.eq(storeId)
                        )
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    @Override
    public Page<StockTakeSheet> searchStockTakeSheets(
            Long storeId,
            StockTakeSheetSearchRequest request,
            Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder()
                .and(stockTakeSheet.storeId.eq(storeId));

        if (request.title() != null && !request.title().isBlank()) {
            builder.and(stockTakeSheet.title.containsIgnoreCase(request.title().trim()));
        }

        if (request.from() != null) {
            builder.and(stockTakeSheet.createdAt.goe(request.from()));
        }

        if (request.to() != null) {
            builder.and(stockTakeSheet.createdAt.loe(request.to()));
        }

        List<StockTakeSheet> content = queryFactory
                .selectFrom(stockTakeSheet)
                .where(builder)
                .orderBy(stockTakeSheet.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(stockTakeSheet.count())
                .from(stockTakeSheet)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }
}