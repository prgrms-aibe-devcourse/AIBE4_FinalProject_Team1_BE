package kr.inventory.domain.stock.controller.dto.request;

import java.time.LocalDate;

public record StockSearchRequest(
	String ingredientName,    // 품목명 검색
	Boolean includeZeroStock, // 재고 0인 품목 포함 여부
	LocalDate expiryBefore    // 특정 날짜 이전 유통기한 품목만 보기
) {
}
