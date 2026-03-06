package kr.inventory.domain.stock.repository.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.inventory.domain.stock.controller.dto.response.StockInboundItemResponse;
import kr.inventory.domain.stock.controller.dto.response.StockInboundResponse;
import kr.inventory.domain.stock.entity.QStockInbound;
import kr.inventory.domain.stock.entity.QStockInboundItem;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.repository.StockInboundRepositoryCustom;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StockInboundRepositoryImpl implements StockInboundRepositoryCustom {
	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<StockInboundResponse> findInboundWithItems(UUID inboundPublicId, Long storeId) {
		QStockInbound stockInbound = QStockInbound.stockInbound;
		QStockInboundItem inboundItem = QStockInboundItem.stockInboundItem;

		StockInbound result = queryFactory
			.selectFrom(stockInbound)
			.where(
				stockInbound.inboundPublicId.eq(inboundPublicId),
				stockInbound.store.storeId.eq(storeId)
			)
			.fetchOne();

		if (result == null)
			return Optional.empty();

		List<StockInboundItemResponse> itemResponses = queryFactory
			.select(Projections.constructor(StockInboundItemResponse.class,
				inboundItem.inboundItemId,
				inboundItem.inbound.inboundId,
				inboundItem.ingredient.name, // ingredientId 추출
				inboundItem.rawProductName,         // ingredientName 추출
				inboundItem.quantity,
				inboundItem.unitCost,
				inboundItem.expirationDate
			))
			.from(inboundItem)
			.join(inboundItem.ingredient) // 이름을 가져오기 위해 조인
			.where(inboundItem.inbound.eq(result))
			.fetch();

		// 3. DTO와 DTO 리스트를 결합하여 반환
		return Optional.of(StockInboundResponse.from(result, itemResponses));
	}
}
