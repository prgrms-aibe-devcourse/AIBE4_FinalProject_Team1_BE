package kr.inventory.domain.dining.service;

import kr.inventory.domain.dining.controller.dto.response.TableQrResponse;
import kr.inventory.domain.dining.entity.enums.TableQrStatus;
import kr.inventory.domain.dining.repository.TableQrRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TableQrService {

    private final TableQrRepository tableQrRepository;
    private final StoreAccessValidator storeAccessValidator;

    public List<TableQrResponse> getTableQrs(Long userId, UUID storePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        return tableQrRepository.findAllByTable_Store_StoreIdAndStatus(storeId, TableQrStatus.ACTIVE).stream()
                .map(TableQrResponse::from)
                .toList();
    }
}