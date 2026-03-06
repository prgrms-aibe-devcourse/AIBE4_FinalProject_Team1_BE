package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.CreatedAtEntity;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.enums.ReferenceType;
import kr.inventory.domain.stock.entity.enums.TransactionType;
import kr.inventory.domain.stock.service.command.StockDeductionLogCommand;
import kr.inventory.domain.stock.service.command.StockInboundLogCommand;
import kr.inventory.domain.stock.service.command.StockWasteCommand;
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
		StockInboundLogCommand command
	) {
		StockLog log = new StockLog();
		log.store = command.store();
		log.ingredient = command.ingredient();
		log.stockBatch = command.batch();
		log.transactionType = TransactionType.INBOUND;
		log.changeQuantity = command.quantity();

		log.balanceAfter = command.balanceAfter();

		log.referenceType = ReferenceType.INBOUND;
		log.referenceId = command.sourceId();
		log.createdByUser = command.user();
		return log;
	}

	public static StockLog createDeductionLog(StockDeductionLogCommand command) {
		StockLog log = new StockLog();
		log.store = command.store();
		log.ingredient = command.ingredient();
		log.stockBatch = command.batch();
		log.transactionType = TransactionType.DEDUCTION;
		log.changeQuantity = command.quantity();
		log.balanceAfter = command.balanceAfter();
		log.referenceType = ReferenceType.SALE;
		log.referenceId = command.sourceId();
		log.createdByUser = null;
		return log;
	}

	public static StockLog createWasteLog(StockWasteCommand command) {
		StockLog log = new StockLog();
		log.store = command.store();
		log.ingredient = command.ingredient();
		log.stockBatch = command.batch();
		log.transactionType = TransactionType.WASTE;
		log.changeQuantity = command.quantity();
		log.balanceAfter = command.balanceAfter();
		log.referenceType = ReferenceType.WASTE;
		log.referenceId = command.sourceId();
		log.createdByUser = command.user();
		return log;
	}
}
