package kr.inventory.domain.stock.normalization.service;

import kr.inventory.domain.stock.controller.dto.response.BulkProductNormalizeResponse;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.normalization.model.BulkProductNormalizeResult;
import kr.inventory.domain.stock.normalization.normalizer.ProductNameNormalized;
import kr.inventory.domain.stock.normalization.normalizer.ProductNameNormalizer;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.StockInboundItemRepository;
import kr.inventory.domain.stock.repository.StockInboundRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductNormalizationService {

    private final StockInboundItemRepository stockInboundItemRepository;
    private final StockInboundRepository stockInboundRepository;
    private final StoreAccessValidator storeAccessValidator;
    private final ProductNameNormalizer productNameNormalizer;

    // 입고 전체 상품명 정규화
    @Transactional
    public BulkProductNormalizeResponse normalizeAllForInbound(
            Long userId,
            UUID storePublicId,
            UUID inboundPublicId
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        StockInbound inbound = stockInboundRepository
                .findByInboundPublicIdAndStoreStoreId(inboundPublicId, storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));

        List<StockInboundItem> items = stockInboundItemRepository.findByInboundInboundId(inbound.getInboundId());

        int total = items.size();
        int normalized = 0;
        int skipped = 0;
        int failed = 0;

        for (StockInboundItem item : items) {
            try {
                boolean hasProductFields = item.getProductDisplayName() != null && item.getProductKey() != null;
                if (hasProductFields) {
                    skipped++;
                    continue;
                }

                ProductNameNormalized normalizedName = productNameNormalizer.normalize(item.getRawProductName());
                item.updateProductName(normalizedName.displayName(), normalizedName.productKey());

                normalized++;
            } catch (Exception e) {
                failed++;
            }
        }

        BulkProductNormalizeResult result = new BulkProductNormalizeResult(total, normalized, skipped, failed);
        return BulkProductNormalizeResponse.from(result);
    }
}
