package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.common.CreatedAtEntity;
import kr.inventory.domain.stock.entity.enums.WasteReason;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "waste_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WasteRecord extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long wasteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_batch_id", nullable = false)
    private IngredientStockBatch stockBatch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal wasteQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WasteReason wasteReason;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal wasteAmount;

    @Column(nullable = false)
    private LocalDate wasteDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id")
    private User recordedByUser;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public static WasteRecord create(
            Store store,
            IngredientStockBatch stockBatch,
            Ingredient ingredient,
            BigDecimal wasteQuantity,
            WasteReason wasteReason,
            BigDecimal wasteAmount,
            LocalDate wasteDate
    ) {
        WasteRecord wasteRecord = new WasteRecord();
        wasteRecord.store = store;
        wasteRecord.stockBatch = stockBatch;
        wasteRecord.ingredient = ingredient;
        wasteRecord.wasteQuantity = wasteQuantity;
        wasteRecord.wasteReason = wasteReason;
        wasteRecord.wasteAmount = wasteAmount;
        wasteRecord.wasteDate = wasteDate;
        return wasteRecord;
    }
}
