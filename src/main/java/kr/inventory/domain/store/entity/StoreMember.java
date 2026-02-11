package kr.inventory.domain.store.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "store_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreMember extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long storeMemberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreMemberStatus status;

    @Column(nullable = false)
    private OffsetDateTime joinedAt;

    public static StoreMember create(Store store, User user, StoreMemberRole role) {
        StoreMember member = new StoreMember();
        member.store = store;
        member.user = user;
        member.role = role;
        member.status = StoreMemberStatus.ACTIVE;
        member.joinedAt = OffsetDateTime.now(ZoneOffset.UTC);
        return member;
    }

    public void updateRole(StoreMemberRole newRole) {
        this.role = newRole;
    }

    public void updateStatus(StoreMemberStatus newStatus) {
        this.status = newStatus;
    }
}
