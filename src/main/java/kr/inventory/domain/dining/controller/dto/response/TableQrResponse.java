package kr.inventory.domain.dining.controller.dto.response;

import kr.inventory.domain.dining.entity.TableQr;
import kr.inventory.domain.dining.entity.enums.TableQrStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TableQrResponse(
        UUID qrPublicId,
        UUID tablePublicId,
        String qrImageUrl,
        TableQrStatus status,
        Integer rotationVersion,
        OffsetDateTime createdAt
) {
    public static TableQrResponse from(TableQr qr) {
        return new TableQrResponse(
                qr.getQrPublicId(),
                qr.getTable().getTablePublicId(),
                qr.getQrImageUrl(),
                qr.getStatus(),
                qr.getRotationVersion(),
                qr.getCreatedAt()
        );
    }
}