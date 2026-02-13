package kr.inventory.domain.document.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kr.inventory.domain.document.entity.Document;
import kr.inventory.domain.document.exception.DocumentError;
import kr.inventory.domain.document.exception.DocumentException;
import kr.inventory.domain.document.repository.DocumentRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.user.entity.User;
import kr.inventory.global.config.infrastructure.S3StorageService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocumentService {
	private final DocumentRepository documentRepository;
	private final S3StorageService s3StorageService;

	@Transactional
	public void saveDocument(Store store, User user, MultipartFile file, String filePath) {
		Document document = Document.create(
			store,
			file.getOriginalFilename(),
			filePath,
			file.getContentType(),
			user
		);

		documentRepository.save(document);
	}

	@Transactional(readOnly = true)
	public String getDocumentFileUrl(Long documentId, Long userId) {
		Document document = documentRepository.findById(documentId)
			.orElseThrow(() -> new DocumentException(DocumentError.DOCUMENT_NOT_FOUND));

		if (!document.getUploadedByUser().getUserId().equals(userId)) {
			throw new DocumentException(DocumentError.DOCUMENT_ACCESS_DENIED);
		}

		return s3StorageService.getPresignedUrl(document.getFilePath());
	}
}
