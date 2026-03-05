package kr.inventory.global.util;

// 재료명 정규화 유틸리티
public class IngredientNameNormalizer {

    private IngredientNameNormalizer() {
        // 인스턴스 생성 방지
    }

    public static String normalizeForSearch(String text) {
        if (text == null) {
            return null;
        }

        return text.toLowerCase()
                .replaceAll("[,()\\[\\]{}/<>]", " ")  // 특수문자를 공백으로
                .replaceAll("[^a-z0-9가-힣\\s]", " ")  // 영문/숫자/한글/공백만 남김
                .trim()
                .replaceAll("\\s+", " ");  // 연속 공백을 하나로 축약
    }
}
