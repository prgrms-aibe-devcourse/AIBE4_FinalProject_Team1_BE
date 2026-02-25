package kr.inventory.domain.dining.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.dining.entity.enums.TableSessionStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "table_sessions",
        indexes = {
                @Index(name = "idx_table_sessions_table", columnList = "table_id"),
                @Index(name = "idx_table_sessions_token_hash", columnList = "session_token_hash"),
                @Index(name = "idx_table_sessions_expires_at", columnList = "expires_at"),
                @Index(name = "idx_table_sessions_public_id", columnList = "session_public_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TableSession extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tableSessionId;

    @Column(name = "session_public_id", nullable = false, updatable = false, unique = true)
    private UUID sessionPublicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_qr_id", nullable = false)
    private TableQr tableQr;

    @Column(name = "session_token_hash", nullable = false, length = 128)
    private String sessionTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableSessionStatus status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    public static TableSession create(
            DiningTable table,
            TableQr tableQr,
            String sessionTokenHash,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt
    ) {
        TableSession s = new TableSession();
        s.table = table;
        s.tableQr = tableQr;
        s.sessionTokenHash = sessionTokenHash;
        s.expiresAt = expiresAt;
        s.status = TableSessionStatus.ACTIVE;
        return s;
    }

    public void touch(OffsetDateTime now) {
        this.lastSeenAt = now;
    }

    public void revoke() {
        this.status = TableSessionStatus.REVOKED;
    }

    public void expireIfNeeded(OffsetDateTime now) {
        if (this.status == TableSessionStatus.ACTIVE && now.isAfter(this.expiresAt)) {
            this.status = TableSessionStatus.EXPIRED;
        }
    }
}
