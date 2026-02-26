package kr.inventory.domain.store.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "store_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_user", columnNames = {"store_id", "user_id"}),
                @UniqueConstraint(name = "uk_user_display_order", columnNames = {"user_id", "display_order"})
        }
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
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean isDefault;

    public static StoreMember create(Store store, User user, StoreMemberRole role, Integer displayOrder, Boolean isDefault) {
        StoreMember member = new StoreMember();
        member.store = store;
        member.user = user;
        member.role = role;
        member.status = StoreMemberStatus.ACTIVE;
        member.displayOrder = displayOrder;
        member.isDefault = isDefault;
        return member;
    }

    public void updateStatus(StoreMemberStatus newStatus) {
        this.status = newStatus;
    }

    public void setAsDefault() {
        this.isDefault = true;
    }
}
