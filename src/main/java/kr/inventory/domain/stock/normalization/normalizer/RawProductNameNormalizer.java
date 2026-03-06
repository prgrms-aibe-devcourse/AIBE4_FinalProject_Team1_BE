package kr.inventory.domain.stock.normalization.normalizer;

import java.util.List;

public interface RawProductNameNormalizer {

    NormalizedResult normalize(String rawProductName);

    List<String> tokenize(String normalized);

    record NormalizedResult(
        String normalizedKey, // 매핑 키 안정화를 위해 수량/단위/기업명 등 메타를 제거한 핵심 토큰
        String normalizedFull // 문자열 정리 - 특수문자 정리/공백 축약/stopword 제거
    ) {}
}
