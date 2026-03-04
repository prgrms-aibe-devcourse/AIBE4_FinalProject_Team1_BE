package kr.inventory.domain.purchase.constant;

public final class PurchaseOrderConstant {

    // ==================== 주문번호 관련 ====================
    public static final String ORDER_NO_PREFIX = "PO";
    public static final String ORDER_NO_SEPARATOR = "-";
    public static final String ORDER_NO_DATE_PATTERN = "yyyyMMddHHmmss";
    public static final String ORDER_NO_SEQUENCE_FORMAT = "%06d";

    // ==================== PDF 파일명 ====================
    public static final String PDF_FILENAME_PREFIX = "purchase-order-";
    public static final String PDF_FILENAME_EXTENSION = ".pdf";

    // ==================== PDF 레이아웃 ====================
    // 페이지 설정
    public static final float PDF_PAGE_WIDTH = 595f;  // A4 너비
    public static final float PDF_PAGE_HEIGHT = 842f; // A4 높이

    // 여백
    public static final float PDF_MARGIN_LEFT = 50f;
    public static final float PDF_MARGIN_RIGHT = 50f;
    public static final float PDF_MARGIN_TOP = 50f;

    // Y 시작 위치
    public static final float PDF_START_Y = PDF_PAGE_HEIGHT - PDF_MARGIN_TOP;

    // 폰트 크기
    public static final int PDF_TITLE_FONT_SIZE = 24;
    public static final int PDF_HEADER_FONT_SIZE = 12;
    public static final int PDF_BODY_FONT_SIZE = 10;
    public static final int PDF_TABLE_HEADER_FONT_SIZE = 11;
    public static final int PDF_TABLE_BODY_FONT_SIZE = 10;

    // 줄 간격
    public static final float PDF_LINE_HEIGHT = 20f;
    public static final float PDF_SECTION_SPACING = 30f;
    public static final float PDF_TITLE_SPACING = 40f;
    public static final float PDF_TABLE_ROW_HEIGHT = 25f;
    public static final float PDF_TABLE_HEADER_HEIGHT = 30f;

    // ==================== PDF 텍스트 ====================
    public static final String PDF_TITLE = "발주서";
    public static final String PDF_ORDER_NO_LABEL = "주문번호: ";
    public static final String PDF_STORE_NAME_LABEL = "매장명: ";
    public static final String PDF_VENDOR_NAME_LABEL = "거래처: ";

    // 테이블 헤더
    public static final String PDF_TABLE_HEADER_ITEM_NAME = "품목명";
    public static final String PDF_TABLE_HEADER_QUANTITY = "수량";
    public static final String PDF_TABLE_HEADER_UNIT_PRICE = "단가";
    public static final String PDF_TABLE_HEADER_LINE_AMOUNT = "금액";

    // 총액
    public static final String PDF_TOTAL_LABEL = "총 금액: ";

    // 테이블 컬럼 너비 비율 (총합 1.0)
    public static final float PDF_TABLE_COL_ITEM_NAME_RATIO = 0.40f;  // 40%
    public static final float PDF_TABLE_COL_QUANTITY_RATIO = 0.15f;   // 15%
    public static final float PDF_TABLE_COL_UNIT_PRICE_RATIO = 0.225f; // 22.5%
    public static final float PDF_TABLE_COL_LINE_AMOUNT_RATIO = 0.225f; // 22.5%

    // 테이블 셀 패딩
    public static final float PDF_TABLE_CELL_PADDING = 5f;

    // 기본값
    public static final String PDF_NOT_AVAILABLE = "N/A";

    private PurchaseOrderConstant() {
    }
}
