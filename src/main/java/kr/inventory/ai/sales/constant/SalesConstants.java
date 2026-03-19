package kr.inventory.ai.sales.constant;

import kr.inventory.domain.analytics.constant.SalesAnalyticsConstants;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SalesConstants {

    private SalesConstants() {
    }

    // 날짜/시간 포맷
    public static final Locale DAY_LABEL_LOCALE = Locale.ENGLISH;
    public static final ZoneId KST = ZoneId.of(SalesAnalyticsConstants.TIMEZONE_KST);
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    public static final Set<String> SUPPORTED_VIEW_TYPES = Set.of("combined", "day_only", "hour_only");

    public static final Map<String, String> STATUS_ALIASES = Map.ofEntries(
            Map.entry("완료", "COMPLETED"),
            Map.entry("주문완료", "COMPLETED"),
            Map.entry("completed", "COMPLETED"),
            Map.entry("환불", "REFUNDED"),
            Map.entry("환불주문", "REFUNDED"),
            Map.entry("refunded", "REFUNDED"),
            Map.entry("refund", "REFUNDED")
    );

    public static final Map<String, String> TYPE_ALIASES = Map.ofEntries(
            Map.entry("홀", "DINE_IN"),
            Map.entry("매장", "DINE_IN"),
            Map.entry("매장식사", "DINE_IN"),
            Map.entry("dinein", "DINE_IN"),
            Map.entry("dine_in", "DINE_IN"),
            Map.entry("포장", "TAKEOUT"),
            Map.entry("takeout", "TAKEOUT"),
            Map.entry("take_out", "TAKEOUT")
    );
}
