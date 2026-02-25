package kr.inventory.domain.dining.controller.dto.response;

import java.util.UUID;

public record TableQrRevokeResponse(
        UUID qrPublicId,
        String message
) {}