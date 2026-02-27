package kr.inventory.domain.dining.service;

import kr.inventory.domain.dining.controller.dto.request.TableSessionEnterRequest;
import kr.inventory.domain.dining.controller.dto.response.TableSessionEnterResponse;
import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.entity.TableQr;
import kr.inventory.domain.dining.entity.TableSession;
import kr.inventory.domain.dining.entity.enums.TableSessionStatus;
import kr.inventory.domain.dining.exception.QrErrorCode;
import kr.inventory.domain.dining.exception.QrException;
import kr.inventory.domain.dining.exception.TableErrorCode;
import kr.inventory.domain.dining.exception.TableException;
import kr.inventory.domain.dining.repository.DiningTableRepository;
import kr.inventory.domain.dining.repository.TableQrRepository;
import kr.inventory.domain.dining.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableSessionService {

    private final DiningTableRepository diningTableRepository;
    private final TableQrRepository tableQrRepository;
    private final TableSessionRepository tableSessionRepository;

    // 세션 TTL (예: 2시간)
    @Value("${app.table-session.ttl-minutes:60}")
    private long sessionTtlMinutes;

    @Transactional
    public TableSessionEnterResponse enter(TableSessionEnterRequest request, String entryToken) {
        validateEntryToken(entryToken);

        DiningTable table = getDiningTable(request.storePublicId(), request.tablePublicId());
        TableQr activeQr = getActiveQr(table.getTableId());

        verifyTokenMatch(entryToken, activeQr.getEntryTokenHash());

        revokeExistingSessions(table.getTableId());

        return createAndSaveNewSession(table, activeQr);
    }

    private void validateEntryToken(String entryToken) {
        if (entryToken == null || entryToken.isBlank()) {
            throw new TableException(TableErrorCode.INVALID_QR_TOKEN);
        }
    }

    private DiningTable getDiningTable(UUID storePublicId, UUID tablePublicId) {
        return diningTableRepository
                .findByStore_StorePublicIdAndTablePublicId(storePublicId, tablePublicId)
                .orElseThrow(() -> new TableException(TableErrorCode.TABLE_NOT_FOUND));
    }

    private TableQr getActiveQr(Long tableId) {
        return tableQrRepository
                .findActiveQrByTable_TableId(tableId)
                .orElseThrow(() -> new QrException(QrErrorCode.QR_NOT_ACTIVE));
    }

    private void verifyTokenMatch(String entryToken, String storedHash) {
        String entryHash = TokenSupport.sha256Hex(entryToken);
        if (!entryHash.equals(storedHash)) {
            throw new TableException(TableErrorCode.INVALID_QR_TOKEN);
        }
    }

    private void revokeExistingSessions(Long tableId) {
        tableSessionRepository.findAllByTable_TableIdAndStatus(tableId, TableSessionStatus.ACTIVE)
                .forEach(TableSession::revoke);
    }

    private TableSessionEnterResponse createAndSaveNewSession(DiningTable table, TableQr activeQr) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiresAt = now.plusMinutes(sessionTtlMinutes);

        // Opaque Token 생성 및 해싱
        String sessionToken = TokenSupport.newOpaqueToken();
        String sessionTokenHash = TokenSupport.sha256Hex(sessionToken);

        TableSession session = TableSession.create(
                table,
                activeQr,
                sessionTokenHash,
                now,
                expiresAt
        );
        session.touch(now);

        tableSessionRepository.save(session);

        return TableSessionEnterResponse.from(
                session.getSessionPublicId(),
                sessionToken,
                expiresAt
        );
    }
}