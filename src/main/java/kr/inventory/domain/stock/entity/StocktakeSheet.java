package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.stock.entity.enums.StocktakeStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StocktakeSheet extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sheetId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StocktakeStatus status;

    private OffsetDateTime confirmedAt;

    public static StocktakeSheet create(String title) {
        StocktakeSheet sheet = new StocktakeSheet();
        sheet.title = title;
        sheet.status = StocktakeStatus.DRAFT;
        return sheet;
    }

    public void confirm() {
        this.status = StocktakeStatus.CONFIRMED;
        this.confirmedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}