package kr.dontworry.domain.category.controller.dto;

import java.util.UUID;

public record CategoryOrderRequest(
        UUID publicId,
        Integer sortOrder
) {}