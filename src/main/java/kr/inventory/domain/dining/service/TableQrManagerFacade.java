package kr.inventory.domain.dining.service;

import kr.inventory.domain.dining.controller.dto.TableQrIssueResponse;
import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.entity.TableQr;
import kr.inventory.domain.dining.entity.enums.TableQrStatus;
import kr.inventory.domain.dining.exception.QrErrorCode;
import kr.inventory.domain.dining.exception.QrException;
import kr.inventory.domain.dining.exception.TableErrorCode;
import kr.inventory.domain.dining.exception.TableException;
import kr.inventory.domain.dining.repository.DiningTableRepository;
import kr.inventory.domain.dining.repository.TableQrRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TableQrManagerFacade {

    private final DiningTableRepository diningTableRepository;
    private final TableQrRepository tableQrRepository;
    private final StoreAccessValidator storeAccessValidator;

    @Value("${app.order.base-url:http:localhost.index.html}")
    private String orderBaseUrl;

    public TableQrIssueResponse issueOrRotate(Long userId, UUID storePublicId, UUID tablePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        DiningTable table = diningTableRepository.findByStore_StoreIdAndTablePublicId(storeId, tablePublicId)
                .orElseThrow(() -> new TableException(TableErrorCode.TABLE_NOT_FOUND));

        TableQr latest = tableQrRepository.findTopByTable_TableIdOrderByRotationVersionDesc(table.getTableId())
                .orElse(null);

        int nextVersion = 1;
        if (latest != null) {
            nextVersion = latest.getRotationVersion() + 1;
            if (latest.getStatus() == TableQrStatus.ACTIVE) {
                latest.revoke(OffsetDateTime.now(ZoneOffset.UTC));
            }
        }

        String entryToken = TokenSupport.newOpaqueToken();
        String entryTokenHash = TokenSupport.sha256Hex(entryToken);

        TableQr qr = TableQr.create(table, entryTokenHash, nextVersion);
        tableQrRepository.save(qr);

        String qrUrl = orderBaseUrl + "/t/" + entryToken;

        return TableQrIssueResponse.from(
                qr.getQrPublicId(),
                table.getTablePublicId(),
                table.getTableCode(),
                qr.getRotationVersion(),
                entryToken,
                qrUrl
        );
    }

    public void revoke(Long userId, UUID storePublicId, UUID qrPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        TableQr qr = tableQrRepository.findByStore_StoreIdAndQrPublicId(storeId, qrPublicId)
                .orElseThrow(() -> new QrException(QrErrorCode.QR_NOT_FOUND));

        qr.revoke(OffsetDateTime.now(ZoneOffset.UTC));
    }
}