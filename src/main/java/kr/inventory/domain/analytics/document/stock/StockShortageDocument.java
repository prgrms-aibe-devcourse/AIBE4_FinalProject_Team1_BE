package kr.inventory.domain.analytics.document.stock;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.stock.entity.StockShortage;

@Document(indexName = ElasticsearchIndex.StockShortage)
public record StockShortageDocument(

	@Id
	String id,

	@Field(type = FieldType.Long)
	Long storeId,

	@Field(type = FieldType.Long)
	Long salesOrderId,

	@Field(type = FieldType.Long)
	Long ingredientId,

	@Field(type = FieldType.Text)
	String ingredientName,

	@Field(type = FieldType.Double)
	BigDecimal requiredAmount,

	@Field(type = FieldType.Double)
	BigDecimal shortageAmount,

	@Field(type = FieldType.Keyword)
	String status,

	@Field(type = FieldType.Date, format = DateFormat.date_time) OffsetDateTime createdAt
) {
	public static StockShortageDocument from(StockShortage stockShortage, String ingredientName) {
		return new StockShortageDocument(
			String.valueOf(stockShortage.getStockShortageId()),
			stockShortage.getStoreId(),
			stockShortage.getSalesOrderId(),
			stockShortage.getIngredientId(),
			ingredientName,
			stockShortage.getRequiredAmount(),
			stockShortage.getShortageAmount(),
			stockShortage.getStatus().name(),
			stockShortage.getCreatedAt()
		);
	}
}
