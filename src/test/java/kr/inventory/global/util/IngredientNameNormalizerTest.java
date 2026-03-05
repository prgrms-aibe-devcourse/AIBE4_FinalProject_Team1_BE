package kr.inventory.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IngredientNameNormalizer 테스트")
class IngredientNameNormalizerTest {

    @Test
    @DisplayName("한글 재료명 정규화")
    void normalizeKoreanName() {
        String input = "청양고추(냉장)";
        String result = IngredientNameNormalizer.normalizeForSearch(input);

        assertThat(result).isEqualTo("청양고추 냉장");
    }

    @Test
    @DisplayName("영문 재료명 정규화 - 소문자 변환")
    void normalizeEnglishName() {
        String input = "Tomato (Fresh)";
        String result = IngredientNameNormalizer.normalizeForSearch(input);

        assertThat(result).isEqualTo("tomato fresh");
    }

    @Test
    @DisplayName("혼합 재료명 정규화 - 영문/한글/숫자")
    void normalizeMixedName() {
        String input = "Apple/사과 100";
        String result = IngredientNameNormalizer.normalizeForSearch(input);

        assertThat(result).isEqualTo("apple 사과 100");
    }

    @Test
    @DisplayName("특수문자 제거")
    void normalizeRemoveSpecialCharacters() {
        String input = "대파[국산]{신선}";
        String result = IngredientNameNormalizer.normalizeForSearch(input);

        assertThat(result).isEqualTo("대파 국산 신선");
    }

    @Test
    @DisplayName("연속 공백 축약")
    void normalizeMultipleSpaces() {
        String input = "양파    신선     국산";
        String result = IngredientNameNormalizer.normalizeForSearch(input);

        assertThat(result).isEqualTo("양파 신선 국산");
    }

    @Test
    @DisplayName("null 입력 처리")
    void normalizeNullInput() {
        String result = IngredientNameNormalizer.normalizeForSearch(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("빈 문자열 입력 처리")
    void normalizeEmptyInput() {
        String input = "   ";
        String result = IngredientNameNormalizer.normalizeForSearch(input);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("앞뒤 공백 제거 (trim)")
    void normalizeWithLeadingTrailingSpaces() {
        String input = "  청양고추  ";
        String result = IngredientNameNormalizer.normalizeForSearch(input);

        assertThat(result).isEqualTo("청양고추");
    }

    @Test
    @DisplayName("DefaultRawProductNameNormalizer와 동일한 기본 규칙 적용")
    void normalizeConsistentWithRawProductNameNormalizer() {
        // 동일한 입력에 대해 동일한 기본 정규화 결과를 보장
        String input = "Coca-Cola (500ml)";
        String result = IngredientNameNormalizer.normalizeForSearch(input);

        // 수량 제거는 RawProductNameNormalizer에서만 수행
        // 여기서는 기본 정규화만 확인
        assertThat(result).isEqualTo("coca cola 500ml");
    }
}
