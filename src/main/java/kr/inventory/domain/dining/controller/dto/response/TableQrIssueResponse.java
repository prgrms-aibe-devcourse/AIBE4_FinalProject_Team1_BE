package kr.inventory.domain.dining.controller.dto.response;

import java.util.UUID;

public record TableQrIssueResponse(
        UUID qrPublicId,
        UUID tablePublicId,
        String tableCode,
        int rotationVersion,
        String entryToken,
        String qrUrl
) {
    public static  TableQrIssueResponse from(UUID qrPublicId, UUID tablePublicId, String tableCode, int rotationVersion, String entryToken, String qrUrl) {
        return new TableQrIssueResponse(
                qrPublicId,
                tablePublicId,
                tableCode,
                rotationVersion,
                entryToken,
                qrUrl
        );
    }
}
