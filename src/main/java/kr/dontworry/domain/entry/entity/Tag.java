package kr.dontworry.domain.entry.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.common.CreatedAtEntity;
import kr.dontworry.domain.ledger.entity.Ledger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "tags",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_tags_ledger_name", columnNames = {"ledger_id", "name"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tagId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 50)
    private String nameNorm;

    public static Tag create(Ledger ledger, String name) {
        Tag tag = new Tag();
        tag.ledger = ledger;
        tag.name = name;
        tag.nameNorm = name.toLowerCase();
        return tag;
    }
}
