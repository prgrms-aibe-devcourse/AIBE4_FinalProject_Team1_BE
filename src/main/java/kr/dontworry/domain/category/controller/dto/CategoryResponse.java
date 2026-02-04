package kr.dontworry.domain.category.controller.dto;

import kr.dontworry.domain.category.entity.Category;
import kr.dontworry.domain.category.entity.enums.CategoryStatus;

import java.time.OffsetDateTime;

public record CategoryResponse(

    Long categoryId,
    String name,
    String icon,
    String color,
    CategoryStatus status,
    Integer sortOrder,
    boolean isDefault,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getName(),
                category.getIcon(),
                category.getColor(),
                category.getStatus(),
                category.getSortOrder(),
                category.isDefault(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}