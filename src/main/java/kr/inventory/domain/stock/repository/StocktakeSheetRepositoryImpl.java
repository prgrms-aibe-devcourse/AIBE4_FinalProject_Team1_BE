package kr.inventory.domain.stock.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kr.inventory.domain.stock.entity.StocktakeSheet;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static kr.inventory.domain.stock.entity.QStocktakeSheet.stocktakeSheet;

@RequiredArgsConstructor
public class StocktakeSheetRepositoryImpl implements StocktakeSheetRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<StocktakeSheet> findByIdAndStoreIdWithLock(Long sheetId, Long storeId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(stocktakeSheet)
                        .where(
                                stocktakeSheet.sheetId.eq(sheetId),
                                stocktakeSheet.storeId.eq(storeId)
                        )
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }
}