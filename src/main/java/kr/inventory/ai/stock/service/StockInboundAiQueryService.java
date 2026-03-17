package kr.inventory.ai.stock.service;

import kr.inventory.ai.stock.tool.dto.request.InboundDetailToolRequest;
import kr.inventory.ai.stock.tool.dto.request.InboundListToolRequest;
import kr.inventory.ai.stock.tool.dto.response.InboundDetailItemToolResponse;
import kr.inventory.ai.stock.tool.dto.response.InboundDetailToolResponse;
import kr.inventory.ai.stock.tool.dto.response.InboundListItemToolResponse;
import kr.inventory.ai.stock.tool.dto.response.InboundListToolResponse;
import kr.inventory.domain.stock.service.StockInboundQueryService;
import kr.inventory.domain.stock.service.command.StockInboundDetailResult;
import kr.inventory.domain.stock.service.command.StockInboundSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockInboundAiQueryService {

    private final StockInboundQueryService stockInboundQueryService;

    public InboundListToolResponse getInboundList(
            Long userId,
            UUID storePublicId,
            InboundListToolRequest request
    ) {
        List<StockInboundSummary> summaries = stockInboundQueryService.getInboundList(
                userId,
                storePublicId,
                request.normalizedKeyword(),
                request.resolvedLimit()
        );

        List<InboundListItemToolResponse> inbounds = summaries.stream()
                .map(this::toToolResponse)
                .toList();

        return new InboundListToolResponse(
                inbounds.size(),
                inbounds
        );
    }

    public InboundDetailToolResponse getInboundDetail(
            Long userId,
            java.util.UUID storePublicId,
            InboundDetailToolRequest request
    ) {
        StockInboundDetailResult detail = stockInboundQueryService.getInboundDetail(
                userId,
                storePublicId,
                request.inboundPublicId()
        );

        List<InboundDetailItemToolResponse> items = detail.items().stream()
                .map(this::toDetailItemResponse)
                .toList();

        BigDecimal totalCost = items.stream()
                .map(InboundDetailItemToolResponse::amount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String vendorName = detail.vendorName() != null ? detail.vendorName() : "거래처 정보 없음";

        String message = "%s / %s / %s 입고의 상세 정보입니다."
                .formatted(
                        detail.inboundPublicId(),
                        detail.createdAt(),
                        vendorName
                );

        return new InboundDetailToolResponse(
                message,
                detail.inboundPublicId(),
                detail.inboundDate(),
                detail.createdAt(),
                vendorName,
                detail.status(),
                totalCost,
                items
        );
    }

    private InboundListItemToolResponse toToolResponse(StockInboundSummary summary) {
        return new InboundListItemToolResponse(
                summary.inboundPublicId(),
                summary.inboundDate(),
                summary.vendorName(),
                summary.itemCount(),
                summary.confirmedByName(),
                summary.confirmedAt()
        );
    }

    private InboundDetailItemToolResponse toDetailItemResponse(StockInboundDetailResult.StockInboundItemDetail item) {
        BigDecimal amount = null;
        if (item.quantity() != null && item.unitCost() != null) {
            amount = item.quantity().multiply(item.unitCost());
        }

        return new InboundDetailItemToolResponse(
                item.inboundItemPublicId(),
                item.rawProductName(),
                item.mappedIngredientName(),
                item.quantity(),
                item.unitCost(),
                amount,
                item.expirationDate(),
                item.resolutionStatus()
        );
    }
}