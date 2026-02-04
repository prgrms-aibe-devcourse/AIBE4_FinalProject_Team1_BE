package kr.dontworry.domain.entry.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.category.entity.Category;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.entry.entity.enums.EntryStatus;
import kr.dontworry.domain.entry.entity.enums.EntryType;
import kr.dontworry.domain.ledger.entity.Ledger;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "entry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long entryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private LocalDate entryDate;

    private LocalTime entryTime;

    @Column(columnDefinition = "text")
    private String merchantName;

    @Column(columnDefinition = "text")
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryStatus status;

    private OffsetDateTime deletedAt;

    public static Entry create(
        Ledger ledger,
        User createdByUser,
        EntryType type,
        Long amount,
        LocalDate entryDate
    ) {
        Entry entry = new Entry();
        entry.ledger = ledger;
        entry.createdByUser = createdByUser;
        entry.type = type;
        entry.amount = amount;
        entry.entryDate = entryDate;
        entry.status = EntryStatus.ACTIVE;
        return entry;
    }
}
