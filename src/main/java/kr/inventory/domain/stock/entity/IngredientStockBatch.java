package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import kr.inventory.domain.stock.entity.enums.StockBatchStatus;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.store.entity.Store;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ingredient_stock_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientStockBatch extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long batchId;

    @Column(nullable = false)
    private UUID batchPublicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngredientUnit unit;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inbound_item_id")
	private StockInboundItem inboundItem;

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

	private String productDisplayName;

	public static IngredientStockBatch createFromInbound(
		Ingredient ingredient,
		StockInboundItem inboundItem
	) {
		IngredientStockBatch batch = new IngredientStockBatch();
		batch.store = ingredient.getStore();
		batch.ingredient = ingredient;
		batch.unit = ingredient.getUnit();
		batch.inboundItem = inboundItem;

		BigDecimal effectiveQuantity = inboundItem.getQuantity();
		if (ingredient.getUnitSize() != null) {
			effectiveQuantity = effectiveQuantity.multiply(ingredient.getUnitSize());
		}

		batch.initialQuantity = effectiveQuantity;
		batch.remainingQuantity = effectiveQuantity;
		batch.unitCost = inboundItem.getUnitCost();
		batch.expirationDate = inboundItem.getExpirationDate();
		batch.status = StockBatchStatus.OPEN;
		batch.productDisplayName = inboundItem.getProductDisplayName();
		return batch;
	}

	public BigDecimal deductWithClamp(BigDecimal needAmount) {
		if (needAmount.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}

		BigDecimal actualDeduct = this.remainingQuantity.min(needAmount);

		this.remainingQuantity = this.remainingQuantity.subtract(actualDeduct);

		if (this.remainingQuantity.signum() <= 0) {
			this.remainingQuantity = BigDecimal.ZERO;
			this.status = StockBatchStatus.CLOSED;
		}

		return actualDeduct;
	}

	public Long getIngredientId() {
		return this.ingredient.getIngredientId();
	}

	public void updateRemaining(BigDecimal newAmount) {
		this.remainingQuantity = newAmount;
		this.status = (newAmount.signum() <= 0) ? StockBatchStatus.CLOSED : StockBatchStatus.OPEN;
	}

	public static IngredientStockBatch createAdjustment(
		Ingredient ingredient,
		BigDecimal quantity,
		BigDecimal unitCost
	) {
		IngredientStockBatch batch = new IngredientStockBatch();
		batch.store = ingredient.getStore();
		batch.ingredient = ingredient;
		batch.unit = ingredient.getUnit();
		batch.inboundItem = null;
		batch.initialQuantity = quantity;
		batch.remainingQuantity = quantity;
		batch.unitCost = unitCost;
		batch.expirationDate = null;
		batch.status = StockBatchStatus.OPEN;
		return batch;
	}

    public void decreaseQuantity(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new StockException(StockErrorCode.INVALID_WASTE_QUANTITY);
        }

        if (this.remainingQuantity.compareTo(amount) < 0) {
            throw new StockException(StockErrorCode.INSUFFICIENT_STOCK);
        }

        this.remainingQuantity = this.remainingQuantity.subtract(amount);

        if (this.remainingQuantity.signum() <= 0) {
            this.remainingQuantity = BigDecimal.ZERO;
            this.status = StockBatchStatus.CLOSED;
        }
    }

    public void updateProductDisplayName(String productDisplayName) {
        this.productDisplayName = productDisplayName;
    }
}
