package kr.inventory.domain.stock.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import kr.inventory.domain.stock.controller.dto.request.WasteSearchRequest;
import kr.inventory.domain.stock.entity.WasteRecord;

public interface WasteRecordRepositoryCustom {
	Page<WasteRecord> searchWasteRecords(Long storeId, WasteSearchRequest searchCondition, Pageable pageable);
}
