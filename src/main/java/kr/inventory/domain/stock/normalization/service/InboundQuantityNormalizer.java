package kr.inventory.domain.stock.normalization.service;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.normalization.model.InboundSpecExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboundQuantityNormalizer {

    private final InboundSpecExtractor inboundSpecExtractor;

    public NormalizationResult normalize(StockInboundItem inboundItem) {
        return normalize(inboundItem, inboundItem.getIngredient());
    }

    public NormalizationResult normalize(StockInboundItem inboundItem, Ingredient ingredient) {
        return normalize(
                inboundItem.getQuantity(),
                inboundItem.getRawProductName(),
                inboundItem.getSpecText(),
                ingredient
        );
    }

    public NormalizationResult normalize(BigDecimal rawQuantity,
                                         String rawProductName,
                                         String specText,
                                         Ingredient ingredient) {
        BigDecimal safeQuantity = rawQuantity == null ? BigDecimal.ZERO : rawQuantity;
        InboundSpecExtractor.Spec spec = inboundSpecExtractor.extract(rawProductName, specText).orElse(null);

        IngredientUnit resolvedUnit = resolveUnit(spec, ingredient);
        if (resolvedUnit == null) {
            log.debug("[InboundQuantityNormalizer] 단위를 결정할 수 없어 원본 수량을 유지합니다. rawProductName={}, specText={}, quantity={}",
                    rawProductName, specText, safeQuantity);
            return new NormalizationResult(safeQuantity, null, null, true);
        }

        if (hasUnitMismatch(spec, ingredient)) {
            log.warn("[InboundQuantityNormalizer] 스펙 단위와 재료 단위가 일치하지 않습니다. rawProductName={}, specText={}, ingredientUnit={}, specUnit={}",
                    rawProductName,
                    specText,
                    ingredient != null ? ingredient.getUnit() : null,
                    spec != null ? spec.unit() : null);
        }

        BigDecimal unitSize = resolveUnitSize(resolvedUnit, spec, ingredient);
        if (unitSize == null || unitSize.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[InboundQuantityNormalizer] unitSize를 찾지 못해 원본 수량으로 fallback 합니다. rawProductName={}, specText={}, resolvedUnit={}, quantity={}, ingredientId={}",
                    rawProductName,
                    specText,
                    resolvedUnit,
                    safeQuantity,
                    ingredient != null ? ingredient.getIngredientId() : null);
            return new NormalizationResult(safeQuantity, resolvedUnit, null, true);
        }

        BigDecimal normalizedQuantity = safeQuantity.multiply(unitSize);
        log.debug("[InboundQuantityNormalizer] 수량 정규화 완료. rawProductName={}, specText={}, resolvedUnit={}, rawQuantity={}, unitSize={}, normalizedQuantity={}",
                rawProductName,
                specText,
                resolvedUnit,
                safeQuantity,
                unitSize,
                normalizedQuantity);

        return new NormalizationResult(normalizedQuantity, resolvedUnit, unitSize, false);
    }

    private IngredientUnit resolveUnit(InboundSpecExtractor.Spec spec, Ingredient ingredient) {
        // G/ML 규격이 있으면 우선 사용 (kg, l 등이 파싱된 경우)
        if (spec != null && (spec.unit() == IngredientUnit.G || spec.unit() == IngredientUnit.ML)) {
            return spec.unit();
        }

        if (ingredient != null && ingredient.getUnit() != null) {
            return ingredient.getUnit();
        }

        if (spec != null) {
            return spec.unit();
        }

        return null;
    }

    private BigDecimal resolveUnitSize(IngredientUnit resolvedUnit,
                                       InboundSpecExtractor.Spec spec,
                                       Ingredient ingredient) {
        if (spec != null
                && spec.unit() == resolvedUnit
                && spec.unitSize() != null
                && spec.unitSize().compareTo(BigDecimal.ZERO) > 0) {
            return spec.unitSize();
        }

        if (ingredient != null
                && ingredient.getUnit() == resolvedUnit
                && ingredient.getUnitSize() != null
                && ingredient.getUnitSize().compareTo(BigDecimal.ZERO) > 0) {
            return ingredient.getUnitSize();
        }

        return null;
    }

    private boolean hasUnitMismatch(InboundSpecExtractor.Spec spec, Ingredient ingredient) {
        if (spec == null || ingredient == null || ingredient.getUnit() == null) {
            return false;
        }

        if (spec.unit() == IngredientUnit.EA || ingredient.getUnit() == IngredientUnit.EA) {
            return false;
        }

        return spec.unit() != ingredient.getUnit();
    }

    public record NormalizationResult(
            BigDecimal normalizedQuantity,
            IngredientUnit resolvedUnit,
            BigDecimal unitSize,
            boolean fallbackApplied
    ) {
    }
}
