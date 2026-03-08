package kr.inventory.domain.stock.repository.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.inventory.domain.stock.controller.dto.request.StockInboundSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.StockInboundItemResponse;
import kr.inventory.domain.stock.controller.dto.response.StockInboundResponse;
import kr.inventory.domain.stock.entity.QStockInbound;
import kr.inventory.domain.stock.entity.QStockInboundItem;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.stock.repository.StockInboundRepositoryCustom;
import kr.inventory.domain.vendor.entity.QVendor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class StockInboundRepositoryImpl implements StockInboundRepositoryCustom {
	private final JPAQueryFactory queryFactory;

	@Override
	public Page<StockInbound> searchInbounds(
			Long storeId,
			List<InboundStatus> statuses,
			StockInboundSearchRequest searchRequest,
			Pageable pageable
	) {
		QStockInbound stockInbound = QStockInbound.stockInbound;
		QVendor vendor = QVendor.vendor;
		QStockInboundItem item = QStockInboundItem.stockInboundItem;

		BooleanBuilder builder = new BooleanBuilder();

		builder.and(stockInbound.store.storeId.eq(storeId));

		if (statuses != null && !statuses.isEmpty()) {
			builder.and(stockInbound.status.in(statuses));
		}

		if (searchRequest != null) {
			if (searchRequest.vendorName() != null && !searchRequest.vendorName().isBlank()) {
				builder.and(stockInbound.vendor.name.containsIgnoreCase(searchRequest.vendorName().trim()));
			}

			if (searchRequest.inboundPublicId() != null && !searchRequest.inboundPublicId().isBlank()) {
				String keyword = searchRequest.inboundPublicId().trim().toLowerCase();
				builder.and(Expressions.stringTemplate(
						"LOWER(CAST({0} AS VARCHAR))",
						stockInbound.inboundPublicId
				).like("%" + keyword + "%"));
			}

			if (searchRequest.inboundDateFrom() != null) {
				builder.and(stockInbound.inboundDate.goe(searchRequest.inboundDateFrom()));
			}
			if (searchRequest.inboundDateTo() != null) {
				builder.and(stockInbound.inboundDate.loe(searchRequest.inboundDateTo()));
			}

			if (searchRequest.itemKeyword() != null && !searchRequest.itemKeyword().isBlank()) {
				String keyword = searchRequest.itemKeyword().trim();
				BooleanExpression itemExists = JPAExpressions
						.selectOne()
						.from(item)
						.where(
								item.inbound.eq(stockInbound),
								item.rawProductName.containsIgnoreCase(keyword)
										.or(item.normalizedRawKey.containsIgnoreCase(keyword))
						)
						.exists();
				builder.and(itemExists);
			}
		}

		Long total = queryFactory
				.select(stockInbound.count())
				.from(stockInbound)
				.leftJoin(stockInbound.vendor, vendor)
				.where(builder)
				.fetchOne();

		List<StockInbound> content = queryFactory
				.selectFrom(stockInbound)
				.leftJoin(stockInbound.vendor, vendor).fetchJoin()
				.leftJoin(stockInbound.store).fetchJoin()
				.where(builder)
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.orderBy(stockInbound.createdAt.desc())
				.fetch();

		return new PageImpl<>(content, pageable, total != null ? total : 0);
	}
}
