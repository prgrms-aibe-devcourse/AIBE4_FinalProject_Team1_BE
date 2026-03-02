package kr.inventory.domain.purchase.entity;

import jakarta.persistence.*;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.common.CreatedAtEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "purchase_order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderItem extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long purchaseOrderItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(precision = 14, scale = 2)
    private BigDecimal unitCost;

    @Column(precision = 14, scale = 2)
    private BigDecimal lineTotal;

    private LocalDate expirationDate;

    public static PurchaseOrderItem create(
            PurchaseOrder purchaseOrder,
            Ingredient ingredient,
            BigDecimal quantity
    ) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.purchaseOrder = purchaseOrder;
        item.ingredient = ingredient;
        item.quantity = quantity;
        return item;
    }
}
