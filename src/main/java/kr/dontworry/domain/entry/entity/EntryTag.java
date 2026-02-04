package kr.dontworry.domain.entry.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.common.CreatedAtEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "entry_tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_entry_tags_ledger_entry_tag",
                columnNames = {"ledger_id", "entry_id", "tag_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntryTag extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_tag_id", nullable = false, updatable = false)
    private Long entryTagId;

    @Column(name = "ledger_id", nullable = false)
    private UUID ledgerId;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "ledger_id", referencedColumnName = "ledger_id", insertable = false, updatable = false),
            @JoinColumn(name = "entry_id", referencedColumnName = "entry_id", insertable = false, updatable = false)
    })
    private Entry entry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "ledger_id", referencedColumnName = "ledger_id", insertable = false, updatable = false),
            @JoinColumn(name = "tag_id", referencedColumnName = "tag_id", insertable = false, updatable = false)
    })
    private Tag tag;

    public static EntryTag create(Entry entry, Tag tag) {
        EntryTag entity = new EntryTag();
        entity.ledgerId = entry.getLedger().getLedgerId();
        entity.entryId = entry.getEntryId();
        entity.tagId = tag.getTagId();
        entity.entry = entry;
        entity.tag = tag;
        return entity;
    }
}
