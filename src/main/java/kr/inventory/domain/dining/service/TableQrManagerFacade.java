package kr.inventory.domain.dining.service;

import kr.inventory.domain.dining.controller.dto.response.TableQrIssueResponse;
import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.entity.TableQr;
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

    private final QrGenerator qrGenerator;
    private final

    @Value("${app.order.base-url:http:localhost.index.html}")
    private String orderBaseUrl;

    public TableQrIssueResponse issueTableQr(Long userId, UUID storePublicId, UUID tablePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        DiningTable table = diningTableRepository.findByStore_StoreIdAndTablePublicId(storeId, tablePublicId)
                .orElseThrow(() -> new TableException(TableErrorCode.TABLE_NOT_FOUND));

        revokeExistingQrs(table.getTableId());

        String entryToken = TokenSupport.newOpaqueToken();
        String entryTokenHash = TokenSupport.sha256Hex(entryToken);
        int nextVersion = getNextVersion(table.getTableId());

        TableQr qr = TableQr.create(table, entryTokenHash, nextVersion);
        tableQrRepository.save(qr);

        String qrUrl = orderBaseUrl + "/t/" + entryToken;
        byte[] imageBytes = qrGenerator.generate(qrUrl, 300, 300);
        String uploadedImageUrl = storageService.uploadQrImage(qr.getQrPublicId(), imageBytes);

        qr.complete(uploadedImageUrl);

        return TableQrIssueResponse.from(qr);
    }

    private void revokeExistingQrs(Long tableId) {
        tableQrRepository.findActiveQrByTableId(tableId)
                .ifPresent(latest -> latest.revoke(OffsetDateTime.now(ZoneOffset.UTC)));
    }

    private int getNextVersion(Long tableId) {
        return tableQrRepository.findTopByTable_TableIdOrderByRotationVersionDesc(tableId)
                .map(latest -> latest.getRotationVersion() + 1)
                .orElse(1);
    }

    public void revoke(Long userId, UUID storePublicId, UUID qrPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        TableQr qr = tableQrRepository.findByTable_Store_StoreIdAndQrPublicId(storeId, qrPublicId)
                .orElseThrow(() -> new QrException(QrErrorCode.QR_NOT_FOUND));

        qr.revoke(OffsetDateTime.now(ZoneOffset.UTC));
    }
}