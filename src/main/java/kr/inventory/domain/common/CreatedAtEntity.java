package kr.inventory.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * created_at 만 가진 테이블용
 */
@Getter
@MappedSuperclass
public abstract class CreatedAtEntity {

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void prePersistCreatedAt() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
