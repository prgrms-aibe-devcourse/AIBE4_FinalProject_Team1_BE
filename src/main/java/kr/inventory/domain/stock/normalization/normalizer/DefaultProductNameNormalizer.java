package kr.inventory.domain.stock.normalization.normalizer;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class DefaultProductNameNormalizer implements ProductNameNormalizer {

    // 법인/회사 표기 패턴
    private static final Pattern COMPANY_PATTERN = Pattern.compile(
            "\\b(주식회사|유한회사|\\(주\\)|\\(유\\)|co\\.?|ltd\\.?|inc\\.?|corp\\.?)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // 보관상태 패턴
    private static final Pattern STORAGE_PATTERN = Pattern.compile(
            "\\b(냉장|냉동|실온|상온)\\b"
    );

    // 숫자+단위 패턴 (소수 포함)
    // 예: 3.2kg, 1.8L, 450g, 12입
    private static final Pattern QUANTITY_UNIT_PATTERN = Pattern.compile(
            "\\b\\d+(?:[.,]\\d+)?\\s*(kg|g|l|ml|ea|개|구|팩|box|봉|병|캔|입|호|번)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // 소수점이 keepAllowedChars 단계에서 공백으로 분리되는 케이스 제거용
    // 예: "3.2kg" -> "3 2kg" 또는 "1.8L" -> "1 8l"
    // 이런 형태는 QUANTITY_UNIT_PATTERN만 적용하면 앞 숫자(3,1)가 남는다.
    private static final Pattern SPLIT_DECIMAL_UNIT_PATTERN = Pattern.compile(
            "\\b(\\d+)\\s+(\\d+)\\s*(kg|g|l|ml|ea|개|구|팩|box|봉|병|캔|입|호|번)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final boolean removeStorageInfo;

    public DefaultProductNameNormalizer() {
        this(true); // 기본값: 보관상태 제거
    }

    public DefaultProductNameNormalizer(boolean removeStorageInfo) {
        this.removeStorageInfo = removeStorageInfo;
    }

    @Override
    public ProductNameNormalized normalize(String rawProductName) {
        if (rawProductName == null || rawProductName.isBlank()) {
            return new ProductNameNormalized("", "");
        }

        String normalized = rawProductName;

        // 1) 소문자화 (영문만)
        normalized = toLowerCase(normalized);

        // 2) 구분자 제거/치환 (괄호, 대괄호, 슬래시, 콤마 등 -> 공백)
        normalized = replaceDelimiters(normalized);

        // 3) 허용 문자만 유지 (한글, 영문, 숫자, 공백)
        normalized = keepAllowedChars(normalized);

        // 4) 법인/회사 표기 제거
        normalized = removeCompanyNames(normalized);

        // 5) 보관상태 제거 (옵션)
        if (removeStorageInfo) {
            normalized = removeStorageInfo(normalized);
        }

        // 6) 수량/단위 토큰 제거
        // 6-1) 소수점 분리 케이스(예: "3 2kg") 먼저 제거
        normalized = removeSplitDecimalQuantityUnits(normalized);
        // 6-2) 일반 케이스(예: "3.2kg", "450g", "12입") 제거
        normalized = removeQuantityUnits(normalized);

        // 7) 공백 축약
        normalized = normalizeWhitespace(normalized);

        String displayName = normalized.trim();
        String productKey = displayName; // MVP: displayName과 동일

        return new ProductNameNormalized(displayName, productKey);
    }

    private String toLowerCase(String text) {
        // 영문만 소문자화 (한글은 유지)
        StringBuilder result = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (ch >= 'A' && ch <= 'Z') {
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private String replaceDelimiters(String text) {
        // 괄호, 대괄호, 슬래시, 콤마 등을 공백으로 치환
        return text.replaceAll("[()\\[\\]/,;:·•\\-_]", " ");
    }

    private String keepAllowedChars(String text) {
        // 한글, 영문, 숫자, 공백만 유지
        // (소수점 '.' 은 제거되어 공백이 되므로 "3.2kg" -> "3 2kg" 형태가 될 수 있음)
        return text.replaceAll("[^가-힣a-z0-9\\s]", " ");
    }

    private String removeCompanyNames(String text) {
        return COMPANY_PATTERN.matcher(text).replaceAll(" ");
    }

    private String removeStorageInfo(String text) {
        return STORAGE_PATTERN.matcher(text).replaceAll(" ");
    }

    private String removeSplitDecimalQuantityUnits(String text) {
        // 예: "마요네즈 3 2kg" -> "마요네즈"
        return SPLIT_DECIMAL_UNIT_PATTERN.matcher(text).replaceAll(" ");
    }

    private String removeQuantityUnits(String text) {
        // 예: "마요네즈 3.2kg" / "버터 450g" / "식빵 12입"
        return QUANTITY_UNIT_PATTERN.matcher(text).replaceAll(" ");
    }

    private String normalizeWhitespace(String text) {
        // 연속된 공백을 하나로 축약
        return text.replaceAll("\\s+", " ");
    }
}
