package kr.dontworry.domain.ledger.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.ledger.entity.enums.MembershipRole;
import kr.dontworry.domain.ledger.entity.enums.MembershipStatus;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "ledger_memberships",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_membership_ledger_user", columnNames = {"ledger_id", "user_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long membershipId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipRole role;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal defaultShareRatio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status;

    @Column(nullable = false)
    private OffsetDateTime joinedAt;

    private OffsetDateTime leftAt;

    public static LedgerMembership create(Ledger ledger, User user, MembershipRole role) {
        LedgerMembership membership = new LedgerMembership();
        membership.ledger = ledger;
        membership.user = user;
        membership.role = role;
        membership.status = MembershipStatus.ACTIVE;
        membership.joinedAt = OffsetDateTime.now();
        membership.defaultShareRatio = BigDecimal.ZERO;
        return membership;
    }
}
