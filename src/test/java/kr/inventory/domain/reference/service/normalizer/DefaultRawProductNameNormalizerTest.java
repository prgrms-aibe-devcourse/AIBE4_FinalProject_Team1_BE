package kr.inventory.domain.reference.service.normalizer;

import kr.inventory.domain.stock.normalization.normalizer.DefaultRawProductNameNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DefaultRawProductNameNormalizer 테스트")
class DefaultRawProductNameNormalizerTest {

    private final DefaultRawProductNameNormalizer normalizer = new DefaultRawProductNameNormalizer();

    @Test
    @DisplayName("영문 상품명 정규화 - 소문자 변환 및 특수문자 제거")
    void normalizeEnglishProductName() {
        String raw = "Coca-Cola (500ml)";
        var result = normalizer.normalize(raw);

        assertThat(result.normalizedKey()).isEqualTo("coca cola");
        assertThat(result.normalizedFull()).isEqualTo("coca cola 500ml");
    }

    @Test
    @DisplayName("한글 상품명 정규화 - 특수문자 제거 및 공백 정리, 수량 제거")
    void normalizeKoreanProductName() {
        String raw = "삼겹살(냉장), 1kg";
        var result = normalizer.normalize(raw);

        assertThat(result.normalizedKey()).isEqualTo("삼겹살 냉장");
        assertThat(result.normalizedFull()).isEqualTo("삼겹살 냉장 1kg");
    }

    @Test
    @DisplayName("혼합 상품명 정규화 - 영문/한글/숫자 혼합, 수량 제거")
    void normalizeMixedProductName() {
        String raw = "Apple/사과 (Fresh) 500g";
        var result = normalizer.normalize(raw);

        assertThat(result.normalizedKey()).isEqualTo("apple 사과 fresh");
        assertThat(result.normalizedFull()).isEqualTo("apple 사과 fresh 500g");
    }

    @Test
    @DisplayName("연속 공백 축약")
    void normalizeWithMultipleSpaces() {
        String raw = "Tomato    Sauce     500ml";
        var result = normalizer.normalize(raw);

        assertThat(result.normalizedKey()).isEqualTo("tomato sauce");
        assertThat(result.normalizedFull()).isEqualTo("tomato sauce 500ml");
    }

    @Test
    @DisplayName("stopword 제거 - 주식회사")
    void normalizeWithStopWords() {
        String raw = "주식회사 농심 신라면";
        var result = normalizer.normalize(raw);

        assertThat(result.normalizedKey()).isEqualTo("농심 신라면");
        assertThat(result.normalizedFull()).isEqualTo("농심 신라면");
    }

    @Test
    @DisplayName("공급자/브랜드 토큰 제거 - normalizedKey에서만 제거")
    void normalizeRemoveVendorBrandTokens() {
        String raw = "청양고추 500g 농산";
        var result = normalizer.normalize(raw);

        assertThat(result.normalizedKey()).isEqualTo("청양고추");  // 농산 제거됨
        assertThat(result.normalizedFull()).isEqualTo("청양고추 500g 농산");  // 농산 남아있음
    }

    @Test
    @DisplayName("공급자/브랜드 토큰 제거 - CJ 식품")
    void normalizeRemoveCjBrandToken() {
        String raw = "CJ 식품 대파 1kg";
        var result = normalizer.normalize(raw);

        assertThat(result.normalizedKey()).isEqualTo("대파");  // cj, 식품 제거됨
        assertThat(result.normalizedFull()).isEqualTo("cj 식품 대파 1kg");
    }

    @Test
    @DisplayName("null 입력 처리")
    void normalizeNullInput() {
        var result = normalizer.normalize(null);

        assertThat(result.normalizedKey()).isEmpty();
        assertThat(result.normalizedFull()).isEmpty();
    }

    @Test
    @DisplayName("빈 문자열 입력 처리")
    void normalizeEmptyInput() {
        var result = normalizer.normalize("   ");

        assertThat(result.normalizedKey()).isEmpty();
        assertThat(result.normalizedFull()).isEmpty();
    }

    @Test
    @DisplayName("토큰화 - 공백 기준 분리")
    void tokenizeNormalizedString() {
        String normalized = "coca cola 500ml";
        List<String> tokens = normalizer.tokenize(normalized);

        assertThat(tokens).containsExactly("coca", "cola", "500ml");
    }

    @Test
    @DisplayName("토큰화 - 빈 문자열")
    void tokenizeEmptyString() {
        List<String> tokens = normalizer.tokenize("");

        assertThat(tokens).isEmpty();
    }

    @Test
    @DisplayName("토큰화 - null")
    void tokenizeNull() {
        List<String> tokens = normalizer.tokenize(null);

        assertThat(tokens).isEmpty();
    }

    @Test
    @DisplayName("실제 OCR 데이터 예시 1 - 거래명세서 상품명, 수량 제거")
    void normalizeRealOcrExample1() {
        String raw = "[CJ]백설 포도씨유 500ml";
        var result = normalizer.normalize(raw);

        assertThat(result.normalizedKey()).isEqualTo("백설 포도씨유");  // cj 제거됨
        assertThat(result.normalizedFull()).isEqualTo("cj 백설 포도씨유 500ml");
    }

    @Test
    @DisplayName("실제 OCR 데이터 예시 2 - 괄호와 슬래시 포함, 수량 제거")
    void normalizeRealOcrExample2() {
        String raw = "양파(국산)/1kg";
        var result = normalizer.normalize(raw);

        assertThat(result.normalizedKey()).isEqualTo("양파 국산");
        assertThat(result.normalizedFull()).isEqualTo("양파 국산 1kg");
    }

    @Test
    @DisplayName("수량 표현 MVP 핵심 단위만 제거 (kg, g, ml, l, ea, 개)")
    void normalizeRemoveQuantityPatterns() {
        String raw = "감자 10kg, 당근 500g, 우유 1l";
        var result = normalizer.normalize(raw);

        assertThat(result.normalizedKey()).isEqualTo("감자 당근 우유");
        assertThat(result.normalizedFull()).isEqualTo("감자 10kg 당근 500g 우유 1l");
    }

    @Test
    @DisplayName("학습 키 안정성 테스트 - 동일 재료, 다른 공급자")
    void normalizeSameIngredientDifferentVendor() {
        String raw1 = "청양고추 500g 농산";
        String raw2 = "청양고추 500g 유통";
        String raw3 = "청양고추 500g CJ푸드";

        var result1 = normalizer.normalize(raw1);
        var result2 = normalizer.normalize(raw2);
        var result3 = normalizer.normalize(raw3);

        // 모두 동일한 normalizedKey를 가져야 함 (학습 키 안정성)
        assertThat(result1.normalizedKey()).isEqualTo("청양고추");
        assertThat(result2.normalizedKey()).isEqualTo("청양고추");
        assertThat(result3.normalizedKey()).isEqualTo("청양고추");
    }
}
