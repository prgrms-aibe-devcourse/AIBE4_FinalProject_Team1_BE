package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.stock.entity.enums.StocktakeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Getter
@NoArgsConstructor
public class Stocktake extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stocktakeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StocktakeStatus status;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal stocktakeQty;

    @Column(precision = 14, scale = 3)
    private BigDecimal theoreticalQty;

    @Column(precision = 14, scale = 3)
    private BigDecimal varianceQty;

    private OffsetDateTime confirmedAt;

    public static Stocktake createDraft(Ingredient ingredient, BigDecimal qty){
        Stocktake stocktake = new Stocktake();
        stocktake.ingredient = ingredient;
        stocktake.stocktakeQty = qty;
        stocktake.status = StocktakeStatus.DRAFT;
        return stocktake;
    }

    public void confirm(BigDecimal theoreticalQty, BigDecimal varianceQty) {
        this.theoreticalQty = theoreticalQty;
        this.varianceQty = varianceQty;
        this.status = StocktakeStatus.CONFIRMED;
        this.confirmedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
