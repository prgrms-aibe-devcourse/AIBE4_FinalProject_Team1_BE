package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.stock.entity.enums.ShortageStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_shortage")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockShortage extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockShortageId;

    @Column(unique = true, nullable = false, length = 36)
    private UUID stockShortagePublicId;

    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private Long salesOrderId;

    @Column(nullable = false)
    private Long ingredientId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal requiredAmount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal shortageAmount;

    @Enumerated(EnumType.STRING)
    private ShortageStatus status;

    private OffsetDateTime closedAt;

    private StockShortage(Long storeId, Long salesOrderId, Long ingredientId, BigDecimal requiredAmount, BigDecimal shortageAmount) {
        this.stockShortagePublicId = UUID.randomUUID();
        this.storeId = storeId;
        this.salesOrderId = salesOrderId;
        this.ingredientId = ingredientId;
        this.requiredAmount = requiredAmount;
        this.shortageAmount = shortageAmount;
        this.status = ShortageStatus.PENDING;
    }

    public static StockShortage createPending(Long storeId, Long salesOrderId, Long ingredientId, BigDecimal requiredAmount, BigDecimal shortageAmount) {
        return new StockShortage(storeId, salesOrderId, ingredientId, requiredAmount, shortageAmount);
    }

    public void close() {
        this.status = ShortageStatus.CLOSED;
        this.closedAt = OffsetDateTime.now();
    }
}
