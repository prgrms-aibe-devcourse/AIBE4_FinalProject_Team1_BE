package kr.inventory.domain.stock.service;

import kr.inventory.domain.analytics.service.StockInboundIndexingService;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.controller.dto.request.ManualInboundRequest;
import kr.inventory.domain.stock.controller.dto.request.StockInboundSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.StockInboundItemResponse;
import kr.inventory.domain.stock.controller.dto.response.StockInboundListResponse;
import kr.inventory.domain.stock.controller.dto.response.StockInboundResponse;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.entity.StockLog;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.stock.entity.enums.ResolutionStatus;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StockInboundItemRepository;
import kr.inventory.domain.stock.repository.StockInboundRepository;
import kr.inventory.domain.stock.repository.StockLogRepository;
import kr.inventory.domain.stock.normalization.model.InboundSpecExtractor;
import kr.inventory.domain.stock.normalization.service.IngredientResolutionService;
import kr.inventory.domain.stock.service.command.StockInboundLogCommand;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;
import kr.inventory.domain.reference.entity.Vendor;
import kr.inventory.domain.reference.repository.VendorRepository;
import kr.inventory.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import kr.inventory.domain.stock.repository.dto.InboundItemAggregate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockInboundService {

    private final StockInboundRepository stockInboundRepository;
    private final StockInboundItemRepository stockInboundItemRepository;
    private final IngredientStockBatchRepository ingredientStockBatchRepository;
    private final StockLogRepository stockLogRepository;
    private final StoreAccessValidator storeAccessValidator;
    private final StoreRepository storeRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientResolutionService ingredientResolutionService;
    private final InboundSpecExtractor inboundSpecExtractor;
    private final StockLogService stockLogService;
    private final StockInboundIndexingService stockInboundIndexingService;


    public StockInboundResponse createManualInbound(Long userId, UUID storePublicId, ManualInboundRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.STORE_NOT_FOUND));

        Vendor vendor = request.vendorPublicId() != null ? vendorRepository.findByVendorPublicId(request.vendorPublicId())
                .orElseThrow(() -> new StockException(StockErrorCode.VENDOR_NOT_FOUND)) : null;

        StockInbound inbound = StockInbound.create(store, vendor, null, null, request.inboundDate());
        stockInboundRepository.save(inbound);

        List<StockInboundItem> items = request.items().stream()
                .map(itemDto -> StockInboundItem.createRaw(
                        inbound,
                        itemDto.rawProductName(),
                        itemDto.quantity(),
                        itemDto.unitCost(),
                        itemDto.expirationDate(),
                        itemDto.specText()
                )).toList();

        stockInboundItemRepository.saveAll(items);

        List<StockInboundItemResponse> itemResponses = items.stream()
                .map(StockInboundItemResponse::from)
                .toList();

        return StockInboundResponse.from(inbound, itemResponses);
    }

    public void confirmInbound(Long userId, UUID storePublicId, UUID inboundPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        StockInbound inbound = stockInboundRepository.findByInboundPublicIdAndStoreStoreId(inboundPublicId, storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));

        if (inbound.getStatus() != InboundStatus.DRAFT) {
            throw new StockException(StockErrorCode.INBOUND_NOT_DRAFT_STATUS);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new StockException(StockErrorCode.USER_NOT_FOUND));

        List<StockInboundItem> items = stockInboundItemRepository.findByInboundInboundId(inbound.getInboundId());

        boolean hasFailedItems = items.stream()
                .anyMatch(item -> item.getResolutionStatus() == ResolutionStatus.FAILED);

        if (hasFailedItems) {
            throw new StockException(StockErrorCode.INBOUND_ITEMS_NOT_RESOLVED);
        }

        inbound.confirm(user);

        Store store = inbound.getStore();

        for (StockInboundItem item : items) {
            if (item.getResolutionStatus() == ResolutionStatus.AUTO_SUGGESTED && item.getIngredient() == null) {
                if (item.getNormalizedRawKey() != null && !item.getNormalizedRawKey().isBlank()) {
                    // rawProductName에서 스펙 추출
                    InboundSpecExtractor.Spec spec = inboundSpecExtractor.extract(item.getRawProductName()).orElse(null);
                    IngredientUnit unit = (spec == null) ? IngredientUnit.G : spec.unit();
                    BigDecimal unitSize = (spec == null) ? null : spec.unitSize();

                    // normalizedRawKey를 이름으로 하는 새로운 Ingredient 생성
                    Ingredient newIngredient = (unitSize == null)
                            ? Ingredient.create(store, item.getNormalizedRawKey(), unit, null)
                            : Ingredient.create(store, item.getNormalizedRawKey(), unit, null, unitSize);

                    ingredientRepository.save(newIngredient);

                    item.updateResolution(ResolutionStatus.AUTO_SUGGESTED, newIngredient, null);
                }
            }
        }

        // CONFIRMED 상태 항목만 IngredientMapping에 학습
        items.stream()
                .filter(item -> item.getResolutionStatus() == ResolutionStatus.CONFIRMED)
                .filter(item -> item.getNormalizedRawKey() != null && item.getIngredient() != null)
                .forEach(item -> ingredientResolutionService.upsertMapping(
                        store,
                        storeId,
                        item.getNormalizedRawKey(),
                        item.getIngredient()
                ));

        for (StockInboundItem item : items) {
            if (item.getIngredient() == null) {
                continue;
            }

            IngredientStockBatch batch = IngredientStockBatch.createFromInbound(
                    item.getIngredient(),
                    item
            );
            ingredientStockBatchRepository.save(batch);

            BigDecimal balanceAfter = ingredientStockBatchRepository.calculateTotalQuantity(
                    storeId,
                    item.getIngredient().getIngredientId()
            );

            StockInboundLogCommand command = StockInboundLogCommand.ofInbound(
                    inbound,
                    item,
                    batch,
                    balanceAfter,
                    user
            );

            stockLogService.logInbound(command);
        }

        try {
            // ES 인덱싱
            stockInboundIndexingService.index(inbound);
        } catch (Exception e) {
            log.error("[ES] 입고 인덱싱 실패 inboundId={}", inbound.getInboundId(), e);
        }
    }

    @Transactional(readOnly = true)
    public StockInboundResponse getInbound(Long userId, UUID storePublicId, UUID inboundPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        StockInbound inbound = stockInboundRepository.findByInboundPublicIdAndStoreStoreId(inboundPublicId, storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));

        List<StockInboundItem> items = stockInboundItemRepository.findByInboundInboundId(inbound.getInboundId());
        List<StockInboundItemResponse> itemResponses = items.stream()
                .map(StockInboundItemResponse::from)
                .toList();

        return StockInboundResponse.from(inbound, itemResponses);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockInboundListResponse> getInbounds(
            Long userId,
            UUID storePublicId,
            StockInboundSearchRequest searchRequest,
            Pageable pageable
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        List<InboundStatus> targetStatuses = List.of(InboundStatus.CONFIRMED, InboundStatus.DRAFT);

        Page<StockInbound> inboundPage = stockInboundRepository
                .searchInbounds(storeId, targetStatuses, searchRequest, pageable);

        List<Long> inboundIds = inboundPage.getContent().stream()
                .map(StockInbound::getInboundId)
                .toList();

        Map<Long, InboundItemAggregate> aggregates = stockInboundItemRepository
                .findAggregatesByInboundIds(inboundIds)
                .stream()
                .collect(Collectors.toMap(InboundItemAggregate::inboundId, a -> a));

        Page<StockInboundListResponse> dtoPage = inboundPage.map(inbound -> {
            InboundItemAggregate aggregate = aggregates.getOrDefault(
                    inbound.getInboundId(),
                    InboundItemAggregate.empty(inbound.getInboundId())
            );

            return StockInboundListResponse.from(
                    inbound,
                    aggregate.itemCount(),
                    aggregate.totalCost()
            );
        });

        return PageResponse.from(dtoPage);
    }
}
