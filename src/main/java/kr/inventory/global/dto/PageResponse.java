package kr.inventory.global.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 공통 DTO.
 * - Spring Data Page를 그대로 노출하지 않고, 필요한 페이징 메타만 반환
 * - 모든 도메인에서 공통으로 사용
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
