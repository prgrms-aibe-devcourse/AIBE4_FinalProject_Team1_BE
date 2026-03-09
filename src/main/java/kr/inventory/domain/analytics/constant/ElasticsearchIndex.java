package kr.inventory.domain.analytics.constant;

// Elasticsearch 인덱스명 상수
public final class ElasticsearchIndex {

    public static final String SALES_ORDERS = "sales_orders";
    public static final String STOCK_INBOUNDS = "stock_inbounds";
    public static final String STOCK_LOGS = "stock_logs";
    public static final String WASTE_RECORDS = "waste_records";

    private ElasticsearchIndex() {
    }
}
