package kr.inventory.domain.purchase.constant;

public final class PurchaseOrderConstant {

    public static final String ORDER_NO_DATE_PATTERN = "yyyyMMddHHmmss";
    public static final String ORDER_NO_PREFIX = "PO";
    public static final String ORDER_NO_SEPARATOR = "-";
    public static final String ORDER_NO_SEQUENCE_FORMAT = "%06d";

    public static final String PDF_FILENAME_PREFIX = "purchase-order-";
    public static final String PDF_FILENAME_EXTENSION = ".pdf";

    public static final String PDF_TITLE = "Purchase Order";
    public static final String PDF_ORDER_NO_LABEL = "Order No: ";
    public static final String PDF_STATUS_LABEL = "Status: ";
    public static final String PDF_STORE_ID_LABEL = "Store ID: ";
    public static final String PDF_ITEMS_HEADER = "Items";
    public static final String PDF_TOTAL_LABEL = "Total: ";
    public static final String PDF_NOT_AVAILABLE = "N/A";

    public static final float PDF_START_X = 50f;
    public static final float PDF_START_Y = 770f;
    public static final float PDF_LINE_HEIGHT = 18f;
    public static final float PDF_TOTAL_SPACING = 10f;

    private PurchaseOrderConstant() {
    }
}
