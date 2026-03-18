package kr.inventory.domain.analytics.document.stock;

import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.stock.entity.StockLog;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Document(indexName = ElasticsearchIndex.STOCK_LOGS)
public record StockLogDocument(

	@Id
	String id,

	@Field(type = FieldType.Long)
	Long storeId,

	@Field(type = FieldType.Long)
	Long ingredientId,

	@Field(type = FieldType.Keyword)
	String productDisplayName,

	@Field(type = FieldType.Keyword)
	String ingredientName,

	/** INBOUND / DEDUCTION / WASTE / ADJUST */
	@Field(type = FieldType.Keyword)
	String transactionType,

	/** INBOUND / SALE / WASTE / STOCK_TAKING / OTHER */
	@Field(type = FieldType.Keyword)
	String referenceType,

	@Field(type = FieldType.Scaled_Float, scalingFactor = 1000)
	BigDecimal changeQuantity,

	@Field(type = FieldType.Scaled_Float, scalingFactor = 1000)
	BigDecimal balanceAfter,

	@Field(type = FieldType.Keyword)
	String unit,

	@Field(type = FieldType.Date, format = DateFormat.date_time)
	OffsetDateTime createdAt

) {
	public static StockLogDocument from(StockLog stockLog) {
		return new StockLogDocument(
			String.valueOf(stockLog.getLogId()),
			stockLog.getStore().getStoreId(),
			stockLog.getIngredient().getIngredientId(),
			stockLog.getProductDisplayName(),
			stockLog.getIngredient().getName(),
			stockLog.getTransactionType().name(),
			stockLog.getReferenceType() != null ? stockLog.getReferenceType().name() : null,
			stockLog.getChangeQuantity(),
			stockLog.getBalanceAfter(),
			stockLog.getIngredient().getUnit().name(),
			stockLog.getCreatedAt()
		);
	}
}
