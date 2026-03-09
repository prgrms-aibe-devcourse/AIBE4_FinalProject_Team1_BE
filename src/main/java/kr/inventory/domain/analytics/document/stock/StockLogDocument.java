package kr.inventory.domain.analytics.document.stock;

import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.stock.entity.StockLog;
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

@Document(indexName = ElasticsearchIndex.STOCK_LOGS)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockLogDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long storeId;

    @Field(type = FieldType.Long)
    private Long ingredientId;

    @Field(type = FieldType.Keyword)
    private String ingredientName;

    @Field(type = FieldType.Keyword)
    private String transactionType;

    @Field(type = FieldType.Keyword)
    private String referenceType;

    @Field(type = FieldType.Double)
    private BigDecimal changeQuantity;

    @Field(type = FieldType.Double)
    private BigDecimal balanceAfter;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime createdAt;

    public static StockLogDocument from(StockLog stockLog) {
        StockLogDocument doc = new StockLogDocument();
        doc.id = String.valueOf(stockLog.getLogId());
        doc.storeId = stockLog.getStore().getStoreId();
        doc.ingredientId = stockLog.getIngredient().getIngredientId();
        doc.ingredientName = stockLog.getIngredient().getName();
        doc.transactionType = stockLog.getTransactionType().name();
        doc.referenceType = stockLog.getReferenceType() != null
                ? stockLog.getReferenceType().name()
                : null;
        doc.changeQuantity = stockLog.getChangeQuantity();
        doc.balanceAfter = stockLog.getBalanceAfter();
        doc.createdAt = stockLog.getCreatedAt();
        return doc;
    }
}
