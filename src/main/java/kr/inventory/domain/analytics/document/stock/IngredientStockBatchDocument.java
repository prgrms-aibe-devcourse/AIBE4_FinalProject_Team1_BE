package kr.inventory.domain.analytics.document.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.data.annotation.Id;

import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.IngredientStockBatch;

@Document(indexName = ElasticsearchIndex.STOCK_BATCH)
public record IngredientStockBatchDocument(
	@Id
	String id,

	@Field(type = FieldType.Long)
	Long storeId,

	@Field(type = FieldType.Long)
	Long ingredientId,

	@Field(type = FieldType.Keyword)
	String unit,

	@Field(type = FieldType.Text) // 검색용
	String productDisplayName,

	@Field(type = FieldType.Double)
	BigDecimal remainingQuantity,

	@Field(type = FieldType.Double)
	BigDecimal lowStockThreshold,

	@Field(type = FieldType.Date, format = {DateFormat.date, DateFormat.date_time, DateFormat.date_optional_time})
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd['T'HH:mm:ss.SSSX]")
	LocalDate expirationDate,

	@Field(type = FieldType.Keyword)
	String status, // OPEN, CLOSED

	@Field(type = FieldType.Keyword)
	String sourceType, // INBOUND, STOCK_ADJUSTMENT

	@Field(type = FieldType.Date, format = DateFormat.date_time)
	OffsetDateTime createdAt
) {
	public static IngredientStockBatchDocument from(IngredientStockBatch batch, BigDecimal lowStockThreshold) {
		return new IngredientStockBatchDocument(
			String.valueOf(batch.getBatchId()),
			batch.getStore().getStoreId(),
			batch.getIngredientId(),
			batch.getUnit().name(),
			batch.getProductDisplayName(),
			batch.getRemainingQuantity(),
			lowStockThreshold,
			batch.getExpirationDate(),
			batch.getStatus().name(),
			batch.getSourceType().name(),
			batch.getCreatedAt()
		);
	}
}
