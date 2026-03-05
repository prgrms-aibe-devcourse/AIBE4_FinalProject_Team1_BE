package kr.inventory.domain.stock.normalization.normalizer;

import kr.inventory.domain.stock.normalization.constant.InboundItemResolutionConstants;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;

@Component
public class DefaultRawProductNameNormalizer implements RawProductNameNormalizer {

    @Override
    public NormalizedResult normalize(String rawProductName) {
        if (rawProductName == null || rawProductName.isBlank()) {
            return new NormalizedResult("", "");
        }

        Set<String> keyRemovalTokens = extractKeyRemovalTokens(rawProductName);

        String base = basicNormalize(rawProductName);
        List<String> baseTokens = tokenize(base);

        List<String> fullTokens = baseTokens.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .filter(t -> !InboundItemResolutionConstants.STOP_WORDS.contains(t))
                .toList();

        List<String> canonicalFullTokens = dedup(fullTokens);
        String normalizedFull = String.join(" ", canonicalFullTokens).trim();

        String normalizedKey = buildKey(canonicalFullTokens, keyRemovalTokens);
        if (normalizedKey.isBlank() && !normalizedFull.isBlank()) {
            normalizedKey = buildKeyRelaxed(canonicalFullTokens, keyRemovalTokens);
        }

        return new NormalizedResult(normalizedKey, normalizedFull);
    }

    @Override
    public List<String> tokenize(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .toList();
    }

    private String basicNormalize(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        String step1 = InboundItemResolutionConstants.DELIMS_TO_SPACE.matcher(lower).replaceAll(" ");
        String step2 = InboundItemResolutionConstants.KEEP_ALLOWED.matcher(step1).replaceAll(" ");
        String step3 = step2.trim();
        return InboundItemResolutionConstants.MULTI_SPACE.matcher(step3).replaceAll(" ");
    }

    private Set<String> extractKeyRemovalTokens(String raw) {
        Set<String> removal = new HashSet<>();

        Matcher bm = InboundItemResolutionConstants.BRACKET_CONTENT.matcher(raw);
        while (bm.find()) {
            String content = bm.group(1);
            if (content == null) continue;
            String trimmed = content.trim();
            if (trimmed.isBlank()) continue;

            String normalizedMeta = basicNormalize(trimmed);
            removal.addAll(tokenize(normalizedMeta));
        }

        Matcher pm = InboundItemResolutionConstants.PAREN_CONTENT.matcher(raw);
        while (pm.find()) {
            String content = pm.group(1);
            if (content == null) continue;
            String trimmed = content.trim();
            if (trimmed.isBlank()) continue;

            String normalizedMeta = basicNormalize(trimmed);
            removal.addAll(tokenize(normalizedMeta));
        }

        removal.addAll(InboundItemResolutionConstants.STOP_WORDS);
        removal.addAll(InboundItemResolutionConstants.KEY_DESCRIPTION_STOP_WORDS);

        return removal;
    }

    private List<String> dedup(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token == null || token.isBlank()) continue;
            out.add(token);
        }
        return new ArrayList<>(out);
    }

    private String buildKey(List<String> fullTokens, Set<String> keyRemovalTokens) {
        if (fullTokens == null || fullTokens.isEmpty()) {
            return "";
        }

        List<String> keyTokens = new ArrayList<>();
        for (int i = 0; i < fullTokens.size(); i++) {
            String token = fullTokens.get(i);
            if (token == null || token.isBlank()) continue;

            if (InboundItemResolutionConstants.STORAGE_TOKENS_FOR_KEY.contains(token)) continue;
            if (keyRemovalTokens.contains(token)) continue;
            if (InboundItemResolutionConstants.KEY_USAGE_SUFFIX.matcher(token).matches()) continue;
            if (InboundItemResolutionConstants.PROMOTION_PACK_TOKEN.matcher(token).matches()) continue;
            if (InboundItemResolutionConstants.COMBINED_QTY_TOKEN.matcher(token).matches()) continue;

            if (InboundItemResolutionConstants.NUMBER_TOKEN.matcher(token).matches() && i + 1 < fullTokens.size()) {
                String next = fullTokens.get(i + 1);
                if (next != null && InboundItemResolutionConstants.UNIT_TOKENS.contains(next)) {
                    i++;
                    continue;
                }
            }
            if (InboundItemResolutionConstants.NUMBER_TOKEN.matcher(token).matches()) continue;

            keyTokens.add(token);
        }

        return reduceToCanonical(keyTokens);
    }

    private String buildKeyRelaxed(List<String> fullTokens, Set<String> keyRemovalTokens) {
        if (fullTokens == null || fullTokens.isEmpty()) {
            return "";
        }

        List<String> keyTokens = new ArrayList<>();
        for (String token : fullTokens) {
            if (token == null || token.isBlank()) continue;

            if (InboundItemResolutionConstants.STORAGE_TOKENS_FOR_KEY.contains(token)) continue;
            if (keyRemovalTokens.contains(token)) continue;
            if (InboundItemResolutionConstants.PROMOTION_PACK_TOKEN.matcher(token).matches()) continue;
            if (InboundItemResolutionConstants.KEY_USAGE_SUFFIX.matcher(token).matches()) continue;

            keyTokens.add(token);
        }

        return reduceToCanonical(keyTokens);
    }

    private String reduceToCanonical(List<String> keyTokens) {
        if (keyTokens == null || keyTokens.isEmpty()) {
            return "";
        }

        // 1) tail word 우선
        for (String token : keyTokens) {
            if (InboundItemResolutionConstants.CANONICAL_TAIL_WORDS.contains(token)) {
                return token;
            }
        }

        // 2) 결합 토큰에서 헤드워드 추출 (서울우유 -> 우유, 롯데햄 -> 햄 등)
        for (String headword : InboundItemResolutionConstants.CANONICAL_SUBSTRING_HEADWORDS) {
            for (String token : keyTokens) {
                if (token != null && token.contains(headword)) {
                    return headword;
                }
            }
        }

        // 3) 일반 케이스: 브랜드는 앞에 붙는 경우가 많으므로 마지막 토큰 우선
        return keyTokens.get(keyTokens.size() - 1);
    }
}
