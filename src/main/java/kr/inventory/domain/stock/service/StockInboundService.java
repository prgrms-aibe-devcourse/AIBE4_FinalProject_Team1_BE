package kr.inventory.domain.stock.service;

import kr.inventory.domain.analytics.service.indexing.StockInboundIndexingService;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.Vendor;
import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.reference.repository.VendorRepository;
import kr.inventory.domain.stock.controller.dto.request.ManualInboundRequest;
import kr.inventory.domain.stock.controller.dto.request.StockInboundSearchRequest;
import kr.inventory.domain.stock.controller.dto.request.UpdateNormalizationRequest;
import kr.inventory.domain.stock.controller.dto.response.StockInboundItemResponse;
import kr.inventory.domain.stock.controller.dto.response.StockInboundListResponse;
import kr.inventory.domain.stock.controller.dto.response.StockInboundResponse;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.stock.entity.enums.ResolutionStatus;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.normalization.model.InboundSpecExtractor;
import kr.inventory.domain.stock.normalization.service.InboundQuantityNormalizer;
import kr.inventory.domain.stock.normalization.service.IngredientResolutionService;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StockInboundItemRepository;
import kr.inventory.domain.stock.repository.StockInboundRepository;
import kr.inventory.domain.stock.repository.dto.InboundItemAggregate;
import kr.inventory.domain.stock.service.command.StockInboundLogCommand;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;
import kr.inventory.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockInboundService {

    private final StockInboundRepository stockInboundRepository;
    private final StockInboundItemRepository stockInboundItemRepository;
    private final IngredientStockBatchRepository ingredientStockBatchRepository;
    private final StoreAccessValidator storeAccessValidator;
    private final StoreRepository storeRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientResolutionService ingredientResolutionService;
    private final InboundSpecExtractor inboundSpecExtractor;
    private final InboundQuantityNormalizer inboundQuantityNormalizer;
    private final StockLogService stockLogService;
    private final StockInboundIndexingService stockInboundIndexingService;
    private final ShortageResolutionService shortageResolutionService;

    public StockInboundResponse createManualInbound(Long userId, UUID storePublicId, ManualInboundRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.STORE_NOT_FOUND));

        Vendor vendor = request.vendorPublicId() != null
                ? vendorRepository.findByVendorPublicId(request.vendorPublicId())
                .orElseThrow(() -> new StockException(StockErrorCode.VENDOR_NOT_FOUND))
                : null;

        StockInbound inbound = StockInbound.create(store, vendor, null, null, request.inboundDate());
        stockInboundRepository.save(inbound);

        List<StockInboundItem> items = request.items().stream()
                .map(itemDto -> {
                    StockInboundItem item = StockInboundItem.createRaw(
                            inbound,
                            itemDto.rawProductName(),
                            itemDto.quantity(),
                            itemDto.unitCost(),
                            itemDto.expirationDate(),
                            itemDto.specText()
                    );
                    applyNormalizedQuantity(item, null);
                    return item;
                })
                .toList();

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

        // AUTO_SUGGESTED 상태이면서 ingredient가 null인 경우 자동 재료 생성 및 매핑 저장
        for (StockInboundItem item : items) {
            if (item.getResolutionStatus() == ResolutionStatus.AUTO_SUGGESTED && item.getIngredient() == null) {
                if (item.getNormalizedRawKey() != null && !item.getNormalizedRawKey().isBlank()) {
                    String originalKey = item.getNormalizedRawKey();
                    Ingredient newIngredient = createAutoSuggestedIngredient(store, item);
                    ingredientRepository.save(newIngredient);
                    item.updateResolution(ResolutionStatus.AUTO_SUGGESTED, newIngredient, null);

                    // 원본 정규화 키로 매핑 저장
                    ingredientResolutionService.upsertMapping(store, storeId, originalKey, newIngredient);

                    // 매핑 저장 후 normalizedRawKey를 재료의 정규화된 이름으로 업데이트
                    String ingredientKey = newIngredient.getNormalizedName();
                    if (ingredientKey != null && !ingredientKey.isBlank()) {
                        item.updateNormalizedKeys(ingredientKey.trim().toLowerCase(), item.getNormalizedRawFull());
                    }
                }
            }
        }

        // CONFIRMED 상태 아이템의 매핑 저장 및 키 업데이트
        items.stream()
                .filter(item -> item.getResolutionStatus() == ResolutionStatus.CONFIRMED)
                .filter(item -> item.getNormalizedRawKey() != null && item.getIngredient() != null)
                .forEach(item -> {
                    // 원본 정규화 키로 매핑 저장
                    ingredientResolutionService.upsertMapping(
                            store,
                            storeId,
                            item.getNormalizedRawKey(),
                            item.getIngredient()
                    );

                    // 매핑 저장 후 normalizedRawKey를 재료의 정규화된 이름으로 업데이트
                    String ingredientKey = item.getIngredient().getNormalizedName();
                    if (ingredientKey != null && !ingredientKey.isBlank()) {
                        item.updateNormalizedKeys(
                                ingredientKey.trim().toLowerCase(),
                                item.getNormalizedRawFull()
                        );
                    }
                });

        Set<Long> affectedIngredientIds = new HashSet<>();

        for (StockInboundItem item : items) {
            if (item.getIngredient() == null) {
                continue;
            }

            applyNormalizedQuantity(item, item.getIngredient());
            validateFinalUnitCompatibilityOrThrow(item, item.getIngredient());

            IngredientStockBatch batch = IngredientStockBatch.createFromInbound(
                    item.getIngredient(),
                    item
            );
            ingredientStockBatchRepository.save(batch);

            affectedIngredientIds.add(item.getIngredient().getIngredientId());

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

        shortageResolutionService.closePendingShortagesIfStockRecovered(storeId, affectedIngredientIds);

        try {
            stockInboundIndexingService.index(inbound);
        } catch (Exception e) {
            log.error("[ES] 입고 인덱싱 실패 inboundId={}", inbound.getInboundId(), e);
        }
    }

    @Transactional
    public void updateItemNormalization(Long userId,
                                        UUID storePublicId,
                                        UUID inboundPublicId,
                                        UUID inboundItemPublicId,
                                        UpdateNormalizationRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        StockInboundItem inboundItem = stockInboundItemRepository
                .findWithInbound(inboundItemPublicId)
                .orElseThrow(() -> new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND));

        if (!inboundItem.getInbound().getInboundPublicId().equals(inboundPublicId)) {
            throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);
        }

        if (!inboundItem.getInbound().getStore().getStoreId().equals(storeId)) {
            throw new StockException(StockErrorCode.INBOUND_NOT_FOUND);
        }

        if (request.specText() != null && !request.specText().isBlank()) {
            inboundItem.updateSpecText(request.specText());
        }

        // normalizedUnit과 normalizedUnitSize가 제공되면 정규화 수량 재계산
        if (request.normalizedUnit() != null) {
            IngredientUnit unit = parseIngredientUnit(request.normalizedUnit());
            BigDecimal unitSize = request.normalizedUnitSize();

            if (unitSize != null && unitSize.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal normalizedQuantity = inboundItem.getQuantity().multiply(unitSize);
                inboundItem.updateNormalizedQuantity(normalizedQuantity);
            } else {
                // unitSize가 없으면 원본 수량 사용
                inboundItem.updateNormalizedQuantity(inboundItem.getQuantity());
            }
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

        Page<StockInboundListResponse> dtoPage = inboundPage.map(inboundEntity -> {
            InboundItemAggregate aggregate = aggregates.getOrDefault(
                    inboundEntity.getInboundId(),
                    InboundItemAggregate.empty(inboundEntity.getInboundId())
            );

            return StockInboundListResponse.from(
                    inboundEntity,
                    aggregate.itemCount(),
                    aggregate.totalCost()
            );
        });

        return PageResponse.from(dtoPage);
    }

    private Ingredient createAutoSuggestedIngredient(Store store, StockInboundItem item) {
        InboundSpecExtractor.Spec spec = inboundSpecExtractor
                .extract(item.getRawProductName(), item.getSpecText())
                .orElse(null);

        IngredientUnit unit = spec != null ? spec.unit() : IngredientUnit.EA;
        BigDecimal unitSize = resolveIngredientUnitSize(spec, unit);

        if (unitSize == null) {
            return Ingredient.create(store, item.getNormalizedRawKey(), unit, null);
        }

        return Ingredient.create(store, item.getNormalizedRawKey(), unit, null, unitSize);
    }

    private BigDecimal resolveIngredientUnitSize(InboundSpecExtractor.Spec spec, IngredientUnit unit) {
        if (spec == null || unit == null) {
            return null;
        }

        if (unit == IngredientUnit.EA) {
            return null;
        }

        if (spec.unit() != unit) {
            return null;
        }

        if (spec.unitSize() == null || spec.unitSize().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return spec.unitSize();
    }

    private void applyNormalizedQuantity(StockInboundItem item, Ingredient ingredient) {
        InboundQuantityNormalizer.NormalizationResult result = inboundQuantityNormalizer.normalize(item, ingredient);
        item.updateNormalizedQuantity(result.normalizedQuantity());
    }

    private void validateFinalUnitCompatibilityOrThrow(StockInboundItem item, Ingredient ingredient) {
        InboundSpecExtractor.Spec spec = inboundSpecExtractor
                .extract(item.getRawProductName(), item.getSpecText())
                .orElse(null);

        if (spec == null || ingredient == null || ingredient.getUnit() == null) {
            return;
        }

        // G/ML 규격이 있는 품목은 EA 재료와 매핑되지 않도록 차단
        if (spec.unit() != ingredient.getUnit()) {
            throw new StockException(StockErrorCode.INVALID_INBOUND_ITEM_UNIT_MAPPING);
        }
    }

    private IngredientUnit parseIngredientUnit(String unitStr) {
        if (unitStr == null || unitStr.isBlank()) {
            return IngredientUnit.EA;
        }

        return switch (unitStr.toUpperCase()) {
            case "EA" -> IngredientUnit.EA;
            case "G" -> IngredientUnit.G;
            case "ML" -> IngredientUnit.ML;
            default -> IngredientUnit.EA;
        };
    }
}
