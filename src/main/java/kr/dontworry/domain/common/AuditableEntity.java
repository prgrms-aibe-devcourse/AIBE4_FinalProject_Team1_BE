package kr.dontworry.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * created_at / updated_at 를 가진 테이블용
 * Postgres TIMESTAMPTZ 매핑하기 위해 OffsetDateTime 사용
 */
@Getter
@MappedSuperclass
public abstract class AuditableEntity {

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void prePersistAudit() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    protected void preUpdateAudit() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}