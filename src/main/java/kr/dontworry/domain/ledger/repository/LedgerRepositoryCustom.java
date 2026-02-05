package kr.dontworry.domain.ledger.repository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerRepositoryCustom {
    Optional<Long> findLedgerIdByPublicId(UUID publicId);
}
