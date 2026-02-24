package kr.inventory.domain.store.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.store.entity.enums.InvitationStatus;
import kr.inventory.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "store_invitations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_invitation_store", columnNames = "store_id"),
                @UniqueConstraint(name = "uk_store_invitation_token", columnNames = "token")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long invitationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedBy;

    @Column(name = "token", nullable = false, length = 64)
    private String token;

    @Column(name = "code", nullable = false, length = 8)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    public static Invitation create(
        Store store,
        User invitedBy,
        String token,
        String code,
        OffsetDateTime expiresAt
    ) {
        Invitation invitation = new Invitation();
        invitation.store = store;
        invitation.invitedBy = invitedBy;
        invitation.token = token;
        invitation.code = code;
        invitation.status = InvitationStatus.ACTIVE;
        invitation.expiresAt = expiresAt;
        return invitation;
    }

    // 초대 갱신(재발급)
    public void renew(User invitedBy, String token, String code, OffsetDateTime expiresAt) {
        this.invitedBy = invitedBy;
        this.token = token;
        this.code = code;
        this.status = InvitationStatus.ACTIVE;
        this.expiresAt = expiresAt;
        this.revokedAt = null;
    }

    // 초대 취소
    public void revoke() {
        this.status = InvitationStatus.REVOKED;
        this.revokedAt = OffsetDateTime.now();
    }
}
