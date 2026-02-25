package kr.inventory.domain.dining.controller.dto.request;

import kr.inventory.domain.dining.entity.enums.TableStatus;

public record DiningTableUpdateRequest(
        String tableCode,
        TableStatus status
) {}