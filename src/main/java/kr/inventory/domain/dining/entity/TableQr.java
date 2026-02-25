package kr.inventory.domain.dining.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.dining.entity.enums.TableQrStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "table_qrs",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"table_id", "rotation_version"})
        },
        indexes = {
                @Index(name = "idx_table_qrs_table", columnList = "table_id"),
                @Index(name = "idx_table_qrs_token_hash", columnList = "entry_token_hash"),
                @Index(name = "idx_table_qrs_public_id", columnList = "qr_public_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TableQr extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tableQrId;

    @Column(name = "qr_public_id", nullable = false, updatable = false, unique = true)
    private UUID qrPublicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @Column(name = "entry_token_hash", nullable = false, length = 128)
    private String entryTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableQrStatus status;

    @Column(name = "rotation_version", nullable = false)
    private Integer rotationVersion;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    public static TableQr create(DiningTable table, String entryTokenHash, int rotationVersion) {
        TableQr qr = new TableQr();
        qr.table = table;
        qr.entryTokenHash = entryTokenHash;
        qr.rotationVersion = rotationVersion;
        qr.status = TableQrStatus.ACTIVE;
        return qr;
    }

    public void revoke(OffsetDateTime revokedAt) {
        if (this.status == TableQrStatus.REVOKED) return;
        this.status = TableQrStatus.REVOKED;
        this.revokedAt = revokedAt;
    }

    public boolean isActive() {
        return this.status == TableQrStatus.ACTIVE;
    }
}