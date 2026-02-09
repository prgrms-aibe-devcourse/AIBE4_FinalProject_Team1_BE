package kr.dontworry.domain.inventory.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.catalog.entity.Ingredient;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.inventory.entity.enums.StockBatchStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ingredient_stock_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientStockBatch extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long batchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inbound_item_id")
    private InventoryInboundItem inboundItem;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal initialQuantity;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal remainingQuantity;

    @Column(precision = 14, scale = 2)
    private BigDecimal unitCost;

    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockBatchStatus status;

    public static IngredientStockBatch create(
            Ingredient ingredient,
            InventoryInboundItem inboundItem,
            BigDecimal initialQuantity
    ) {
        IngredientStockBatch batch = new IngredientStockBatch();
        batch.ingredient = ingredient;
        batch.inboundItem = inboundItem;
        batch.initialQuantity = initialQuantity;
        batch.remainingQuantity = initialQuantity;
        batch.status = StockBatchStatus.OPEN;
        return batch;
    }
}
