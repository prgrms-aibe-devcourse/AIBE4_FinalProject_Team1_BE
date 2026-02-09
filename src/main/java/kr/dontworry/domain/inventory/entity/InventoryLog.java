package kr.dontworry.domain.inventory.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.catalog.entity.Ingredient;
import kr.dontworry.domain.common.CreatedAtEntity;
import kr.dontworry.domain.inventory.entity.enums.ReferenceType;
import kr.dontworry.domain.inventory.entity.enums.TransactionType;
import kr.dontworry.domain.store.entity.Store;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryLog extends CreatedAtEntity {

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

    public static InventoryLog create(
            Store store,
            Ingredient ingredient,
            TransactionType transactionType,
            BigDecimal changeQuantity
    ) {
        InventoryLog log = new InventoryLog();
        log.store = store;
        log.ingredient = ingredient;
        log.transactionType = transactionType;
        log.changeQuantity = changeQuantity;
        return log;
    }
}
