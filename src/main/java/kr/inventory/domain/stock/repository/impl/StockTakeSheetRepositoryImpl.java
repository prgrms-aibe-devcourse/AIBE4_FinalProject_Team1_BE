package kr.inventory.domain.stock.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.LockModeType;
import kr.inventory.domain.stock.entity.StockTakeSheet;
import kr.inventory.domain.stock.repository.StockTakeSheetRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static kr.inventory.domain.stock.entity.QStockTakeSheet.stockTakeSheet;

@RequiredArgsConstructor
public class StockTakeSheetRepositoryImpl implements StockTakeSheetRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<StockTakeSheet> findByIdAndStoreIdWithLock(Long sheetId, Long storeId) {
		return Optional.ofNullable(
			queryFactory
				.selectFrom(stockTakeSheet)
				.where(
					stockTakeSheet.sheetId.eq(sheetId),
					stockTakeSheet.storeId.eq(storeId)
				)
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.fetchOne()
		);
	}
}