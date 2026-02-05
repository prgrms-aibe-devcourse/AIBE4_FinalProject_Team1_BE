package kr.dontworry.domain.ledger.service;

import kr.dontworry.domain.ledger.exception.LedgerErrorCode;
import kr.dontworry.domain.ledger.exception.LedgerException;
import kr.dontworry.domain.ledger.repository.LedgerRepository;
import kr.dontworry.global.common.PublicIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerReadService implements PublicIdResolver {
    private final LedgerRepository ledgerRepository;

    @Override
    public Long resolveInternalId(UUID publicId) {
        return ledgerRepository.findLedgerIdByPublicId(publicId)
                .orElseThrow(() -> new LedgerException(LedgerErrorCode.LEDGER_NOT_FOUND));
    }
}