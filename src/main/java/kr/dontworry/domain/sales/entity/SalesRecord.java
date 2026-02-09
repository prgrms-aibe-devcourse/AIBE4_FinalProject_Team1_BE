package kr.dontworry.domain.sales.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.catalog.entity.Menu;
import kr.dontworry.domain.common.CreatedAtEntity;
import kr.dontworry.domain.sales.entity.enums.SalesChannel;
import kr.dontworry.domain.store.entity.Store;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "sales_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesRecord extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long saleRecordId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private OffsetDateTime soldAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    @Column(length = 120)
    private String menuNameRaw;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SalesChannel channel;

    public static SalesRecord create(
            Store store,
            OffsetDateTime soldAt,
            Integer quantity,
            BigDecimal grossAmount,
            SalesChannel channel
    ) {
        SalesRecord record = new SalesRecord();
        record.store = store;
        record.soldAt = soldAt;
        record.quantity = quantity;
        record.grossAmount = grossAmount;
        record.channel = channel;
        return record;
    }
}
