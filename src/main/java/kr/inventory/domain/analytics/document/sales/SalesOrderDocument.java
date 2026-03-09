package kr.inventory.domain.analytics.document.sales;

import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Document(indexName = ElasticsearchIndex.SALES_ORDERS)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesOrderDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long storeId;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime orderedAt;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime completedAt;

    @Field(type = FieldType.Double)
    private BigDecimal totalAmount;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String orderType;

    @Field(type = FieldType.Nested)
    private List<SalesOrderItemDocument> items;

    public static SalesOrderDocument from(SalesOrder order, List<SalesOrderItem> items) {
        SalesOrderDocument doc = new SalesOrderDocument();
        doc.id = String.valueOf(order.getSalesOrderId());
        doc.storeId = order.getStore().getStoreId();
        doc.orderedAt = order.getOrderedAt();
        doc.completedAt = order.getCompletedAt();
        doc.totalAmount = order.getTotalAmount();
        doc.status = order.getStatus().name();
        doc.orderType = order.getType().name();
        doc.items = items.stream()
                .map(SalesOrderItemDocument::from)
                .toList();
        return doc;
    }
}
