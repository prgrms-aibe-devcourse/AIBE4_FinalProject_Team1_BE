package kr.dontworry.domain.inventory.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.catalog.entity.Ingredient;
import kr.dontworry.domain.common.CreatedAtEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_inbound_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryInboundItem extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inboundItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inbound_id", nullable = false)
    private InventoryInbound inbound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(precision = 14, scale = 2)
    private BigDecimal unitCost;

    private LocalDate expirationDate;

    public static InventoryInboundItem create(
            InventoryInbound inbound,
            Ingredient ingredient,
            BigDecimal quantity
    ) {
        InventoryInboundItem item = new InventoryInboundItem();
        item.inbound = inbound;
        item.ingredient = ingredient;
        item.quantity = quantity;
        return item;
    }
}
