package kr.inventory.domain.stock.normalization.model;

import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class InboundSpecExtractor {

    private static final Pattern SPEC_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)(?:\\s*)(개|구|입|매|장|kg|g|l|ml|ea)",
            Pattern.CASE_INSENSITIVE
    );

    public Optional<Spec> extract(String rawProductName) {
        if (rawProductName == null || rawProductName.isBlank()) {
            return Optional.empty();
        }

        String normalized = rawProductName.toLowerCase().trim();
        Matcher matcher = SPEC_PATTERN.matcher(normalized);

        Spec best = null;
        int bestPriority = -1;

        while (matcher.find()) {
            String quantityStr = matcher.group(1);
            String unitStr = matcher.group(2);

            try {
                BigDecimal quantity = new BigDecimal(quantityStr.replace(",", "."));
                UnitConversion conversion = convertUnit(unitStr);
                if (conversion == null) continue;

                BigDecimal unitSize = quantity.multiply(conversion.multiplier);

                String baseName = normalized.replaceFirst(Pattern.quote(matcher.group(0)), " ").trim();
                baseName = baseName.replaceAll("\\s+", " ");
                if (baseName.isBlank()) continue;

                if (conversion.priority > bestPriority) {
                    bestPriority = conversion.priority;
                    best = new Spec(baseName, conversion.unit, unitSize);
                }
            } catch (NumberFormatException e) {
            }
        }

        return Optional.ofNullable(best);
    }

    private UnitConversion convertUnit(String unitStr) {
        String u = unitStr.toLowerCase();
        return switch (u) {
            case "개", "ea", "구", "입", "매", "장" -> new UnitConversion(IngredientUnit.EA, BigDecimal.ONE, 3);
            case "ml" -> new UnitConversion(IngredientUnit.ML, BigDecimal.ONE, 2);
            case "l" -> new UnitConversion(IngredientUnit.ML, BigDecimal.valueOf(1000), 2);
            case "g" -> new UnitConversion(IngredientUnit.G, BigDecimal.ONE, 1);
            case "kg" -> new UnitConversion(IngredientUnit.G, BigDecimal.valueOf(1000), 1);
            default -> null;
        };
    }

    private record UnitConversion(IngredientUnit unit, BigDecimal multiplier, int priority) {}

    public record Spec(String baseName, IngredientUnit unit, BigDecimal unitSize) {}
}