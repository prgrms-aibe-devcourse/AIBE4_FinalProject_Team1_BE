package kr.inventory.domain.stock.repository;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StockInboundItemRepositoryImpl implements StockInboundItemRepositoryCustom {

	private final JPAQueryFactory queryFactory;

}