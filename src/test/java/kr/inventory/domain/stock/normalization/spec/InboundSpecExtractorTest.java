package kr.inventory.domain.stock.normalization.spec;

import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import kr.inventory.domain.stock.normalization.model.InboundSpecExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InboundSpecExtractor 테스트")
class InboundSpecExtractorTest {

    private final InboundSpecExtractor extractor = new InboundSpecExtractor();

    @Test
    @DisplayName("계란 30개 - EA 단위 추출")
    void extractEgg30Ea() {
        // given
        String rawProductName = "계란 30개";

        // when
        Optional<InboundSpecExtractor.Spec> result = extractor.extract(rawProductName);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().baseName()).isEqualTo("계란");
        assertThat(result.get().unit()).isEqualTo(IngredientUnit.EA);
        assertThat(result.get().unitSize()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    @DisplayName("계란 30구 - EA 단위 추출 (구 = EA)")
    void extractEgg30Pack() {
        // given
        String rawProductName = "계란 30구";

        // when
        Optional<InboundSpecExtractor.Spec> result = extractor.extract(rawProductName);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().baseName()).isEqualTo("계란");
        assertThat(result.get().unit()).isEqualTo(IngredientUnit.EA);
        assertThat(result.get().unitSize()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    @DisplayName("우유 1L - ML 단위로 환산 (1000ml)")
    void extractMilk1L() {
        // given
        String rawProductName = "우유 1L";

        // when
        Optional<InboundSpecExtractor.Spec> result = extractor.extract(rawProductName);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().baseName()).isEqualTo("우유");
        assertThat(result.get().unit()).isEqualTo(IngredientUnit.ML);
        assertThat(result.get().unitSize()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    @DisplayName("양파 10kg - G 단위로 환산 (10000g)")
    void extractOnion10Kg() {
        // given
        String rawProductName = "양파 10kg";

        // when
        Optional<InboundSpecExtractor.Spec> result = extractor.extract(rawProductName);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().baseName()).isEqualTo("양파");
        assertThat(result.get().unit()).isEqualTo(IngredientUnit.G);
        assertThat(result.get().unitSize()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("소금 500g - G 단위 그대로")
    void extractSalt500G() {
        // given
        String rawProductName = "소금 500g";

        // when
        Optional<InboundSpecExtractor.Spec> result = extractor.extract(rawProductName);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().baseName()).isEqualTo("소금");
        assertThat(result.get().unit()).isEqualTo(IngredientUnit.G);
        assertThat(result.get().unitSize()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    @DisplayName("물 2L - ML 단위로 환산 (2000ml)")
    void extractWater2L() {
        // given
        String rawProductName = "물 2L";

        // when
        Optional<InboundSpecExtractor.Spec> result = extractor.extract(rawProductName);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().baseName()).isEqualTo("물");
        assertThat(result.get().unit()).isEqualTo(IngredientUnit.ML);
        assertThat(result.get().unitSize()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    @Test
    @DisplayName("숫자+단위 패턴이 없으면 empty")
    void extractNoPattern() {
        // given
        String rawProductName = "양파";

        // when
        Optional<InboundSpecExtractor.Spec> result = extractor.extract(rawProductName);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null 입력 시 empty")
    void extractNull() {
        // when
        Optional<InboundSpecExtractor.Spec> result = extractor.extract(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("빈 문자열 입력 시 empty")
    void extractBlank() {
        // when
        Optional<InboundSpecExtractor.Spec> result = extractor.extract("   ");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("대소문자 구분 없이 추출 (EA)")
    void extractCaseInsensitive() {
        // given
        String rawProductName = "계란 30EA";

        // when
        Optional<InboundSpecExtractor.Spec> result = extractor.extract(rawProductName);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().unit()).isEqualTo(IngredientUnit.EA);
        assertThat(result.get().unitSize()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    @DisplayName("소수점 포함 수량 처리")
    void extractDecimalQuantity() {
        // given
        String rawProductName = "설탕 1.5kg";

        // when
        Optional<InboundSpecExtractor.Spec> result = extractor.extract(rawProductName);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().baseName()).isEqualTo("설탕");
        assertThat(result.get().unit()).isEqualTo(IngredientUnit.G);
        assertThat(result.get().unitSize()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }
}
