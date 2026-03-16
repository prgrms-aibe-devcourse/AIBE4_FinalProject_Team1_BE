package kr.inventory.ai.stock.service;

import kr.inventory.ai.stock.tool.dto.request.InboundListToolRequest;
import kr.inventory.ai.stock.tool.dto.response.InboundListItemToolResponse;
import kr.inventory.ai.stock.tool.dto.response.InboundListToolResponse;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.stock.service.StockInboundQueryService;
import kr.inventory.domain.stock.service.command.StockInboundSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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
                request.status(),
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

    private InboundListItemToolResponse toToolResponse(StockInboundSummary summary) {
        return new InboundListItemToolResponse(
                summary.inboundPublicId(),
                summary.inboundDate(),
                summary.status().name(),
                summary.vendorName(),
                summary.itemCount(),
                summary.isConfirmed(),
                summary.confirmedByName(),
                summary.confirmedAt(),
                summary.hasItemsNeedingNormalization(),
                resolveAvailableActions(summary)
        );
    }

    private List<String> resolveAvailableActions(StockInboundSummary summary) {
        if (summary.hasItemsNeedingNormalization()) {
            return List.of("상세 보기", "정규화 필요 항목 보기", "확정 여부 확인");
        }

        if (summary.status() == InboundStatus.DRAFT) {
            return List.of("상세 보기", "확정 여부 확인");
        }

        return List.of("상세 보기");
    }
}