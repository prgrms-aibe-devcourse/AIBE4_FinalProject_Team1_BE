package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.common.CreatedAtEntity;
import kr.inventory.domain.stock.entity.enums.ReferenceType;
import kr.inventory.domain.stock.entity.enums.TransactionType;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockLog extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_batch_id")
    private IngredientStockBatch stockBatch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal changeQuantity;

    @Column(precision = 14, scale = 3)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReferenceType referenceType;

    private Long referenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    public static StockLog createInboundLog(
            Store store,
            Ingredient ingredient,
            BigDecimal changeQuantity,
            IngredientStockBatch stockBatch,
            Long inboundId,
            User createdByUser
    ) {
        StockLog log = new StockLog();
        log.store = store;
        log.ingredient = ingredient;
        log.transactionType = TransactionType.INBOUND;
        log.changeQuantity = changeQuantity;
        log.stockBatch = stockBatch;
        log.referenceType = ReferenceType.INBOUND;
        log.referenceId = inboundId;
        log.createdByUser = createdByUser;
        return log;
    }
}
