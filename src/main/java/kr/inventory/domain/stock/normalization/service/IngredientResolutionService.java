package kr.inventory.domain.stock.normalization.service;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.IngredientAlias;
import kr.inventory.domain.reference.entity.IngredientMapping;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import kr.inventory.domain.reference.exception.IngredientErrorCode;
import kr.inventory.domain.reference.exception.IngredientException;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.controller.dto.request.IngredientConfirmRequest;
import kr.inventory.domain.stock.controller.dto.response.BulkResolveResponse;
import kr.inventory.domain.stock.controller.dto.response.IngredientConfirmResponse;
import kr.inventory.domain.stock.controller.dto.response.ItemResolveResult;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.entity.enums.ResolutionStatus;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.normalization.constant.InboundItemResolutionConstants;
import kr.inventory.domain.stock.normalization.model.BulkResolveResult;
import kr.inventory.domain.stock.normalization.model.ConfirmResult;
import kr.inventory.domain.stock.normalization.model.InboundSpecExtractor;
import kr.inventory.domain.stock.normalization.model.ResolutionResult;
import kr.inventory.domain.stock.normalization.normalizer.RawProductNameNormalizer;
import kr.inventory.domain.stock.normalization.repository.IngredientAliasRepository;
import kr.inventory.domain.stock.normalization.repository.IngredientMappingRepository;
import kr.inventory.domain.stock.repository.StockInboundItemRepository;
import kr.inventory.domain.stock.repository.StockInboundRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientResolutionService {

    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository ingredientAliasRepository;
    private final IngredientMappingRepository ingredientMappingRepository;
    private final StockInboundItemRepository stockInboundItemRepository;
    private final StockInboundRepository stockInboundRepository;
    private final StoreAccessValidator storeAccessValidator;
    private final RawProductNameNormalizer normalizer;
    private final InboundSpecExtractor inboundSpecExtractor;
    private final InboundQuantityNormalizer inboundQuantityNormalizer;

    @Transactional
    public BulkResolveResponse resolveAllForInbound(Long userId, UUID storePublicId, UUID inboundPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        StockInbound inbound = stockInboundRepository
                .findByInboundPublicIdAndStoreStoreId(inboundPublicId, storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));

        Map<String, String> aliasMap = loadAliasMap();

        List<Ingredient> activeIngredients = ingredientRepository
                .findAllByStoreStoreIdAndStatusNot(storeId, IngredientStatus.DELETED);

        List<StockInboundItem> allItems = stockInboundItemRepository
                .findByInboundInboundId(inbound.getInboundId());

        List<StockInboundItem> targetItems = allItems.stream()
                .filter(item -> {
                    ResolutionStatus status = item.getResolutionStatus();
                    return status == null || status == ResolutionStatus.FAILED;
                })
                .toList();

        int skippedCount = allItems.size() - targetItems.size();

        int autoResolvedCount = 0;
        int failedCount = 0;
        List<ItemResolveResult> itemResults = new ArrayList<>();

        for (StockInboundItem item : targetItems) {
            try {
                validateInboundItemScopeOrThrow(item, storeId, inboundPublicId);
                ResolutionResult result = resolveInternal(storeId, aliasMap, activeIngredients, item);

                switch (result.status()) {
                    case AUTO_SUGGESTED -> autoResolvedCount++;
                    case FAILED -> failedCount++;
                    default -> {
                    }
                }

                itemResults.add(new ItemResolveResult(
                        item.getInboundItemPublicId(),
                        item.getRawProductName(),
                        item.getNormalizedRawKey(),
                        item.getNormalizedRawFull(),
                        item.getResolutionStatus(),
                        item.getIngredient() != null ? item.getIngredient().getIngredientPublicId() : null,
                        item.getIngredient() != null ? item.getIngredient().getName() : null
                ));
            } catch (Exception e) {
                failedCount++;
                itemResults.add(new ItemResolveResult(
                        item.getInboundItemPublicId(),
                        item.getRawProductName(),
                        item.getNormalizedRawKey(),
                        item.getNormalizedRawFull(),
                        ResolutionStatus.FAILED,
                        null,
                        null
                ));
            }
        }

        BulkResolveResult result = new BulkResolveResult(
                targetItems.size(),
                autoResolvedCount,
                failedCount,
                skippedCount,
                itemResults
        );
        return BulkResolveResponse.from(result);
    }

    private ResolutionResult resolveInternal(Long storeId,
                                             Map<String, String> aliasMap,
                                             List<Ingredient> activeIngredients,
                                             StockInboundItem inboundItem) {
        RawProductNameNormalizer.NormalizedResult normalized = normalizer.normalize(inboundItem.getRawProductName());
        String normalizedFull = normalized.normalizedFull() == null ? "" : normalized.normalizedFull();

        List<String> tokens = buildCandidateTokens(normalized, aliasMap);
        if (tokens.isEmpty()) {
            ResolutionResult result = ResolutionResult.failed("", normalizedFull);
            inboundItem.updateNormalizedKeys("", normalizedFull);
            inboundItem.updateResolution(result.status(), null, null);
            return result;
        }

        String canonicalKey = extractCanonicalKey(tokens);
        inboundItem.updateNormalizedKeys(canonicalKey, normalizedFull);

        Optional<IngredientMapping> mapping = ingredientMappingRepository.findActiveStoreLevelMapping(storeId, canonicalKey);
        if (mapping.isPresent()) {
            Ingredient ingredient = mapping.get().getIngredient();
            if (isUsableIngredient(ingredient, storeId) && isUnitCompatible(inboundItem, ingredient)) {
                inboundItem.updateResolution(ResolutionStatus.AUTO_SUGGESTED, ingredient, null);
                applyNormalizedQuantity(inboundItem, ingredient);

                return ResolutionResult.confirmed(canonicalKey, normalizedFull, ingredient);
            }
        }

        if (activeIngredients != null && !activeIngredients.isEmpty()) {
            List<ScoredCandidate> scored = scoreCandidates(tokens, activeIngredients, inboundItem);
            if (!scored.isEmpty()) {
                List<ScoredCandidate> top = scored.stream()
                        .limit(InboundItemResolutionConstants.TOP_N_CANDIDATES)
                        .toList();

                double topScore = top.get(0).score;
                double secondScore = top.size() >= 2 ? top.get(1).score : 0.0;

                boolean confidentAuto = topScore >= InboundItemResolutionConstants.AUTO_SCORE_THRESHOLD
                        && (top.size() == 1 || (topScore - secondScore >= InboundItemResolutionConstants.AUTO_GAP_THRESHOLD));

                if (confidentAuto) {
                    Ingredient chosen = top.get(0).ingredient;
                    inboundItem.updateResolution(ResolutionStatus.AUTO_SUGGESTED, chosen, null);
                    applyNormalizedQuantity(inboundItem, chosen);
                    updateInboundItemKeyToIngredientNormalizedName(inboundItem, chosen, normalizedFull);

                    return ResolutionResult.confirmed(canonicalKey, normalizedFull, chosen);
                }
            }
        }

        inboundItem.updateResolution(ResolutionStatus.AUTO_SUGGESTED, null, null);
        applyNormalizedQuantity(inboundItem, null);
        return ResolutionResult.autoSuggestedWithoutIngredient(canonicalKey, normalizedFull);
    }

    @Transactional
    public IngredientConfirmResponse confirm(Long confirmedByUserId,
                                             UUID storePublicId,
                                             UUID inboundPublicId,
                                             UUID inboundItemPublicId,
                                             IngredientConfirmRequest request) {
        ConfirmResult result = confirmInternal(
                confirmedByUserId,
                storePublicId,
                inboundPublicId,
                inboundItemPublicId,
                request
        );
        return IngredientConfirmResponse.from(result);
    }

    private ConfirmResult confirmInternal(Long confirmedByUserId,
                                          UUID storePublicId,
                                          UUID inboundPublicId,
                                          UUID inboundItemPublicId,
                                          IngredientConfirmRequest request) {
        validateConfirmRequestOrThrow(request);

        Long storeId = storeAccessValidator.validateAndGetStoreId(confirmedByUserId, storePublicId);

        StockInboundItem inboundItem = stockInboundItemRepository
                .findWithInbound(inboundItemPublicId)
                .orElseThrow(() -> new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND));

        validateInboundItemScopeOrThrow(inboundItem, storeId, inboundPublicId);

        String key = resolveNormalizedRawKey(inboundItem);
        InboundSpecExtractor.Spec extractedSpec = inboundSpecExtractor
                .extract(inboundItem.getRawProductName(), inboundItem.getSpecText())
                .orElse(null);

        Ingredient ingredient;

        if (hasExistingIngredient(request)) {
            ingredient = ingredientRepository
                    .findByIngredientPublicIdAndStatusNotWithStore(
                            request.existingIngredientPublicId(),
                            IngredientStatus.DELETED
                    )
                    .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));

            if (!isUsableIngredient(ingredient, storeId)) {
                throw new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND);
            }
            validateUnitCompatibilityOrThrow(extractedSpec, ingredient.getUnit());
        } else {
            Store store = inboundItem.getInbound().getStore();
            IngredientUnit unit = resolveIngredientUnit(request.newIngredientUnit(), extractedSpec);
            validateUnitCompatibilityOrThrow(extractedSpec, unit);
            String ingredientNameToCreate = resolveIngredientNameForCreate(inboundItem, request, key);
            BigDecimal unitSize = resolveIngredientUnitSize(extractedSpec, unit);

            ingredient = unitSize == null
                    ? Ingredient.create(store, ingredientNameToCreate, unit, null)
                    : Ingredient.create(store, ingredientNameToCreate, unit, null, unitSize);

            try {
                ingredient = ingredientRepository.save(ingredient);
            } catch (DataIntegrityViolationException e) {
                throw new IngredientException(IngredientErrorCode.INGREDIENT_ALREADY_EXISTS);
            }
        }

        if (request.specText() != null && !request.specText().isBlank()) {
            inboundItem.updateSpecText(request.specText());
        }

        inboundItem.confirmResolution(ingredient);
        applyNormalizedQuantity(inboundItem, ingredient);

        String confirmedKey = ingredient.getNormalizedName();
        return new ConfirmResult(
                inboundItemPublicId,
                ingredient.getIngredientPublicId(),
                ingredient.getName(),
                confirmedKey != null ? confirmedKey.trim().toLowerCase() : key,
                false
        );
    }

    private String resolveNormalizedRawKey(StockInboundItem inboundItem) {
        String key = inboundItem.getNormalizedRawKey();
        if (key != null && !key.isBlank()) {
            return key.trim();
        }

        Map<String, String> aliasMap = loadAliasMap();
        RawProductNameNormalizer.NormalizedResult normalized = normalizer.normalize(inboundItem.getRawProductName());
        List<String> tokens = buildCandidateTokens(normalized, aliasMap);

        if (tokens.isEmpty()) {
            throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);
        }

        key = extractCanonicalKey(tokens);
        inboundItem.updateNormalizedKeys(
                key,
                normalized.normalizedFull() == null ? "" : normalized.normalizedFull()
        );
        return key;
    }

    private String resolveIngredientNameForCreate(StockInboundItem inboundItem,
                                                  IngredientConfirmRequest request,
                                                  String normalizedRawKey) {
        String requestedName = request.newIngredientName() == null ? "" : request.newIngredientName().trim();
        String rawProductName = inboundItem.getRawProductName() == null ? "" : inboundItem.getRawProductName().trim();
        String normalizedRawFull = inboundItem.getNormalizedRawFull() == null ? "" : inboundItem.getNormalizedRawFull().trim();

        if (requestedName.isBlank()) {
            return normalizedRawKey;
        }

        if (requestedName.equals(rawProductName) || requestedName.equals(normalizedRawFull)) {
            return normalizedRawKey;
        }

        return requestedName;
    }

    private void validateConfirmRequestOrThrow(IngredientConfirmRequest request) {
        if (request == null) {
            throw new StockException(StockErrorCode.INVALID_REQUEST);
        }

        boolean hasExisting = hasExistingIngredient(request);
        boolean hasNewName = hasNewIngredientName(request);
        boolean hasNewUnit = hasText(request.newIngredientUnit());

        if (hasExisting && (hasNewName || hasNewUnit)) {
            throw new StockException(StockErrorCode.INVALID_REQUEST);
        }

        if (!hasExisting && !hasNewName) {
            throw new StockException(StockErrorCode.INVALID_REQUEST);
        }
    }

    private boolean hasExistingIngredient(IngredientConfirmRequest request) {
        return request.existingIngredientPublicId() != null;
    }

    private boolean hasNewIngredientName(IngredientConfirmRequest request) {
        return hasText(request.newIngredientName());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private IngredientUnit resolveIngredientUnit(String unitValue, InboundSpecExtractor.Spec spec) {
        if (unitValue != null && !unitValue.isBlank()) {
            try {
                return IngredientUnit.valueOf(unitValue.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return IngredientUnit.EA;
            }
        }

        if (spec != null) {
            return spec.unit();
        }

        return IngredientUnit.EA;
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

    private void applyNormalizedQuantity(StockInboundItem inboundItem, Ingredient ingredient) {
        InboundQuantityNormalizer.NormalizationResult result = inboundQuantityNormalizer.normalize(inboundItem, ingredient);
        inboundItem.updateNormalizedQuantity(result.normalizedQuantity());
    }


    private boolean isUnitCompatible(StockInboundItem inboundItem, Ingredient ingredient) {
        if (ingredient == null || ingredient.getUnit() == null) {
            return true;
        }

        InboundSpecExtractor.Spec spec = inboundSpecExtractor
                .extract(inboundItem.getRawProductName(), inboundItem.getSpecText())
                .orElse(null);

        if (spec == null) {
            return true;
        }

        return spec.unit() == ingredient.getUnit();
    }

    private void validateUnitCompatibilityOrThrow(InboundSpecExtractor.Spec spec, IngredientUnit unit) {
        if (spec == null || unit == null) {
            return;
        }

        // G/ML 규격이 있는 품목은 EA 재료와 매핑되지 않도록 차단
        if (spec.unit() != unit) {
            throw new StockException(StockErrorCode.INVALID_INBOUND_ITEM_UNIT_MAPPING);
        }
    }

    private void updateInboundItemKeyToIngredientNormalizedName(
            StockInboundItem inboundItem,
            Ingredient ingredient,
            String normalizedRawFull
    ) {
        if (ingredient == null) {
            return;
        }

        String ingredientKey = ingredient.getNormalizedName();
        if (ingredientKey == null || ingredientKey.isBlank()) {
            return;
        }

        inboundItem.updateNormalizedKeys(
                ingredientKey.trim().toLowerCase(),
                normalizedRawFull
        );
    }

    private static class ScoredCandidate {
        final Ingredient ingredient;
        final double score;

        private ScoredCandidate(Ingredient ingredient, double score) {
            this.ingredient = ingredient;
            this.score = score;
        }
    }

    private List<ScoredCandidate> scoreCandidates(List<String> tokens,
                                               List<Ingredient> ingredients,
                                               StockInboundItem inboundItem) {
        Set<String> query = tokens.stream()
                .map(t -> t.trim().toLowerCase())
                .filter(t -> !t.isBlank())
                .collect(Collectors.toSet());

        if (query.isEmpty()) {
            return List.of();
        }

        List<ScoredCandidate> scored = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient == null) continue;
            if (ingredient.getStatus() == IngredientStatus.DELETED) continue;
            if (!isUnitCompatible(inboundItem, ingredient)) continue;

            String normalizedName = ingredient.getNormalizedName();
            if (normalizedName == null || normalizedName.isBlank()) {
                normalizedName = ingredient.getName();
            }
            if (normalizedName == null || normalizedName.isBlank()) continue;

            Set<String> target = Arrays.stream(normalizedName.toLowerCase().split("\\s+"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());

            if (target.isEmpty()) continue;

            double score = jaccard(query, target);
            if (score <= 0.0) continue;

            scored.add(new ScoredCandidate(ingredient, score));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored;
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        int intersection = 0;
        for (String x : a) {
            if (b.contains(x)) intersection++;
        }

        int union = a.size() + b.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / (double) union;
    }

    private String extractCanonicalKey(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }

        for (int i = tokens.size() - 1; i >= 0; i--) {
            String token = tokens.get(i);
            if (token == null || token.isBlank()) {
                continue;
            }

            String lowerToken = token.toLowerCase();

            if (InboundItemResolutionConstants.NUMBER_TOKEN.matcher(lowerToken).matches()) {
                continue;
            }

            if (InboundItemResolutionConstants.COMBINED_QTY_TOKEN.matcher(lowerToken).matches()) {
                continue;
            }

            if (InboundItemResolutionConstants.PROMOTION_PACK_TOKEN.matcher(lowerToken).matches()) {
                continue;
            }

            if (InboundItemResolutionConstants.STORAGE_TOKENS_FOR_KEY.contains(lowerToken)) {
                continue;
            }

            if (InboundItemResolutionConstants.KEY_DESCRIPTION_STOP_WORDS.contains(lowerToken)) {
                continue;
            }

            if (InboundItemResolutionConstants.STOP_WORDS.contains(lowerToken)) {
                continue;
            }

            if (InboundItemResolutionConstants.KEY_USAGE_SUFFIX.matcher(lowerToken).matches()) {
                continue;
            }

            return lowerToken;
        }

        return tokens.get(0).toLowerCase();
    }

    private Map<String, String> loadAliasMap() {
        Map<String, String> map = new HashMap<>();

        List<IngredientAlias> aliases = ingredientAliasRepository.findAll();
        for (IngredientAlias alias : aliases) {
            if (alias == null) continue;
            if (alias.getAlias() == null || alias.getAlias().isBlank()) continue;
            if (alias.getCanonical() == null || alias.getCanonical().isBlank()) continue;

            map.put(
                    alias.getAlias().trim().toLowerCase(),
                    alias.getCanonical().trim().toLowerCase()
            );
        }

        return map;
    }

    private List<String> buildCandidateTokens(RawProductNameNormalizer.NormalizedResult normalized,
                                              Map<String, String> aliasMap) {
        String keyPhrase = normalized.normalizedKey() == null ? "" : normalized.normalizedKey();
        String full = normalized.normalizedFull() == null ? "" : normalized.normalizedFull();

        List<String> tokens = !keyPhrase.isBlank()
                ? normalizer.tokenize(keyPhrase)
                : normalizer.tokenize(full);

        if (tokens.isEmpty()) return List.of();

        List<String> out = new ArrayList<>();
        for (String t : tokens) {
            if (t == null) continue;

            String token = t.trim().toLowerCase();
            if (token.isBlank()) continue;

            if (InboundItemResolutionConstants.NUMBER_TOKEN.matcher(token).matches()) continue;
            if (InboundItemResolutionConstants.COMBINED_QTY_TOKEN.matcher(token).matches()) continue;
            if (InboundItemResolutionConstants.PROMOTION_PACK_TOKEN.matcher(token).matches()) continue;

            if (InboundItemResolutionConstants.STORAGE_TOKENS_FOR_KEY.contains(token)) continue;
            if (InboundItemResolutionConstants.KEY_DESCRIPTION_STOP_WORDS.contains(token)) continue;
            if (InboundItemResolutionConstants.KEY_USAGE_SUFFIX.matcher(token).matches()) continue;
            if (InboundItemResolutionConstants.STOP_WORDS.contains(token)) continue;

            token = aliasMap.getOrDefault(token, token);

            if (token.isBlank()) continue;
            out.add(token);
        }

        LinkedHashSet<String> deduplicated = new LinkedHashSet<>(out);
        return new ArrayList<>(deduplicated);
    }

    public boolean upsertMapping(Store store, Long storeId, String key, Ingredient ingredient) {
        if (key == null || key.isBlank()) return false;

        Optional<IngredientMapping> existingMapping = ingredientMappingRepository
                .findActiveStoreLevelMapping(storeId, key);

        if (existingMapping.isPresent()) {
            existingMapping.get().updateIngredient(ingredient);
            return false;
        }

        IngredientMapping mapping = IngredientMapping.createNormalizedRawMapping(ingredient, store, key);
        ingredientMappingRepository.save(mapping);
        return true;
    }

    private void validateInboundItemScopeOrThrow(StockInboundItem inboundItem, Long storeId, UUID inboundPublicId) {
        StockInbound inbound = inboundItem.getInbound();
        if (inbound == null) throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);

        Store store = inbound.getStore();
        if (store == null) throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);

        if (!storeId.equals(store.getStoreId())) throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);
        if (inbound.getInboundPublicId() == null) throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);
        if (!inbound.getInboundPublicId().equals(inboundPublicId)) {
            throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);
        }
    }

    private boolean isUsableIngredient(Ingredient ingredient, Long storeId) {
        if (ingredient == null) return false;
        if (ingredient.getStatus() != IngredientStatus.ACTIVE) return false;
        if (ingredient.getStore() == null) return false;
        return storeId.equals(ingredient.getStore().getStoreId());
    }
}
