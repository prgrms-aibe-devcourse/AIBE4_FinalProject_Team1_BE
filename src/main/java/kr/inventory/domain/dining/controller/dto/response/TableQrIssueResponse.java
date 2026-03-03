package kr.inventory.domain.dining.controller.dto.response;

import kr.inventory.domain.dining.entity.TableQr;
import kr.inventory.domain.dining.entity.enums.TableQrStatus;

import java.util.UUID;

public record TableQrIssueResponse(
        UUID qrPublicId,
        UUID tablePublicId,
        String tableCode,
        int rotationVersion,
        String qrImageUrl,
        TableQrStatus status
) {
    public static TableQrIssueResponse from(TableQr qr) {
        return new TableQrIssueResponse(
                qr.getQrPublicId(),
                qr.getTable().getTablePublicId(),
                qr.getTable().getTableCode(),
                qr.getRotationVersion(),
                qr.getQrImageUrl(),
                qr.getStatus()
        );
    }
}