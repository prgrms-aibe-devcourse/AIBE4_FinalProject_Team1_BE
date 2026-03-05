package kr.inventory.domain.stock.controller.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

public record WastePageResponse<T>(
	List<T> content,
	long totalElements,
	int totalPages,
	int currentPage,
	boolean isFirst,
	boolean isLast,
	boolean hasNext
) {
	public static <T> WastePageResponse<T> from(Page<T> page) {
		return new WastePageResponse<>(
			page.getContent(),
			page.getTotalElements(),
			page.getTotalPages(),
			page.getNumber(),
			page.isFirst(),
			page.isLast(),
			page.hasNext()
		);
	}
}