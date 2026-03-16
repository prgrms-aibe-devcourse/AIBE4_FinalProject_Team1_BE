package kr.inventory.domain.stock.normalization.model;

import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class InboundSpecExtractor {

    private static final Pattern SPEC_PATTERN = Pattern.compile(
            "([0-9]+(?:[.,][0-9]+)*)(?:\\s*)(kg|ml|ea|개|구|입|매|장|g|l|리터|킬로|그램)",
            Pattern.CASE_INSENSITIVE
    );

    public Optional<Spec> extract(String rawProductName) {
        return extract(rawProductName, null);
    }

    public Optional<Spec> extract(String rawProductName, String specText) {
        if (isBlank(rawProductName) && isBlank(specText)) {
            return Optional.empty();
        }

        String normalizedRaw = normalize(rawProductName);
        String normalizedSpec = normalize(specText);

        List<MatchedSpec> rawCandidates = findCandidates(normalizedRaw);
        List<MatchedSpec> specCandidates = findCandidates(normalizedSpec);

        MatchedSpec selected = selectBest(rawCandidates, specCandidates);
        if (selected == null) {
            return Optional.empty();
        }

        String baseName = buildBaseName(normalizedRaw);
        if (baseName.isBlank()) {
            baseName = normalizedRaw.isBlank() ? normalizedSpec : normalizedRaw;
        }

        return Optional.of(new Spec(baseName, selected.unit(), selected.unitSize()));
    }

    private List<MatchedSpec> findCandidates(String source) {
        if (source.isBlank()) {
            return List.of();
        }

        Matcher matcher = SPEC_PATTERN.matcher(source);
        List<MatchedSpec> candidates = new ArrayList<>();

        while (matcher.find()) {
            try {
                BigDecimal amount = parseNumber(matcher.group(1));
                UnitConversion conversion = convertUnit(matcher.group(2));
                if (conversion == null) {
                    continue;
                }

                BigDecimal unitSize = amount.multiply(conversion.multiplier());
                candidates.add(new MatchedSpec(conversion.unit(), unitSize));
            } catch (NumberFormatException ignored) {
            }
        }

        return candidates;
    }

    private MatchedSpec selectBest(List<MatchedSpec> rawCandidates, List<MatchedSpec> specCandidates) {
        MatchedSpec measurementCandidate = firstMeasurement(rawCandidates);
        if (measurementCandidate != null) {
            return measurementCandidate;
        }

        measurementCandidate = firstMeasurement(specCandidates);
        if (measurementCandidate != null) {
            return measurementCandidate;
        }

        MatchedSpec eaCandidate = firstEa(rawCandidates);
        if (eaCandidate != null) {
            return eaCandidate;
        }

        return firstEa(specCandidates);
    }

    private MatchedSpec firstMeasurement(List<MatchedSpec> candidates) {
        for (MatchedSpec candidate : candidates) {
            if (candidate.unit() == IngredientUnit.G || candidate.unit() == IngredientUnit.ML) {
                return candidate;
            }
        }
        return null;
    }

    private MatchedSpec firstEa(List<MatchedSpec> candidates) {
        for (MatchedSpec candidate : candidates) {
            if (candidate.unit() == IngredientUnit.EA) {
                return candidate;
            }
        }
        return null;
    }

    private String buildBaseName(String normalizedRaw) {
        if (normalizedRaw.isBlank()) {
            return "";
        }

        String baseName = SPEC_PATTERN.matcher(normalizedRaw).replaceAll(" ");
        return baseName.replaceAll("\\s+", " ").trim();
    }

    private BigDecimal parseNumber(String rawValue) {
        String value = rawValue.trim();

        if (value.contains(",") && value.contains(".")) {
            return new BigDecimal(value.replace(",", ""));
        }

        if (value.contains(",")) {
            int commaIndex = value.lastIndexOf(',');
            int digitsAfterComma = value.length() - commaIndex - 1;
            if (digitsAfterComma == 3) {
                return new BigDecimal(value.replace(",", ""));
            }
            return new BigDecimal(value.replace(',', '.'));
        }

        return new BigDecimal(value);
    }

    private UnitConversion convertUnit(String unitStr) {
        String unit = unitStr.toLowerCase();
        return switch (unit) {
            case "개", "ea", "구", "입", "매", "장" -> new UnitConversion(IngredientUnit.EA, BigDecimal.ONE);
            case "ml" -> new UnitConversion(IngredientUnit.ML, BigDecimal.ONE);
            case "l", "리터" -> new UnitConversion(IngredientUnit.ML, BigDecimal.valueOf(1000));
            case "g", "그램" -> new UnitConversion(IngredientUnit.G, BigDecimal.ONE);
            case "kg", "킬로" -> new UnitConversion(IngredientUnit.G, BigDecimal.valueOf(1000));
            default -> null;
        };
    }

    private String normalize(String source) {
        if (isBlank(source)) {
            return "";
        }
        return source.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record UnitConversion(IngredientUnit unit, BigDecimal multiplier) {
    }

    private record MatchedSpec(IngredientUnit unit, BigDecimal unitSize) {
    }

    public record Spec(String baseName, IngredientUnit unit, BigDecimal unitSize) {
    }
}
