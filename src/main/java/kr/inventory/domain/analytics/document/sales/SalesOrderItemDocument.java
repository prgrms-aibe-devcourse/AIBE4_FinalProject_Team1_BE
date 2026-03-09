package kr.inventory.domain.analytics.document.sales;

import kr.inventory.domain.sales.entity.SalesOrderItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

// @Document 없음: 독립 인덱스가 아닌 sales_orders 내부에 중첩
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesOrderItemDocument {

    @Field(type = FieldType.Keyword)
    private String menuName;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer quantity;

    @Field(type = FieldType.Double)
    private BigDecimal subtotal;

    public static SalesOrderItemDocument from(SalesOrderItem item) {
        SalesOrderItemDocument doc = new SalesOrderItemDocument();
        doc.menuName = item.getMenuName();
        doc.price = item.getPrice();
        doc.quantity = item.getQuantity();
        doc.subtotal = item.getSubtotal();
        return doc;
    }
}
