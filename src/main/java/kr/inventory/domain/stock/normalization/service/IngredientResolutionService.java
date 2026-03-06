package kr.inventory.domain.stock.normalization.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.IngredientAlias;
import kr.inventory.domain.reference.entity.IngredientMapping;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import kr.inventory.domain.reference.exception.IngredientErrorCode;
import kr.inventory.domain.reference.exception.IngredientException;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.controller.dto.request.BulkIngredientConfirmRequest;
import kr.inventory.domain.stock.controller.dto.response.*;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.entity.enums.ResolutionStatus;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;

import kr.inventory.domain.stock.normalization.constant.InboundItemResolutionConstants;
import kr.inventory.domain.stock.normalization.model.*;
import kr.inventory.domain.stock.normalization.normalizer.RawProductNameNormalizer;
import kr.inventory.domain.stock.normalization.repository.IngredientAliasRepository;
import kr.inventory.domain.stock.normalization.repository.IngredientMappingRepository;
import kr.inventory.domain.stock.repository.StockInboundItemRepository;
import kr.inventory.domain.stock.repository.StockInboundRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.global.util.IngredientNameNormalizer;
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
    private final InboundSpecExtractor specExtractor;
    private final ObjectMapper objectMapper;

    @Transactional
    public IngredientResolveResponse resolve(Long userId, UUID storePublicId, UUID inboundPublicId, UUID inboundItemPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        StockInboundItem inboundItem = stockInboundItemRepository
                .findWithInbound(inboundItemPublicId)
                .orElseThrow(() -> new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND));

        validateInboundItemScopeOrThrow(inboundItem, storeId, inboundPublicId);

        Map<String, String> aliasMap = loadAliasMapWithDefaults(storeId);

        RawProductNameNormalizer.NormalizedResult normalized = normalizer.normalize(inboundItem.getRawProductName());
        String normalizedFull = normalized.normalizedFull() == null ? "" : normalized.normalizedFull();

        List<String> tokens = buildCandidateTokens(normalized, aliasMap);
        if (tokens.isEmpty()) {
            ResolutionResult result = ResolutionResult.failed("", normalizedFull);
            inboundItem.updateNormalizedKeys("", normalizedFull);
            inboundItem.updateResolution(result.status(), null, null);
            return IngredientResolveResponse.from(result, List.of());
        }

        // canonicalKey: 뒤에서부터 스캔하여 의미 토큰 추출
        String canonicalKey = extractCanonicalKey(tokens);
        inboundItem.updateNormalizedKeys(canonicalKey, normalizedFull);

        // 1) mapping이 있으면 자동확정
        Optional<IngredientMapping> mapping = ingredientMappingRepository
                .findActiveStoreLevelMapping(storeId, canonicalKey);

        if (mapping.isPresent()) {
            Ingredient ingredient = mapping.get().getIngredient();
            if (isUsableIngredient(ingredient, storeId)) {
                ResolutionResult result = ResolutionResult.confirmed(canonicalKey, normalizedFull, ingredient);
                inboundItem.updateResolution(ResolutionStatus.AUTO_RESOLVED, ingredient, null);
                return IngredientResolveResponse.from(result, List.of());
            }
        }

        List<Ingredient> activeIngredients = ingredientRepository
                .findAllByStoreStoreIdAndStatusNot(storeId, IngredientStatus.DELETED);

        // 2) 재료 마스터 0개면 후보를 만들 수 없으니, 후보용 ingredient를 생성하되 PENDING으로 둔다.
        if (activeIngredients.isEmpty()) {
            Ingredient createdCandidate = createBootstrapIngredientCandidate(inboundItem, storeId, canonicalKey);

            List<ResolutionResult.CandidateInfo> candidates = List.of(
                    new ResolutionResult.CandidateInfo(
                            createdCandidate.getIngredientPublicId(),
                            createdCandidate.getName(),
                            createdCandidate.getUnit().name(),
                            1.0
                    )
            );
            JsonNode candidatesJson = objectMapper.valueToTree(candidates);
            ResolutionResult result = ResolutionResult.pending(canonicalKey, normalizedFull, 1.0, candidates);
            inboundItem.updateResolution(ResolutionStatus.PENDING, null, candidatesJson);

            List<IngredientResolveCandidateResponse> candidateResponses = candidates.stream()
                    .map(IngredientResolveCandidateResponse::from)
                    .toList();
            return IngredientResolveResponse.from(result, candidateResponses);
        }

        // 3) 후보 추천(Jaccard)
        List<ScoredCandidate> scored = scoreCandidates(tokens, activeIngredients);
        if (scored.isEmpty()) {
            ResolutionResult result = ResolutionResult.failed(canonicalKey, normalizedFull);
            inboundItem.updateResolution(ResolutionStatus.FAILED, null, null);
            return IngredientResolveResponse.from(result, List.of());
        }

        // top N 변환
        List<ScoredCandidate> top = scored.stream().limit(InboundItemResolutionConstants.TOP_N_CANDIDATES).toList();
        List<ResolutionResult.CandidateInfo> candidateInfos = top.stream()
                .map(s -> new ResolutionResult.CandidateInfo(
                        s.ingredient.getIngredientPublicId(),
                        s.ingredient.getName(),
                        s.ingredient.getUnit().name(),
                        s.score
                ))
                .toList();

        // 4) "충분히 명확한 경우"만 AUTO_RESOLVED
        double topScore = top.get(0).score;
        double secondScore = (top.size() >= 2) ? top.get(1).score : 0.0;
        boolean confidentAuto = topScore >= InboundItemResolutionConstants.AUTO_SCORE_THRESHOLD
                && ((top.size() == 1) || (topScore - secondScore >= InboundItemResolutionConstants.AUTO_GAP_THRESHOLD));

        if (confidentAuto) {
            Ingredient chosen = top.get(0).ingredient;
            inboundItem.updateResolution(ResolutionStatus.AUTO_RESOLVED, chosen, null);
            // 학습(매핑 저장) -> 다음부터는 mapping으로 바로 auto
            upsertMapping(inboundItem.getInbound().getStore(), storeId, canonicalKey, chosen);

            ResolutionResult result = ResolutionResult.confirmed(canonicalKey, normalizedFull, chosen);
            return IngredientResolveResponse.from(result, List.of());
        }

        // 5) 그 외는 PENDING + 후보 제공(사용자가 선택해야 학습)
        JsonNode candidatesJson = objectMapper.valueToTree(candidateInfos);
        ResolutionResult result = ResolutionResult.pending(canonicalKey, normalizedFull, topScore, candidateInfos);
        inboundItem.updateResolution(ResolutionStatus.PENDING, null, candidatesJson);

        List<IngredientResolveCandidateResponse> candidateResponses = candidateInfos.stream()
                .map(IngredientResolveCandidateResponse::from)
                .toList();
        return IngredientResolveResponse.from(result, candidateResponses);
    }

    @Transactional
    public BulkResolveResponse resolveAllForInbound(Long userId, UUID storePublicId, UUID inboundPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        StockInbound inbound = stockInboundRepository
                .findByInboundPublicIdAndStoreStoreId(inboundPublicId, storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));

        Map<String, String> aliasMap = loadAliasMapWithDefaults(storeId);

        List<Ingredient> activeIngredients = ingredientRepository
                .findAllByStoreStoreIdAndStatusNot(storeId, IngredientStatus.DELETED);

        List<StockInboundItem> allItems = stockInboundItemRepository
                .findByInboundInboundId(inbound.getInboundId());

        List<StockInboundItem> targetItems = allItems.stream()
                .filter(item -> {
                    ResolutionStatus status = item.getResolutionStatus();
                    return status == null || status == ResolutionStatus.PENDING || status == ResolutionStatus.FAILED;
                })
                .toList();

        int skippedCount = allItems.size() - targetItems.size();

        int autoResolvedCount = 0;
        int pendingCount = 0;
        int failedCount = 0;

        for (StockInboundItem item : targetItems) {
            try {
                validateInboundItemScopeOrThrow(item, storeId, inboundPublicId);
                ResolutionResult r = resolveInternal(storeId, aliasMap, activeIngredients, item);
                switch (r.status()) {
                    case AUTO_RESOLVED -> autoResolvedCount++;
                    case PENDING -> pendingCount++;
                    case FAILED -> failedCount++;
                    default -> {}
                }
            } catch (Exception e) {
                failedCount++;
            }
        }

        BulkResolveResult result = new BulkResolveResult(targetItems.size(), autoResolvedCount, pendingCount, failedCount, skippedCount);
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
            if (isUsableIngredient(ingredient, storeId)) {
                inboundItem.updateResolution(ResolutionStatus.AUTO_RESOLVED, ingredient, null);
                return ResolutionResult.confirmed(canonicalKey, normalizedFull, ingredient);
            }
        }

        if (activeIngredients == null || activeIngredients.isEmpty()) {
            Ingredient createdCandidate = createBootstrapIngredientCandidate(inboundItem, storeId, canonicalKey);
            List<ResolutionResult.CandidateInfo> candidates = List.of(
                    new ResolutionResult.CandidateInfo(
                            createdCandidate.getIngredientPublicId(),
                            createdCandidate.getName(),
                            createdCandidate.getUnit().name(),
                            1.0
                    )
            );
            JsonNode candidatesJson = objectMapper.valueToTree(candidates);
            inboundItem.updateResolution(ResolutionStatus.PENDING, null, candidatesJson);
            return ResolutionResult.pending(canonicalKey, normalizedFull, 1.0, candidates);
        }

        List<ScoredCandidate> scored = scoreCandidates(tokens, activeIngredients);
        if (scored.isEmpty()) {
            inboundItem.updateResolution(ResolutionStatus.FAILED, null, null);
            return ResolutionResult.failed(canonicalKey, normalizedFull);
        }

        List<ScoredCandidate> top = scored.stream().limit(InboundItemResolutionConstants.TOP_N_CANDIDATES).toList();
        List<ResolutionResult.CandidateInfo> infos = top.stream()
                .map(s -> new ResolutionResult.CandidateInfo(
                        s.ingredient.getIngredientPublicId(),
                        s.ingredient.getName(),
                        s.ingredient.getUnit().name(),
                        s.score
                ))
                .toList();

        double topScore = top.get(0).score;
        double secondScore = (top.size() >= 2) ? top.get(1).score : 0.0;
        boolean confidentAuto = topScore >= InboundItemResolutionConstants.AUTO_SCORE_THRESHOLD
                && ((top.size() == 1) || (topScore - secondScore >= InboundItemResolutionConstants.AUTO_GAP_THRESHOLD));

        if (confidentAuto) {
            Ingredient chosen = top.get(0).ingredient;
            inboundItem.updateResolution(ResolutionStatus.AUTO_RESOLVED, chosen, null);
            upsertMapping(inboundItem.getInbound().getStore(), storeId, canonicalKey, chosen);
            return ResolutionResult.confirmed(canonicalKey, normalizedFull, chosen);
        }

        JsonNode candidatesJson = objectMapper.valueToTree(infos);
        inboundItem.updateResolution(ResolutionStatus.PENDING, null, candidatesJson);
        return ResolutionResult.pending(canonicalKey, normalizedFull, topScore, infos);
    }

    @Transactional
    public IngredientConfirmResponse confirm(Long confirmedByUserId,
                                 UUID storePublicId,
                                 UUID inboundPublicId,
                                 UUID inboundItemPublicId,
                                 UUID chosenIngredientPublicId) {
        ConfirmResult result = confirmInternal(
                confirmedByUserId,
                storePublicId,
                inboundPublicId,
                inboundItemPublicId,
                chosenIngredientPublicId
        );
        return IngredientConfirmResponse.from(result);
    }

    private ConfirmResult confirmInternal(Long confirmedByUserId,
                                          UUID storePublicId,
                                          UUID inboundPublicId,
                                          UUID inboundItemPublicId,
                                          UUID chosenIngredientPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(confirmedByUserId, storePublicId);

        StockInboundItem inboundItem = stockInboundItemRepository
                .findWithInbound(inboundItemPublicId)
                .orElseThrow(() -> new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND));

        validateInboundItemScopeOrThrow(inboundItem, storeId, inboundPublicId);

        Ingredient ingredient = ingredientRepository
                .findByIngredientPublicIdAndStatusNotWithStore(chosenIngredientPublicId, IngredientStatus.DELETED)
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));

        if (!isUsableIngredient(ingredient, storeId)) {
            throw new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND);
        }

        String key = inboundItem.getNormalizedRawKey();
        if (key == null || key.isBlank()) {
            Map<String, String> aliasMap = loadAliasMapWithDefaults(storeId);
            RawProductNameNormalizer.NormalizedResult normalized = normalizer.normalize(inboundItem.getRawProductName());
            List<String> tokens = buildCandidateTokens(normalized, aliasMap);
            if (tokens.isEmpty()) {
                throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);
            }
            key = tokens.get(tokens.size() - 1);
            inboundItem.updateNormalizedKeys(key, normalized.normalizedFull() == null ? "" : normalized.normalizedFull());
        }

        inboundItem.confirmResolution(ingredient);

        Store store = inboundItem.getInbound().getStore();
        boolean newMappingCreated = upsertMapping(store, storeId, key, ingredient);

        return new ConfirmResult(
                inboundItemPublicId,
                ingredient.getIngredientPublicId(),
                ingredient.getName(),
                key,
                newMappingCreated
        );
    }

    @Transactional
    public BulkIngredientConfirmResponse confirmAllForInbound(Long confirmedByUserId,
                                                  UUID storePublicId,
                                                  UUID inboundPublicId,
                                                  BulkIngredientConfirmRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(confirmedByUserId, storePublicId);

        StockInbound inbound = stockInboundRepository
                .findByInboundPublicIdAndStoreStoreId(inboundPublicId, storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));

        BulkIngredientConfirmCommand command = BulkIngredientConfirmCommand.from(request);

        List<BulkConfirmItemResultDetail> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (BulkIngredientConfirmItemCommand item : command.items()) {
            UUID inboundItemPublicId = item.inboundItemPublicId();
            UUID chosenIngredientPublicId = item.chosenIngredientPublicId();

            try {
                ConfirmResult confirmResult = confirmInternal(
                        confirmedByUserId,
                        storePublicId,
                        inboundPublicId,
                        inboundItemPublicId,
                        chosenIngredientPublicId
                );
                results.add(BulkConfirmItemResultDetail.success(inboundItemPublicId, confirmResult));
                successCount++;
            } catch (Exception e) {
                results.add(BulkConfirmItemResultDetail.failure(inboundItemPublicId, e.getMessage()));
                failedCount++;
            }
        }

        List<BulkIngredientConfirmItemResultResponse> responseResults = results.stream()
                .map(r -> r.success()
                        ? BulkIngredientConfirmItemResultResponse.success(r.inboundItemPublicId(), r.confirmResult())
                        : BulkIngredientConfirmItemResultResponse.failure(r.inboundItemPublicId(), r.errorMessage()))
                .toList();

        return BulkIngredientConfirmResponse.from(
                responseResults.size(),
                successCount,
                failedCount,
                responseResults
        );
    }

    // ------------------ 후보 스코어링 ------------------

    private static class ScoredCandidate {
        final Ingredient ingredient;
        final double score;

        private ScoredCandidate(Ingredient ingredient, double score) {
            this.ingredient = ingredient;
            this.score = score;
        }
    }

    private List<ScoredCandidate> scoreCandidates(List<String> tokens, List<Ingredient> ingredients) {
        Set<String> query = tokens.stream()
                .map(t -> t.trim().toLowerCase())
                .filter(t -> !t.isBlank())
                .collect(Collectors.toSet());

        if (query.isEmpty()) {
            return List.of();
        }

        List<ScoredCandidate> scored = new ArrayList<>();
        for (Ingredient ing : ingredients) {
            if (ing == null) continue;
            if (ing.getStatus() == IngredientStatus.DELETED) continue;

            String nn = ing.getNormalizedName();
            if (nn == null || nn.isBlank()) nn = ing.getName();
            if (nn == null || nn.isBlank()) continue;

            Set<String> target = Arrays.stream(nn.toLowerCase().split("\\s+"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());

            if (target.isEmpty()) continue;

            double j = jaccard(query, target);
            if (j <= 0.0) continue;

            scored.add(new ScoredCandidate(ing, j));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored;
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        int inter = 0;
        for (String x : a) {
            if (b.contains(x)) inter++;
        }
        int union = a.size() + b.size() - inter;
        return union == 0 ? 0.0 : (double) inter / (double) union;
    }

    // ------------------ 토큰/alias ------------------

    /**
     * 토큰 리스트에서 canonical key 추출
     * 뒤에서부터 스캔하여 숫자/단위/불용어가 아닌 첫 번째 의미 토큰을 반환
     */
    private String extractCanonicalKey(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }

        // 뒤에서부터 스캔
        for (int i = tokens.size() - 1; i >= 0; i--) {
            String token = tokens.get(i);
            if (token == null || token.isBlank()) {
                continue;
            }

            String lowerToken = token.toLowerCase();

            // 숫자 토큰 건너뛰기
            if (InboundItemResolutionConstants.NUMBER_TOKEN.matcher(lowerToken).matches()) {
                continue;
            }

            // 숫자+단위 결합 토큰 건너뛰기 (예: 1l, 454g, 2.5kg)
            if (InboundItemResolutionConstants.COMBINED_QTY_TOKEN.matcher(lowerToken).matches()) {
                continue;
            }

            // 프로모션 팩 토큰 건너뛰기 (예: 1x2, 10개입)
            if (InboundItemResolutionConstants.PROMOTION_PACK_TOKEN.matcher(lowerToken).matches()) {
                continue;
            }

            // 보관 관련 토큰 건너뛰기 (냉장, 냉동 등)
            if (InboundItemResolutionConstants.STORAGE_TOKENS_FOR_KEY.contains(lowerToken)) {
                continue;
            }

            // 설명 불용어 건너뛰기 (두꺼운, 얇은 등)
            if (InboundItemResolutionConstants.KEY_DESCRIPTION_STOP_WORDS.contains(lowerToken)) {
                continue;
            }

            // 일반 불용어 건너뛰기
            if (InboundItemResolutionConstants.STOP_WORDS.contains(lowerToken)) {
                continue;
            }

            // "~용" 패턴 건너뛰기 (샐러드용, 토스트용 등)
            if (InboundItemResolutionConstants.KEY_USAGE_SUFFIX.matcher(lowerToken).matches()) {
                continue;
            }

            // 의미있는 토큰 발견
            return lowerToken;
        }

        // 모든 토큰이 필터링된 경우, 첫 번째 토큰 반환
        return tokens.get(0).toLowerCase();
    }

    private Map<String, String> loadAliasMapWithDefaults(Long storeId) {
        Map<String, String> map = new HashMap<>();

        // 기본 내장 alias(업종 무관, 극히 안전한 것만)
        map.put("특란", "계란");
        map.put("대란", "계란");
        map.put("중란", "계란");
        map.put("소란", "계란");
        map.put("유정란", "계란");
        map.put("무정란", "계란");

        // "~우유" 결합 토큰은 우유로 수렴(서울우유/연세우유/매일우유 등)
        // (이건 buildCandidateTokens에서 heuristic으로 처리)

        // DB alias가 있으면 override
        List<IngredientAlias> aliases = ingredientAliasRepository.findAllByStoreStoreId(storeId);

        for (IngredientAlias a : aliases) {
            map.put(a.getAlias().trim().toLowerCase(), a.getCanonical().trim().toLowerCase());
        }

        return map;
    }

    private List<String> buildCandidateTokens(RawProductNameNormalizer.NormalizedResult normalized, Map<String, String> aliasMap) {
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

            // 숫자/단위 제거
            if (InboundItemResolutionConstants.NUMBER_TOKEN.matcher(token).matches()) continue;
            if (InboundItemResolutionConstants.COMBINED_QTY_TOKEN.matcher(token).matches()) continue;
            if (InboundItemResolutionConstants.PROMOTION_PACK_TOKEN.matcher(token).matches()) continue;

            // 보관/설명/stopwords 제거
            if (InboundItemResolutionConstants.STORAGE_TOKENS_FOR_KEY.contains(token)) continue;
            if (InboundItemResolutionConstants.KEY_DESCRIPTION_STOP_WORDS.contains(token)) continue;
            if (InboundItemResolutionConstants.KEY_USAGE_SUFFIX.matcher(token).matches()) continue;
            if (InboundItemResolutionConstants.STOP_WORDS.contains(token)) continue;

            // heuristic: "~우유" 결합 토큰 -> "우유"
            if (token.endsWith("우유") && token.length() > 2) {
                token = "우유";
            }

            // alias 적용(기본 + DB)
            token = aliasMap.getOrDefault(token, token);

            if (token.isBlank()) continue;
            out.add(token);
        }

        LinkedHashSet<String> set = new LinkedHashSet<>(out);
        return new ArrayList<>(set);
    }

    // ------------------ 부트스트랩 생성 ------------------

    private Ingredient createBootstrapIngredientCandidate(StockInboundItem inboundItem, Long storeId, String canonicalKey) {
        Store store = inboundItem.getInbound().getStore();

        String name = canonicalKey.trim();
        if (name.length() > 120) name = name.substring(0, 120);

        String normalizedName = IngredientNameNormalizer.normalizeForSearch(name);

        Optional<Ingredient> existing = ingredientRepository
                .findByStoreStoreIdAndNormalizedNameAndStatusNot(storeId, normalizedName, IngredientStatus.DELETED);

        if (existing.isPresent()) return existing.get();

        InboundSpecExtractor.Spec spec = specExtractor.extract(inboundItem.getRawProductName()).orElse(null);
        IngredientUnit unit = (spec == null) ? IngredientUnit.EA : spec.unit();
        BigDecimal unitSize = (spec == null) ? null : spec.unitSize();

        Ingredient created = (unitSize == null)
                ? Ingredient.create(store, name, unit, null)
                : Ingredient.create(store, name, unit, null, unitSize);

        try {
            return ingredientRepository.save(created);
        } catch (DataIntegrityViolationException e) {
            return ingredientRepository
                    .findByStoreStoreIdAndNormalizedNameAndStatusNot(storeId, normalizedName, IngredientStatus.DELETED)
                    .orElseThrow(() -> e);
        }
    }

    // ------------------ mapping ------------------

    private boolean upsertMapping(Store store, Long storeId, String key, Ingredient ingredient) {
        if (key == null || key.isBlank()) return false;

        try {
            IngredientMapping mapping = IngredientMapping.createNormalizedRawMapping(ingredient, store, key);
            ingredientMappingRepository.saveAndFlush(mapping);
            return true;
        } catch (DataIntegrityViolationException e) {
            Optional<IngredientMapping> existingMapping = ingredientMappingRepository
                    .findActiveStoreLevelMapping(storeId, key);

            if (existingMapping.isPresent()) {
                existingMapping.get().updateIngredient(ingredient);
                return false;
            }
            throw e;
        }
    }

    // ------------------ scope / utils ------------------

    private void validateInboundItemScopeOrThrow(StockInboundItem inboundItem, Long storeId, UUID inboundPublicId) {
        StockInbound inbound = inboundItem.getInbound();
        if (inbound == null) throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);

        Store store = inbound.getStore();
        if (store == null) throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);

        if (!storeId.equals(store.getStoreId())) throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);
        if (inbound.getInboundPublicId() == null) throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);
        if (!inbound.getInboundPublicId().equals(inboundPublicId)) throw new StockException(StockErrorCode.INBOUND_ITEM_NOT_FOUND);
    }

    private boolean isUsableIngredient(Ingredient ingredient, Long storeId) {
        if (ingredient == null) return false;
        if (ingredient.getStatus() != IngredientStatus.ACTIVE) return false;
        if (ingredient.getStore() == null) return false;
        return storeId.equals(ingredient.getStore().getStoreId());
    }
}