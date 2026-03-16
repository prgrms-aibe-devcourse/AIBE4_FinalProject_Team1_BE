package kr.inventory.domain.analytics.service.report;

public record ReportData(
        SalesSection  sales,
        RefundSection refund,
        WasteSection waste,
        StockInboundSection inbound
) {}
