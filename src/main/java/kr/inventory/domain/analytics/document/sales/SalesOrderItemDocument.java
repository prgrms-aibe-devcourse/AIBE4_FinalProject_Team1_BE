package kr.inventory.domain.analytics.document.sales;

import kr.inventory.domain.sales.entity.SalesOrderItem;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

// @Document 없음: 독립 인덱스가 아닌 sales_orders 내부에 중첩
public record SalesOrderItemDocument(

        @Field(type = FieldType.Keyword)
        String menuName,

        @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
        BigDecimal price,

        @Field(type = FieldType.Integer)
        Integer quantity,

        @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
        BigDecimal subtotal

) {
    public static SalesOrderItemDocument from(SalesOrderItem item) {
        return new SalesOrderItemDocument(
                item.getMenuName(),
                item.getPrice(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }
}
