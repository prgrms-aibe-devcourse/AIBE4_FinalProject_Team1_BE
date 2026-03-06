package kr.inventory.domain.stock.normalization.model;

import java.util.UUID;

public record ConfirmResult(
    UUID inboundItemPublicId,
    UUID ingredientPublicId,
    String ingredientName,
    String normalizedRawKey,
    boolean newMappingCreated
) {}
