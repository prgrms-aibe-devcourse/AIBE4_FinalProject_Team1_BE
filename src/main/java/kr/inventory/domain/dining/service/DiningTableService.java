package kr.inventory.domain.dining.service;

import kr.inventory.domain.dining.controller.dto.request.DiningTableCreateRequest;
import kr.inventory.domain.dining.controller.dto.response.DiningTableResponse;
import kr.inventory.domain.dining.controller.dto.request.DiningTableUpdateRequest;
import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.exception.TableErrorCode;
import kr.inventory.domain.dining.exception.TableException;
import kr.inventory.domain.dining.repository.DiningTableRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiningTableService {

    private final DiningTableRepository diningTableRepository;
    private final StoreRepository storeRepository;
    private final StoreAccessValidator storeAccessValidator;

    @Transactional
    public UUID createTable(Long userId, UUID storePublicId, DiningTableCreateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

        DiningTable table = DiningTable.create(store, request.tableCode());
        diningTableRepository.save(table);
        return table.getTablePublicId();
    }

    public List<DiningTableResponse> getTables(Long userId, UUID storePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        return diningTableRepository.findAllByStore_StoreId(storeId).stream()
                .map(DiningTableResponse::from)
                .toList();
    }

    public DiningTableResponse getTable(Long userId, UUID storePublicId, UUID tablePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        DiningTable table = findTable(storeId, tablePublicId);
        return DiningTableResponse.from(table);
    }

    @Transactional
    public void updateTable(Long userId, UUID storePublicId, UUID tablePublicId, DiningTableUpdateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        DiningTable table = findTable(storeId, tablePublicId);
        table.update(request.tableCode(), request.status());
    }

    @Transactional
    public void deleteTable(Long userId, UUID storePublicId, UUID tablePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        DiningTable table = findTable(storeId, tablePublicId);
        diningTableRepository.delete(table);
    }

    private DiningTable findTable(Long storeId, UUID tablePublicId) {
        return diningTableRepository.findByStore_StoreIdAndTablePublicId(storeId, tablePublicId)
                .orElseThrow(() -> new TableException(TableErrorCode.TABLE_NOT_FOUND));
    }
}