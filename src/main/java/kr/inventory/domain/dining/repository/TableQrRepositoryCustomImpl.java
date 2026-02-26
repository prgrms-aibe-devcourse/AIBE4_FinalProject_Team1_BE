package kr.inventory.domain.dining.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kr.inventory.domain.dining.entity.TableQr;
import kr.inventory.domain.dining.entity.enums.TableQrStatus;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class TableQrRepositoryCustomImpl implements TableQrRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<TableQr> findActiveQrByTableIdWithLock(Long tableId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(tableQr)
                        .where(
                                tableQr.table.tableId.eq(tableId),
                                tableQr.status.eq(TableQrStatus.ACTIVE)
                        )
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }
}
