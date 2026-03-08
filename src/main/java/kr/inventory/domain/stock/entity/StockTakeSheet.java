package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.stock.entity.enums.StockTakeStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockTakeSheet extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sheetId;

    @Column(nullable = false, updatable = false)
    private UUID sheetPublicId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockTakeStatus status;

    private OffsetDateTime confirmedAt;

    public static StockTakeSheet create(Long storeId, String title) {
        StockTakeSheet sheet = new StockTakeSheet();
        sheet.sheetPublicId = UUID.randomUUID();
        sheet.storeId = storeId;
        sheet.title = title;
        sheet.status = StockTakeStatus.DRAFT;
        return sheet;
    }

    public void confirm() {
        this.status = StockTakeStatus.CONFIRMED;
        this.confirmedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void updateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("전표 제목은 비어 있을 수 없습니다.");
        }
        this.title = title;
    }
}