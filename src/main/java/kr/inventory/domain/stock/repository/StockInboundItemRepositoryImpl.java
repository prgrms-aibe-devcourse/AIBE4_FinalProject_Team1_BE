package kr.inventory.domain.stock.repository;

import static kr.inventory.domain.catalog.entity.QIngredient.*;
import static kr.inventory.domain.stock.entity.QStockInboundItem.*;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import kr.inventory.domain.stock.controller.dto.StockInboundItemResponse;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StockInboundItemRepositoryImpl implements StockInboundItemRepositoryCustom {

	private final JPAQueryFactory queryFactory;

}