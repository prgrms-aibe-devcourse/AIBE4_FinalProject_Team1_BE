package kr.inventory.domain.product.normalizer;

import kr.inventory.domain.stock.normalization.normalizer.DefaultProductNameNormalizer;
import kr.inventory.domain.stock.normalization.normalizer.ProductNameNormalized;
import kr.inventory.domain.stock.normalization.normalizer.ProductNameNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultProductNameNormalizerTest {

    private ProductNameNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new DefaultProductNameNormalizer(true); // 보관상태 제거
    }

    @Test
    @DisplayName("HACCP 난각번호 1번 자연방사 유정란, 1개, 60구 -> 유정란")
    void normalizeComplexEggProduct() {
        // given
        String rawProductName = "HACCP 난각번호 1번 자연방사 유정란, 1개, 60구";

        // when
        ProductNameNormalized result = normalizer.normalize(rawProductName);

        // then
        assertThat(result.displayName()).isEqualTo("haccp 난각번호 자연방사 유정란");
        assertThat(result.productKey()).isEqualTo("haccp 난각번호 자연방사 유정란");
    }

    @Test
    @DisplayName("서울우유 1L [주식회사] -> 서울우유")
    void normalizeCompanyNameRemoval() {
        // given
        String rawProductName = "서울우유 1L [주식회사]";

        // when
        ProductNameNormalized result = normalizer.normalize(rawProductName);

        // then
        assertThat(result.displayName()).isEqualTo("서울우유");
        assertThat(result.productKey()).isEqualTo("서울우유");
    }

    @Test
    @DisplayName("연세 저지방 우유 1L -> 연세 저지방 우유 (수량 제거, 저지방은 유지)")
    void normalizeLowFatMilk() {
        // given
        String rawProductName = "연세 저지방 우유 1L";

        // when
        ProductNameNormalized result = normalizer.normalize(rawProductName);

        // then
        assertThat(result.displayName()).isEqualTo("연세 저지방 우유");
        assertThat(result.productKey()).isEqualTo("연세 저지방 우유");
    }

    @Test
    @DisplayName("공백 문자열 -> 빈 문자열")
    void normalizeBlankString() {
        // given
        String rawProductName = "  ";

        // when
        ProductNameNormalized result = normalizer.normalize(rawProductName);

        // then
        assertThat(result.displayName()).isEmpty();
        assertThat(result.productKey()).isEmpty();
    }

    @Test
    @DisplayName("null -> 빈 문자열")
    void normalizeNullString() {
        // given
        String rawProductName = null;

        // when
        ProductNameNormalized result = normalizer.normalize(rawProductName);

        // then
        assertThat(result.displayName()).isEmpty();
        assertThat(result.productKey()).isEmpty();
    }

    @Test
    @DisplayName("냉장 상품 1kg (주) -> 상품 (보관상태 및 법인 제거)")
    void normalizeStorageAndCompanyRemoval() {
        // given
        String rawProductName = "냉장 상품 1kg (주)";

        // when
        ProductNameNormalized result = normalizer.normalize(rawProductName);

        // then
        assertThat(result.displayName()).isEqualTo("상품");
        assertThat(result.productKey()).isEqualTo("상품");
    }

    @Test
    @DisplayName("ABC Product 500g Co., Ltd. -> abc product (영문 소문자, 수량/법인 제거)")
    void normalizeEnglishProduct() {
        // given
        String rawProductName = "ABC Product 500g Co., Ltd.";

        // when
        ProductNameNormalized result = normalizer.normalize(rawProductName);

        // then
        assertThat(result.displayName()).isEqualTo("abc product");
        assertThat(result.productKey()).isEqualTo("abc product");
    }

    @Test
    @DisplayName("특수문자 제거 테스트: 김치(100g)/국내산 -> 김치 국내산")
    void normalizeSpecialCharacters() {
        // given
        String rawProductName = "김치(100g)/국내산";

        // when
        ProductNameNormalized result = normalizer.normalize(rawProductName);

        // then
        assertThat(result.displayName()).isEqualTo("김치 국내산");
        assertThat(result.productKey()).isEqualTo("김치 국내산");
    }

    @Test
    @DisplayName("무정란 30개입 -> 무정란 (수량 제거)")
    void normalizeEggWithQuantity() {
        // given
        String rawProductName = "무정란 30개입";

        // when
        ProductNameNormalized result = normalizer.normalize(rawProductName);

        // then
        assertThat(result.displayName()).isEqualTo("무정란");
        assertThat(result.productKey()).isEqualTo("무정란");
    }

    @Test
    @DisplayName("보관상태 유지 옵션 테스트: 냉장 우유 -> 냉장 우유")
    void normalizeWithStorageInfoKeep() {
        // given
        ProductNameNormalizer keepStorageNormalizer = new DefaultProductNameNormalizer(false);
        String rawProductName = "냉장 우유 1L";

        // when
        ProductNameNormalized result = keepStorageNormalizer.normalize(rawProductName);

        // then
        assertThat(result.displayName()).contains("냉장");
        assertThat(result.displayName()).contains("우유");
    }
}
