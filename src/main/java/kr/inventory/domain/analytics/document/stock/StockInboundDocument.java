package kr.inventory.domain.analytics.document.stock;

import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.stock.entity.StockInbound;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Document(indexName = ElasticsearchIndex.STOCK_INBOUNDS)
public record StockInboundDocument(

        @Id
        String id,

        @Field(type = FieldType.Long)
        Long storeId,

        @Field(type = FieldType.Keyword)
        String vendorName,

        @Field(type = FieldType.Date, format = DateFormat.date)
        LocalDate inboundDate,

        /** DRAFT / CONFIRMED */
        @Field(type = FieldType.Keyword)
        String status,

        @Field(type = FieldType.Date, format = DateFormat.date_time)
        OffsetDateTime confirmedAt

) {
    public static StockInboundDocument from(StockInbound inbound) {
        return new StockInboundDocument(
                String.valueOf(inbound.getInboundId()),
                inbound.getStore().getStoreId(),
                inbound.getVendor() != null ? inbound.getVendor().getName() : null,
                inbound.getInboundDate(),
                inbound.getStatus().name(),
                inbound.getConfirmedAt()
        );
    }
}
