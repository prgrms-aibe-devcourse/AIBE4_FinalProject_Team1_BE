package kr.inventory.domain.sales.entity;

import jakarta.persistence.*;
import kr.inventory.domain.catalog.entity.Menu;
import kr.inventory.domain.common.CreatedAtEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesOrderItem extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long salesOrderItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    /**
     * 메뉴명 스냅샷
     * 주문 시점의 메뉴명 저장
     */
    @Column(nullable = false, length = 120)
    private String menuName;

    /**
     * 단가 스냅샷
     * 주문 시점의 Menu.basePrice 저장
     */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * 소계 (price * quantity)
     */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;


    public static SalesOrderItem create(
            SalesOrder salesOrder,
            Menu menu,
            Integer quantity
    ) {
        SalesOrderItem item = new SalesOrderItem();
        item.salesOrder = salesOrder;
        item.menu = menu;
        item.menuName = menu.getName();
        item.price = menu.getBasePrice();
        item.quantity = quantity;
        item.subtotal = menu.getBasePrice().multiply(BigDecimal.valueOf(quantity));
        return item;
    }
}