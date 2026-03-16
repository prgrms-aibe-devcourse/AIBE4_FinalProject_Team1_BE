package kr.inventory.domain.analytics.constant;

public final class ReportConstants {

    // ==================== waste_records 필드명 ====================
    public static final String FIELD_WASTE_DATE            = "wasteDate";
    public static final String FIELD_WASTE_REASON          = "wasteReason";
    public static final String FIELD_WASTE_QUANTITY        = "wasteQuantity";
    public static final String FIELD_WASTE_AMOUNT          = "wasteAmount";
    public static final String FIELD_PRODUCT_DISPLAY_NAME  = "productDisplayName";
    public static final String FIELD_INGREDIENT_ID         = "ingredientId";

    // ==================== stock_inbounds 필드명 ====================
    public static final String FIELD_VENDOR_NAME           = "vendorName";
    public static final String FIELD_INBOUND_DATE          = "inboundDate";

    // ==================== Aggregation 이름 ====================
    public static final String AGG_BY_REASON               = "by_reason";
    public static final String AGG_BY_VENDOR               = "by_vendor";
    public static final String AGG_BY_INGREDIENT           = "by_ingredient";
    public static final String AGG_SUM_WASTE_AMOUNT        = "sum_waste_amount";
    public static final String AGG_SUM_WASTE_QUANTITY      = "sum_waste_quantity";
    public static final String AGG_TOTAL_REFUND_AMOUNT     = "total_refund_amount";
    public static final String AGG_TOP_HIT                 = "top_hit";
    public static final String DEFAULT_UNKNOWN_INGREDIENT  = "알 수 없는 식재료";

    // ==================== Status 값 ====================
    public static final String STATUS_REFUNDED             = "REFUNDED";
    public static final String STATUS_CONFIRMED            = "CONFIRMED";

    // ==================== 집계 크기 ====================
    public static final int REPORT_TOP_N_MENU              = 5;
    public static final int REPORT_TOP_N_WASTE_INGREDIENT  = 5;
    public static final int REPORT_REASON_SIZE             = 10;  // wasteReason 종류 최대
    public static final int REPORT_VENDOR_SIZE             = 100; // 거래처 수 최대

    // ==================== PDF 레이아웃 ====================
    public static final float PDF_START_Y                  = 780f;
    public static final float PDF_MARGIN_LEFT              = 50f;
    public static final float PDF_MARGIN_RIGHT             = 50f;
    public static final float PDF_MARGIN_BOTTOM            = 50f;
    public static final float PDF_PAGE_WIDTH               = 595.28f; // A4
    public static final int   PDF_TITLE_FONT_SIZE          = 18;
    public static final int   PDF_SECTION_FONT_SIZE        = 13;
    public static final int   PDF_BODY_FONT_SIZE           = 10;
    public static final float PDF_TITLE_SPACING            = 35f;
    public static final float PDF_PERIOD_SPACING           = 25f;
    public static final float PDF_SECTION_HEADER_SPACING   = 22f;
    public static final float PDF_LINE_HEIGHT              = 16f;
    public static final float PDF_SECTION_GAP              = 24f;   // 섹션 간 여백

    private ReportConstants() {}
}
