package kr.inventory.domain.purchase.service;

import java.time.OffsetDateTime;

public interface PurchaseOrderNumberGenerator {
    String generate(Long purchaseOrderId, OffsetDateTime submittedAt);
}
