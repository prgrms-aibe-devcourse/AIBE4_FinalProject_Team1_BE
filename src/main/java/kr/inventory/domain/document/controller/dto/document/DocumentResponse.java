package kr.inventory.domain.document.controller.dto.document;

import java.time.OffsetDateTime;

import kr.inventory.domain.document.entity.Document;

public record DocumentResponse(
	Long documentId,
	String fileName,
	String presignedUrl,
	OffsetDateTime uploadedAt
) {
	public static DocumentResponse from(Document document, String presignedUrl) {
		return new DocumentResponse(
			document.getDocumentId(),
			document.getFileName(),
			presignedUrl,
			document.getUploadedAt()
		);
	}
}
