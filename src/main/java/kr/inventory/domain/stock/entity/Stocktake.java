package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.common.AuditableEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stocktake extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stocktakeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private StocktakeSheet sheet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal stocktakeQty;

    @Column(precision = 14, scale = 3)
    private BigDecimal theoreticalQty;

    @Column(precision = 14, scale = 3)
    private BigDecimal varianceQty;

    public static Stocktake createDraft(StocktakeSheet sheet, Ingredient ingredient, BigDecimal qty){
        Stocktake item = new Stocktake();
        item.sheet = sheet;
        item.ingredient = ingredient;
        item.stocktakeQty = qty;
        return item;
    }

    public void updateQuantities(BigDecimal theoreticalQty, BigDecimal varianceQty) {
        this.theoreticalQty = theoreticalQty;
        this.varianceQty = varianceQty;
    }
}
