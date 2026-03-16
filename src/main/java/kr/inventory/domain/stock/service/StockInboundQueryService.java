package kr.inventory.domain.stock.service;

import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.stock.repository.StockInboundQueryRepository;
import kr.inventory.domain.stock.service.command.StockInboundSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockInboundQueryService {

    private final StockInboundQueryRepository stockInboundQueryRepository;

    public List<StockInboundSummary> getInboundList(
            Long userId,
            UUID storePublicId,
            InboundStatus status,
            String keyword,
            int limit
    ) {
        return stockInboundQueryRepository.findInboundSummaries(
                storePublicId,
                status,
                keyword,
                limit
        );
    }
}