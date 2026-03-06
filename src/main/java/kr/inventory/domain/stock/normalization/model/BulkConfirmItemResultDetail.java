package kr.inventory.domain.stock.normalization.model;

import java.util.UUID;

public record BulkConfirmItemResultDetail(
        UUID inboundItemPublicId,
        boolean success,
        ConfirmResult confirmResult,
        String errorMessage
) {
    public static BulkConfirmItemResultDetail success(UUID inboundItemPublicId, ConfirmResult confirmResult) {
        return new BulkConfirmItemResultDetail(inboundItemPublicId, true, confirmResult, null);
    }

    public static BulkConfirmItemResultDetail failure(UUID inboundItemPublicId, String errorMessage) {
        return new BulkConfirmItemResultDetail(inboundItemPublicId, false, null, errorMessage);
    }
}