package kr.inventory.domain.analytics.document.sales;

import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Document(indexName = ElasticsearchIndex.SALES_ORDERS)
public record SalesOrderDocument(

        @Id
        String id,

        @Field(type = FieldType.Long)
        Long storeId,

        @Field(type = FieldType.Date, format = DateFormat.date_time)
        OffsetDateTime orderedAt,

        @Field(type = FieldType.Date, format = DateFormat.date_time)
        OffsetDateTime completedAt,

        @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
        BigDecimal totalAmount,

        @Field(type = FieldType.Keyword)
        String status,

        @Field(type = FieldType.Keyword)
        String orderType,

        @Field(type = FieldType.Nested)
        List<SalesOrderItemDocument> items

) {
    public static SalesOrderDocument from(SalesOrder order, List<SalesOrderItem> items) {
        return new SalesOrderDocument(
                String.valueOf(order.getSalesOrderId()),
                order.getStore().getStoreId(),
                order.getOrderedAt(),
                order.getCompletedAt(),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getType().name(),
                items.stream()
                        .map(SalesOrderItemDocument::from)
                        .toList()
        );
    }
}
