package kr.inventory.domain.stock.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import kr.inventory.domain.stock.controller.dto.request.WasteSearchCondition;
import kr.inventory.domain.stock.entity.WasteRecord;

public interface WasteRecordRepositoryCustom {
	Page<WasteRecord> searchWasteRecords(Long storeId, WasteSearchCondition searchCondition, Pageable pageable);
}
