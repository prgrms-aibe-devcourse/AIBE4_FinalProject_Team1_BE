package kr.dontworry.domain.ledger.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

import static kr.dontworry.domain.ledger.entity.QLedger.ledger;

@RequiredArgsConstructor
public class LedgerRepositoryCustomImpl implements LedgerRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Long> findLedgerIdByPublicId(UUID publicId) {
        return Optional.ofNullable(
                queryFactory
                        .select(ledger.ledgerId)
                        .from(ledger)
                        .where(ledger.publicId.eq(publicId))
                        .fetchOne()
        );
    }
}
