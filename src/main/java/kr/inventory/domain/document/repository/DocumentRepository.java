package kr.inventory.domain.document.repository;

import kr.inventory.domain.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
