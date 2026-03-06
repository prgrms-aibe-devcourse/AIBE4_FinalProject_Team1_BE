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

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal theoreticalQty;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal varianceQty;

    public static StockTake createDraft(
            StockTakeSheet sheet,
            Ingredient ingredient,
            BigDecimal theoreticalQty,
            BigDecimal stockTakeQty
    ) {
        validateQty(theoreticalQty);
        validateQty(stockTakeQty);

        StockTake item = new StockTake();
        item.sheet = sheet;
        item.ingredient = ingredient;
        item.theoreticalQty = theoreticalQty;
        item.stockTakeQty = stockTakeQty;
        item.varianceQty = stockTakeQty.subtract(theoreticalQty);
        return item;
    }

    public void updateStockTakeQty(BigDecimal newQty) {
        validateQty(newQty);
        this.stockTakeQty = newQty;
        this.varianceQty = newQty.subtract(this.theoreticalQty);
    }

    private static void validateQty(BigDecimal qty) {
        if (qty == null) {
            throw new IllegalArgumentException("수량은 null일 수 없습니다.");
        }
        if (qty.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("수량은 0 이상이어야 합니다.");
        }
    }
}