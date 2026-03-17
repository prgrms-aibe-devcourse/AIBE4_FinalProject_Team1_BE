package kr.inventory.ai.stock.service;

import kr.inventory.ai.stock.tool.dto.request.InboundDetailToolRequest;
import kr.inventory.ai.stock.tool.dto.request.InboundListToolRequest;
import kr.inventory.ai.stock.tool.dto.response.InboundDetailToolResponse;
import kr.inventory.ai.stock.tool.dto.response.InboundListToolResponse;
import kr.inventory.domain.stock.service.StockInboundQueryService;
import kr.inventory.domain.stock.service.command.StockInboundDetailResult;
import kr.inventory.domain.stock.service.command.StockInboundSummary;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockInboundAiQueryService {

    private final StockInboundQueryService stockInboundQueryService;
    private final StoreAccessValidator storeAccessValidator;

    public InboundListToolResponse getInboundList(
            Long userId,
            UUID storePublicId,
            InboundListToolRequest request
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        List<StockInboundSummary> summaries = stockInboundQueryService.getInboundList(
                storeId,
                request.normalizedKeyword(),
                request.resolvedLimit()
        );

        return InboundListToolResponse.from(summaries);
    }

    public InboundDetailToolResponse getInboundDetail(
            Long userId,
            UUID storePublicId,
            InboundDetailToolRequest request
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        StockInboundDetailResult detail = stockInboundQueryService.getInboundDetail(
                storeId,
                request.inboundPublicId()
        );

        return InboundDetailToolResponse.from(detail);
    }
}