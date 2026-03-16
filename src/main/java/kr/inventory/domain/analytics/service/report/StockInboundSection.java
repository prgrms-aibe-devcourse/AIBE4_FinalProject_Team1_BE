package kr.inventory.domain.analytics.service.report;

import java.util.List;

public record StockInboundSection(
        long totalInboundCount,
        List<VendorEntry> vendorBreakdown
) {
    public record VendorEntry(
            String vendorName,
            long inboundCount
    ) {}
}
