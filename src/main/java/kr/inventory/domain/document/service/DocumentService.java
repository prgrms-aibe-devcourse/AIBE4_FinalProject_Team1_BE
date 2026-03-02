package kr.inventory.domain.document.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kr.inventory.domain.document.controller.dto.document.DocumentResponse;
import kr.inventory.domain.document.entity.Document;
import kr.inventory.domain.document.repository.DocumentRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.global.config.S3StorageService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocumentService {
	private final DocumentRepository documentRepository;
	private final S3StorageService s3StorageService;
	private final StoreAccessValidator storeAccessValidator;

	@Transactional
	public void saveDocument(Store store, MultipartFile file, String filePath) {
		Document document = Document.create(
			store,
			file.getOriginalFilename(),
			filePath,
			file.getContentType()
		);

		documentRepository.save(document);
	}

	@Transactional(readOnly = true)
	public List<DocumentResponse> getDocuments(UUID storePublicId, Long userId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		List<Document> documents = documentRepository.findAllByStore_StoreId(storeId);

		return documents.stream()
			.map(document -> DocumentResponse.from(
				document,
				s3StorageService.getPresignedUrl(document.getFilePath())
			))
			.collect(Collectors.toList());
	}
}
