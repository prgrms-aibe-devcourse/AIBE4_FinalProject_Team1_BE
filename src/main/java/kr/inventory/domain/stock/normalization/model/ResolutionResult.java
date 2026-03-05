package kr.inventory.domain.stock.normalization.model;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.enums.ResolutionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ResolutionResult(
    ResolutionStatus status,
    String normalizedRawKey,
    String normalizedRawFull,
    BigDecimal confidence,
    UUID resolvedIngredientPublicId,
    String resolvedIngredientName,
    String resolvedIngredientUnit,
    List<CandidateInfo> candidates
) {
    public static ResolutionResult confirmed(String key, String full, Ingredient ingredient) {
        return new ResolutionResult(
                ResolutionStatus.AUTO_RESOLVED,
                key,
                full,
                BigDecimal.ONE,
                ingredient.getIngredientPublicId(),
                ingredient.getName(),
                ingredient.getUnit().name(),
                List.of()
        );
    }

    public static ResolutionResult pending(String key, String full, double confidence, List<CandidateInfo> candidates) {
        return new ResolutionResult(
                ResolutionStatus.PENDING,
                key,
                full,
                BigDecimal.valueOf(confidence),
                null,
                null,
                null,
                candidates
        );
    }

    public static ResolutionResult failed(String key, String full) {
        return new ResolutionResult(
                ResolutionStatus.FAILED,
                key,
                full,
                BigDecimal.ZERO,
                null,
                null,
                null,
                List.of()
        );
    }

    public record CandidateInfo(
        UUID ingredientPublicId,
        String ingredientName,
        String ingredientUnit,
        Double score
    ) {}
}
