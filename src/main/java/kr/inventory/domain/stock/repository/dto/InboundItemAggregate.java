package kr.inventory.domain.stock.repository.dto;

import java.math.BigDecimal;

/**
 * 입고 목록(헤더) 조회에서 라인 아이템 집계 값을 담기 위한 DTO.
 */
public record InboundItemAggregate(
        Long inboundId,
        long itemCount,
        long unresolvedItemCount,
        BigDecimal totalCost
) {

    public static InboundItemAggregate empty(Long inboundId) {
        return new InboundItemAggregate(inboundId, 0L, 0L, BigDecimal.ZERO);
    }
}
