package kr.inventory.domain.stock.normalization.normalizer;

public interface ProductNameNormalizer {

    /**
     * 원문 상품명을 정규화하여 표시용 상품명과 검색용 키를 생성합니다.
     *
     * @param rawProductName OCR 등으로 추출한 원문 상품명
     * @return 정규화된 상품명 정보
     */
    ProductNameNormalized normalize(String rawProductName);
}
