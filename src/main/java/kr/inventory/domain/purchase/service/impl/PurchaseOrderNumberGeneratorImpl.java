package kr.inventory.domain.purchase.service.impl;

import kr.inventory.domain.purchase.constant.PurchaseOrderConstant;
import kr.inventory.domain.purchase.service.PurchaseOrderNumberGenerator;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PurchaseOrderNumberGeneratorImpl implements PurchaseOrderNumberGenerator {

    private static final DateTimeFormatter ORDER_NO_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(PurchaseOrderConstant.ORDER_NO_DATE_PATTERN);

    @Override
    public String generate(Long purchaseOrderId, OffsetDateTime submittedAt) {
        String datePart = submittedAt.format(ORDER_NO_DATE_FORMATTER);
        String sequencePart = String.format(PurchaseOrderConstant.ORDER_NO_SEQUENCE_FORMAT, purchaseOrderId);
        return String.join(
                PurchaseOrderConstant.ORDER_NO_SEPARATOR,
                PurchaseOrderConstant.ORDER_NO_PREFIX,
                datePart,
                sequencePart
        );
    }
}
