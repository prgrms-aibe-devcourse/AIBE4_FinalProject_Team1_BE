package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.common.CreatedAtEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stock_inbound_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockInboundItem extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inboundItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inbound_id", nullable = false)
    private StockInbound inbound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(precision = 14, scale = 2)
    private BigDecimal unitCost;

    private LocalDate expirationDate;

    public static StockInboundItem create(
            StockInbound inbound,
            Ingredient ingredient,
            BigDecimal quantity,
            BigDecimal unitCost,
            LocalDate expirationDate
    ) {
        StockInboundItem item = new StockInboundItem();
        item.inbound = inbound;
        item.ingredient = ingredient;
        item.quantity = quantity;
        item.unitCost = unitCost;
        item.expirationDate = expirationDate;
        return item;
    }
}
