package kr.inventory.domain.vendor.controller.dto;

import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;

import java.time.OffsetDateTime;

public record VendorResponse(
        Long vendorId,
        String name,
        String contactPerson,
        String phone,
        String email,
        Integer leadTimeDays,
        VendorStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static VendorResponse from(Vendor vendor) {
        return new VendorResponse(
                vendor.getVendorId(),
                vendor.getName(),
                vendor.getContactPerson(),
                vendor.getPhone(),
                vendor.getEmail(),
                vendor.getLeadTimeDays(),
                vendor.getStatus(),
                vendor.getCreatedAt(),
                vendor.getUpdatedAt()
        );
    }
}
