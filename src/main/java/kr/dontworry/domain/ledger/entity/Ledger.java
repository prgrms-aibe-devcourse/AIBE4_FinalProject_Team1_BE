package kr.dontworry.domain.ledger.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.ledger.entity.enums.LedgerStatus;
import kr.dontworry.domain.ledger.entity.enums.LedgerType;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ledger extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long ledgerId;

    @Column(nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerType type;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerStatus status;

    @Column(length = 20)
    private String sharedKind;

    @Column(length = 20)
    private String inviteCode;

    private OffsetDateTime inviteCodeExpiresAt;

    public static Ledger create(User ownerUser, LedgerType type, String name) {
        Ledger ledger = new Ledger();
        ledger.ownerUser = ownerUser;
        ledger.type = type;
        ledger.name = name;
        ledger.status = LedgerStatus.ACTIVE;
        return ledger;
    }
}
