package kr.inventory.ai.sales.tool.dto.response;

import kr.inventory.global.common.PageResponse;

public record SalesRecordsPageInfoToolResponse(
        int currentPage,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static SalesRecordsPageInfoToolResponse from(PageResponse<?> pageResponse) {
        return new SalesRecordsPageInfoToolResponse(
                pageResponse.page() + 1,
                pageResponse.size(),
                pageResponse.totalElements(),
                pageResponse.totalPages(),
                pageResponse.hasNext()
        );
    }
}
