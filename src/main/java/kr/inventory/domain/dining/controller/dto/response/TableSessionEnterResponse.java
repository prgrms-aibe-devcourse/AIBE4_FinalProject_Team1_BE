package kr.inventory.domain.dining.controller.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TableSessionEnterResponse(
        UUID sessionPublicId,
        String sessionToken,
        OffsetDateTime expiresAt
) {
    public static  TableSessionEnterResponse from(UUID sessionPublicId, String sessionToken, OffsetDateTime expiresAt) {
        return new TableSessionEnterResponse(sessionPublicId, sessionToken, expiresAt);
    }
}
