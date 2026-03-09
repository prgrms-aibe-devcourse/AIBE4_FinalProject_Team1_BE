package kr.inventory.domain.analytics.document.stock;

import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.stock.entity.StockInbound;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Document(indexName = ElasticsearchIndex.STOCK_INBOUNDS)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockInboundDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long storeId;

    @Field(type = FieldType.Keyword)
    private String vendorName;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate inboundDate;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime confirmedAt;

    public static StockInboundDocument from(StockInbound inbound) {
        StockInboundDocument doc = new StockInboundDocument();
        doc.id = String.valueOf(inbound.getInboundId());
        doc.storeId = inbound.getStore().getStoreId();
        doc.vendorName = inbound.getVendor() != null
                ? inbound.getVendor().getName()
                : null;
        doc.inboundDate = inbound.getInboundDate();
        doc.status = inbound.getStatus().name();
        doc.confirmedAt = inbound.getConfirmedAt();
        return doc;
    }
}
