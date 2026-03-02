package kr.inventory.domain.dining.service;

import kr.inventory.domain.dining.controller.dto.response.TableQrIssueResponse;
import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.entity.TableQr;
import kr.inventory.domain.dining.exception.TableErrorCode;
import kr.inventory.domain.dining.exception.TableException;
import kr.inventory.domain.dining.repository.DiningTableRepository;
import kr.inventory.domain.dining.repository.TableQrRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.global.config.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TableQrManagerFacade {

    private final DiningTableRepository diningTableRepository;
    private final TableQrRepository tableQrRepository;
    private final StoreAccessValidator storeAccessValidator;
    private final S3StorageService s3storageService;
    private final QrGenerator qrGenerator;

    @Value("${app.order.base-url:http://localhost:8080}")
    private String orderBaseUrl;

    public List<TableQrIssueResponse> issueTableQrs(Long userId, UUID storePublicId, List<UUID> tablePublicIds) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        List<DiningTable> tables = diningTableRepository.findAllByStore_StoreIdAndTablePublicIdIn(storeId, tablePublicIds);

        if (tables.size() != tablePublicIds.size()) {
            throw new TableException(TableErrorCode.SOME_TABLES_NOT_FOUND);
        }

        return tables.stream()
                .map(table -> processSingleTableQr(storePublicId, table))
                .toList();
    }

    private TableQrIssueResponse processSingleTableQr(UUID storePublicId, DiningTable table) {
        revokeExistingQrs(table.getTableId());
        int nextVersion = getNextVersion(table.getTableId());

        String entryToken = TokenSupport.newOpaqueToken();
        String entryTokenHash = TokenSupport.sha256Hex(entryToken);

        TableQr qr = TableQr.create(table, entryTokenHash, nextVersion);
        tableQrRepository.save(qr);

        String qrContent = String.format("%s/qr_menu_order.html?s=%s&t=%s&token=%s",
                orderBaseUrl, storePublicId, table.getTablePublicId(), entryToken);

        byte[] imageBytes = qrGenerator.generate(qrContent, 300, 300);
        String s3Path = String.format("public/qr/%s/tables/%s/qr_v%d.png",
                storePublicId, table.getTablePublicId(), nextVersion);

        String uploadedImageUrl = s3storageService.upload(imageBytes, s3Path, "image/png");
        qr.complete(uploadedImageUrl);

        return TableQrIssueResponse.from(qr);
    }

    private void revokeExistingQrs(Long tableId) {
        tableQrRepository.findActiveQrByTableIdWithLock(tableId)
                .ifPresent(latest -> latest.revoke(OffsetDateTime.now(ZoneOffset.UTC)));
    }

    private int getNextVersion(Long tableId) {
        return tableQrRepository.findTopByTable_TableIdOrderByRotationVersionDesc(tableId)
                .map(latest -> latest.getRotationVersion() + 1)
                .orElse(1);
    }
}