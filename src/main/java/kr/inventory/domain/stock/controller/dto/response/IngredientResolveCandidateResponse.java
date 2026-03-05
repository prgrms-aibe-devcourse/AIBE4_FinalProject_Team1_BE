package kr.inventory.domain.stock.controller.dto.response;

import kr.inventory.domain.stock.normalization.model.ResolutionResult;

import java.util.UUID;

public record IngredientResolveCandidateResponse(
        UUID ingredientPublicId,
        String ingredientName,
        String ingredientUnit,
        Double score
) {
    public static IngredientResolveCandidateResponse from(ResolutionResult.CandidateInfo candidate) {
        return new IngredientResolveCandidateResponse(
                candidate.ingredientPublicId(),
                candidate.ingredientName(),
                candidate.ingredientUnit(),
                candidate.score()
        );
    }
}