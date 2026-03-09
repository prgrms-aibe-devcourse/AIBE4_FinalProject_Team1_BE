package kr.inventory.domain.analytics.document.stock;

import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.stock.entity.WasteRecord;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Document(indexName = ElasticsearchIndex.WASTE_RECORDS)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WasteRecordDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long storeId;

    @Field(type = FieldType.Long)
    private Long ingredientId;

    @Field(type = FieldType.Keyword)
    private String ingredientName;

    @Field(type = FieldType.Double)
    private BigDecimal wasteQuantity;

    @Field(type = FieldType.Keyword)
    private String wasteReason;

    @Field(type = FieldType.Double)
    private BigDecimal wasteAmount;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime wasteDate;

    public static WasteRecordDocument from(WasteRecord wasteRecord) {
        WasteRecordDocument doc = new WasteRecordDocument();
        doc.id = String.valueOf(wasteRecord.getWasteId());
        doc.storeId = wasteRecord.getStore().getStoreId();
        doc.ingredientId = wasteRecord.getIngredient().getIngredientId();
        doc.ingredientName = wasteRecord.getIngredient().getName();
        doc.wasteQuantity = wasteRecord.getWasteQuantity();
        doc.wasteReason = wasteRecord.getWasteReason().name();
        doc.wasteAmount = wasteRecord.getWasteAmount();
        doc.wasteDate = wasteRecord.getWasteDate();
        return doc;
    }
}
