package kr.inventory.domain.stock.service;

import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.StockInboundItemRepository;
import kr.inventory.domain.stock.repository.StockInboundQueryRepository;
import kr.inventory.domain.stock.repository.StockInboundRepository;
import kr.inventory.domain.stock.service.command.StockInboundDetailResult;
import kr.inventory.domain.stock.service.command.StockInboundSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockInboundQueryService {

    private final StockInboundQueryRepository stockInboundQueryRepository;
    private final StockInboundRepository stockInboundRepository;
    private final StockInboundItemRepository stockInboundItemRepository;

    public List<StockInboundSummary> getInboundList(
            Long storeId,
            String keyword,
            OffsetDateTime from,
            OffsetDateTime to,
            int limit
    ) {
        return stockInboundQueryRepository.findInboundSummaries(
                storeId,
                keyword,
                from,
                to,
                limit
        );
    }

    public StockInboundDetailResult getInboundDetail(
            Long storeId,
            UUID inboundPublicId
    ) {
        StockInbound inbound = stockInboundRepository.findByInboundPublicIdAndStore_StoreId(
                        inboundPublicId,
                        storeId
                )
                .orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));

        List<StockInboundItem> inboundItems =
                stockInboundItemRepository.findByInbound_InboundIdOrderByInboundItemIdAsc(inbound.getInboundId());

        List<StockInboundDetailResult.StockInboundItemDetail> items = inboundItems.stream()
                .map(StockInboundDetailResult.StockInboundItemDetail::toItemDetail)
                .toList();

        return new StockInboundDetailResult(
                inbound.getInboundPublicId(),
                inbound.getInboundDate(),
                inbound.getCreatedAt(),
                inbound.getVendor() != null ? inbound.getVendor().getName() : null,
                inbound.getStatus().name(),
                items
        );
    }
}