package kr.inventory.domain.analytics.constant;

import java.time.format.DateTimeFormatter;

public final class SalesAnalyticsConstants {

    // ==================== Elasticsearch 필드명 ====================
    public static final String FIELD_STORE_ID = "storeId";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_ORDERED_AT = "orderedAt";
    public static final String FIELD_TOTAL_AMOUNT = "totalAmount";
    public static final String FIELD_ITEMS = "items";
    public static final String FIELD_ITEMS_MENU_NAME = "items.menuName";
    public static final String FIELD_ITEMS_QUANTITY = "items.quantity";
    public static final String FIELD_ITEMS_SUBTOTAL = "items.subtotal";

    // ==================== Aggregation 이름 ====================
    public static final String AGG_BY_DATE = "by_date";
    public static final String AGG_BY_DAY = "by_day";
    public static final String AGG_BY_HOUR = "by_hour";
    public static final String AGG_BY_MENU = "by_menu";
    public static final String AGG_MENU_NAME = "menu_name";
    public static final String AGG_TOTAL_AMOUNT = "total_amount";
    public static final String AGG_TOTAL_QUANTITY = "total_quantity";
    public static final String AGG_AVG_AMOUNT = "avg_amount";
    public static final String AGG_MAX_AMOUNT = "max_amount";
    public static final String AGG_MIN_AMOUNT = "min_amount";
    public static final String AGG_MENU_FILTER = "menu_filter";
    public static final String AGG_BY_STATUS = "by_status";
    public static final String AGG_TOTAL_SALES_AMOUNT = "total_sales_amount";

    // ==================== 타임존 ====================
    public static final String TIMEZONE_KST = "Asia/Seoul";

    // ==================== Painless Script ====================
    // KST(Asia/Seoul) 기준으로 요일 추출 (1=Monday, 7=Sunday)
    public static final String SCRIPT_DAY_OF_WEEK =
            "doc['orderedAt'].value.withZoneSameInstant(ZoneId.of('" + TIMEZONE_KST + "')).dayOfWeek.value";
    // KST(Asia/Seoul) 기준으로 시간 추출 (0-23)
    public static final String SCRIPT_HOUR_OF_DAY =
            "doc['orderedAt'].value.withZoneSameInstant(ZoneId.of('" + TIMEZONE_KST + "')).hour";

    // ==================== 기본값 ====================
    public static final String DEFAULT_CALENDAR_INTERVAL = "day";
    public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
    public static final DateTimeFormatter ES_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    // ==================== 집계 크기 제한 ====================
    public static final int AGGREGATION_SIZE_DAY_OF_WEEK = 7;   // 요일은 7개
    public static final int AGGREGATION_SIZE_HOUR_OF_DAY = 24;  // 시간은 24개

    // ==================== Service 기본값 ====================
    public static final int DEFAULT_TOP_N = 10;
    public static final int MIN_TOP_N = 1;
    public static final int MAX_TOP_N = 100;
    public static final long MAX_QUERY_DAYS = 365;  // 최대 조회 기간 (1년)
    public static final int DEFAULT_DAYS_BACK = 7;  // 기본 조회 기간 (최근 7일)

    private SalesAnalyticsConstants() {
        // Prevent instantiation
    }
}