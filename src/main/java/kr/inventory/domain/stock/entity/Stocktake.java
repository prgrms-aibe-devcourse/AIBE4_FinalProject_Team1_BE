package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.stock.entity.enums.StocktakeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Stocktake {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stocktakeId;

    private Long ingredientId;

    @Enumerated(EnumType.STRING)
    private StocktakeStatus status;

    private BigDecimal stocktakeQty;
    private BigDecimal bookQty;
    private BigDecimal adjustmentQty;

    private LocalDateTime confirmedAt;

    public static Stocktake createDraft(Long ingredientId, BigDecimal qty){
        Stocktake stocktake = new Stocktake();
        stocktake.ingredientId = ingredientId;
        stocktake.stocktakeQty = qty;
        stocktake.status = StocktakeStatus.DRAFT;
        return stocktake;
    }

    public void confirm(BigDecimal bookQty, BigDecimal adjustmentQty) {
        this.bookQty = bookQty;
        this.adjustmentQty = adjustmentQty;
        this.status = StocktakeStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }
}
