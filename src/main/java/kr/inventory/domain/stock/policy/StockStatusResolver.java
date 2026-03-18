package kr.inventory.domain.stock.policy;

import java.math.BigDecimal;

public class StockStatusResolver {

    public static String resolve(BigDecimal currentQuantity, BigDecimal threshold) {
        BigDecimal safe = currentQuantity == null ? BigDecimal.ZERO : currentQuantity;

        if (safe.compareTo(BigDecimal.ZERO) <= 0) {
            return "OUT_OF_STOCK";
        }

        if (threshold != null && safe.compareTo(threshold) <= 0) {
            return "LOW_STOCK";
        }

        return "NORMAL";
    }

    public static boolean isBelowThreshold(BigDecimal currentQuantity, BigDecimal threshold) {
        if (threshold == null) {
            return false;
        }

        BigDecimal safe = currentQuantity == null ? BigDecimal.ZERO : currentQuantity;
        return safe.compareTo(threshold) <= 0;
    }
}