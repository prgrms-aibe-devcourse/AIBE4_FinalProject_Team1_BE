package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.common.AuditableEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockTake extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockTakeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private StockTakeSheet sheet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal stockTakeQty;

    @Column(precision = 14, scale = 3)
    private BigDecimal theoreticalQty;

    @Column(precision = 14, scale = 3)
    private BigDecimal varianceQty;

    public static StockTake createDraft(StockTakeSheet sheet, Ingredient ingredient, BigDecimal qty){
        StockTake item = new StockTake();
        item.sheet = sheet;
        item.ingredient = ingredient;
        item.stockTakeQty = qty;
        return item;
    }

    public void updateQuantities(BigDecimal theoreticalQty, BigDecimal varianceQty) {
        this.theoreticalQty = theoreticalQty;
        this.varianceQty = varianceQty;
    }

    public void updateStockTakeQty(BigDecimal newQty){
        if(newQty == null){
            throw new IllegalArgumentException("수량은 null일 수 없습니다.");
        }
        if(newQty.compareTo(BigDecimal.ZERO) < 0){
            throw new IllegalArgumentException("수량은 0 이상이어야 합니다.");
        }
        this.stockTakeQty = newQty;
    }
}
