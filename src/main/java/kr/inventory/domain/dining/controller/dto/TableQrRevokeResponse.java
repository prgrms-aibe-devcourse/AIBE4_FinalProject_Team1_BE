package kr.inventory.domain.dining.controller.dto;

import java.util.UUID;

public record TableQrRevokeResponse(
        UUID qrPublicId,
        String message
) {}