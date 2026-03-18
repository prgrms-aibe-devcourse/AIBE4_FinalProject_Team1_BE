package kr.inventory.domain.analytics.document.stock;

import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.stock.entity.WasteRecord;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Document(indexName = ElasticsearchIndex.WASTE_RECORDS)
public record WasteRecordDocument(

	@Id
	String id,

	@Field(type = FieldType.Long)
	Long storeId,

	@Field(type = FieldType.Long)
	Long ingredientId,

	@Field(type = FieldType.Keyword)
	String productDisplayName,

	@Field(type = FieldType.Scaled_Float, scalingFactor = 1000)
	BigDecimal wasteQuantity,

	@Field(type = FieldType.Keyword)
	String unit,

	@Field(type = FieldType.Scaled_Float, scalingFactor = 100)
	BigDecimal wasteAmount,

	/** EXPIRED / DAMAGED / SPOILED / ETC */
	@Field(type = FieldType.Keyword)
	String wasteReason,

	@Field(type = FieldType.Date, format = DateFormat.date_time)
	OffsetDateTime wasteDate

) {
	public static WasteRecordDocument from(WasteRecord wasteRecord) {
		return new WasteRecordDocument(
			String.valueOf(wasteRecord.getWasteId()),
			wasteRecord.getStore().getStoreId(),
			wasteRecord.getIngredient().getIngredientId(),
			wasteRecord.getStockBatch().getProductDisplayName(),
			wasteRecord.getWasteQuantity(),
			wasteRecord.getStockBatch().getUnit().name(),
			wasteRecord.getWasteAmount(),
			wasteRecord.getWasteReason().name(),
			wasteRecord.getWasteDate()
		);
	}
}
