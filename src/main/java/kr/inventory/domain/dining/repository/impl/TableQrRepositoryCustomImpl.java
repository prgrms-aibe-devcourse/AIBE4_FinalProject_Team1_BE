package kr.inventory.domain.dining.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kr.inventory.domain.dining.entity.TableQr;
import kr.inventory.domain.dining.entity.enums.TableQrStatus;
import kr.inventory.domain.dining.repository.TableQrRepositoryCustom;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static kr.inventory.domain.dining.entity.QTableQr.tableQr;

@RequiredArgsConstructor
public class TableQrRepositoryCustomImpl implements TableQrRepositoryCustom {
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

    @Override
    public Optional<TableQr> findActiveQrByTable_TableId(Long tableId) {
        TableQr result = queryFactory
                .selectFrom(tableQr)
                .where(
                        tableQr.table.tableId.eq(tableId),
                        tableQr.status.eq(TableQrStatus.ACTIVE)
                )
                .orderBy(tableQr.rotationVersion.desc())
                .fetchFirst();

        return Optional.ofNullable(result);
    }
}
