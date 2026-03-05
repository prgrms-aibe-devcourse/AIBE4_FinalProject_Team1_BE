package kr.inventory.domain.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.inventory.domain.stock.entity.WasteRecord;

public interface WasteRecordRepository extends JpaRepository<WasteRecord, Long>, WasteRecordRepositoryCustom {

}
