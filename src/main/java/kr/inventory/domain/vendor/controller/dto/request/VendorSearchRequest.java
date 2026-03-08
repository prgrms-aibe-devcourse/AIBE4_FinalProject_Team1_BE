package kr.inventory.domain.vendor.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;

public record VendorSearchRequest(
        @Schema(description = "상태 필터 (ACTIVE | INACTIVE)", example = "ACTIVE")
        VendorStatus status,

        @Schema(description = "거래처명 검색어", example = "농협")
        String search
) {
}