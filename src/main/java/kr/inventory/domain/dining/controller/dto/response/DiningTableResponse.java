package kr.inventory.domain.dining.controller.dto.response;

import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.entity.enums.TableStatus;

import java.util.UUID;

public record DiningTableResponse(
        UUID tablePublicId,
        String tableCode,
        TableStatus status
) {
    public static DiningTableResponse from(DiningTable table) {
        return new DiningTableResponse(
                table.getTablePublicId(),
                table.getTableCode(),
                table.getStatus()
        );
    }
}