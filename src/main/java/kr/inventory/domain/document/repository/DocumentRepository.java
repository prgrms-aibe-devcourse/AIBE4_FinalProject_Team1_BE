package kr.inventory.domain.document.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.inventory.domain.document.entity.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {
	List<Document> findAllByStore_StoreId(Long storeId);
}
