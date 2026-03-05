package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.normalization.model.ResolutionResult;
import kr.inventory.domain.stock.entity.enums.ResolutionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record IngredientResolveResponse(
        ResolutionStatus status,
        String normalizedRawKey,
        String normalizedRawFull,
        BigDecimal confidence,
        UUID resolvedIngredientPublicId,
        String resolvedIngredientName,
        String resolvedIngredientUnit,
        List<IngredientResolveCandidateResponse> candidates
) {
    public static IngredientResolveResponse from(
            ResolutionResult result,
            List<IngredientResolveCandidateResponse> candidates
    ) {
        return new IngredientResolveResponse(
                result.status(),
                result.normalizedRawKey(),
                result.normalizedRawFull(),
                result.confidence(),
                result.resolvedIngredientPublicId(),
                result.resolvedIngredientName(),
                result.resolvedIngredientUnit(),
                candidates
        );
    }
}