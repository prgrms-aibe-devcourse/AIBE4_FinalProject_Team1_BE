package kr.inventory.domain.dining.repository;

import kr.inventory.domain.dining.entity.TableQr;

import java.util.Optional;

public interface TableQrRepositoryCustom {
    Optional<TableQr> findActiveQrByTableIdWithLock(Long tableId);
}
