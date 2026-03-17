package kr.inventory.domain.analytics.constant;

import java.util.Map;

public final class ReportConstants {

    // ==================== waste_records 필드명 ====================
    public static final String FIELD_WASTE_DATE            = "wasteDate";
    public static final String FIELD_WASTE_REASON          = "wasteReason";
    public static final String FIELD_WASTE_QUANTITY        = "wasteQuantity";
    public static final String FIELD_WASTE_AMOUNT          = "wasteAmount";
    public static final String FIELD_PRODUCT_DISPLAY_NAME  = "productDisplayName";
    public static final String FIELD_INGREDIENT_ID         = "ingredientId";
    public static final Map<String, String> WASTE_REASON_LABELS = Map.of(
            "EXPIRED", "유통기한 경과",
            "DAMAGED", "포장 파손",
            "SPOILED", "부패 및 변질",
            "ETC",     "기타 사유"
    );

    // ==================== stock_inbounds 필드명 ====================
    public static final String FIELD_VENDOR_NAME           = "vendorName";
    public static final String DEFAULT_UNKNOWN_VENDOR      = "미등록 거래처";
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
    public static final float PDF_PAGE_WIDTH               = 595.28f; // A4
    public static final int   PDF_TITLE_FONT_SIZE          = 18;
    public static final int   PDF_SECTION_FONT_SIZE        = 13;
    public static final int   PDF_BODY_FONT_SIZE           = 10;
    public static final float PDF_TITLE_SPACING            = 35f;
    public static final float PDF_PERIOD_SPACING           = 25f;
    public static final float PDF_LINE_HEIGHT              = 20f;
    public static final float PDF_SECTION_GAP              = 24f;   // 섹션 간 여백

    // ==================== PDF 개선 레이아웃 추가 상수 ====================
    public static final int   PDF_FONT_VALUE              = 14;   // KPI 숫자 강조
    public static final int   PDF_FONT_LABEL              = 9;    // KPI 레이블
    public static final float PDF_KPI_ROW_HEIGHT          = 50f;  // KPI 박스 높이
    public static final float PDF_SECTION_HEADER_HEIGHT   = 22f;  // 섹션 헤더 박스 높이
    public static final float PDF_TABLE_COL_RANK          = 4f;
    public static final float PDF_TABLE_COL_NAME          = 40f;
    public static final float PDF_TABLE_COL_QUANTITY      = 280f;
    public static final float PDF_TABLE_COL_AMOUNT        = 360f;
    public static final float PDF_TABLE_COL_REASON        = 160f;
    public static final float PDF_TABLE_COL_WASTE_AMOUNT  = 260f;
    public static final float PDF_TABLE_COL_RATIO         = 380f;
    public static final float PDF_TABLE_COL_VENDOR_AMOUNT = 360f;
    public static final float PDF_TABLE_HEADER_TOP_PAD    = 12f;
    public static final float PDF_TABLE_HEADER_BOTTOM_PAD = 6f;
    public static final float PDF_SUB_TITLE_TOP_GAP = 16f;

    // 색상 (RGB 0~1)
    public static final float[] PDF_COLOR_DARK            = {0.1f, 0.1f, 0.1f};
    public static final float[] PDF_COLOR_GRAY            = {0.5f, 0.5f, 0.5f};
    public static final float[] PDF_COLOR_LIGHT_BG        = {0.96f, 0.96f, 0.96f};
    public static final float[] PDF_COLOR_DIVIDER         = {0.88f, 0.88f, 0.88f};
    public static final float[] PDF_COLOR_BOX_BORDER      = {0.8f, 0.8f, 0.8f};

    private ReportConstants() {}
}
