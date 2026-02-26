package kr.inventory.domain.document.controller.dto.document;

import java.time.OffsetDateTime;

import kr.inventory.domain.document.entity.Document;

public record DocumentResponse(
	Long documentId,
	String fileName,
	String url,
	OffsetDateTime uploadedAt
) {
	public static DocumentResponse from(Document document, String url) {
		return new DocumentResponse(
			document.getDocumentId(),
			document.getFileName(),
			url,
			document.getUploadedAt()
		);
	}
}
