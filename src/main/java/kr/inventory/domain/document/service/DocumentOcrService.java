package kr.inventory.domain.document.service;

import kr.inventory.domain.document.controller.dto.ocr.OcrResultResponse;
import kr.inventory.domain.document.controller.dto.ocr.ReceiptResponse;
import kr.inventory.domain.document.service.processor.OcrProcessor;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.user.repository.UserRepository;
import kr.inventory.global.config.infrastructure.S3StorageService;
import kr.inventory.global.config.infrastructure.exception.FileError;
import kr.inventory.global.config.infrastructure.exception.FileException;
import kr.inventory.global.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentOcrService {

	private final List<OcrProcessor> processors;
	private final DocumentService documentService;
	private final S3StorageService s3StorageService;
	private final StoreAccessValidator storeAccessValidator;
	private final StoreRepository storeRepository;
	private final UserRepository userRepository;

	public OcrResultResponse processOcr(
		UUID storeId,
		Long userId,
		List<MultipartFile> files) {
		Store store = storeRepository.findById(storeAccessValidator.validateAndGetStoreId(userId, storeId))
			.orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));

		List<ReceiptResponse> results = new ArrayList<>();

		for (MultipartFile file : files) {
			if (file.isEmpty())
				continue;

			OcrProcessor processor = findProcessor(file);
			if (processor == null) {
				log.warn("지원하지 않는 파일 형식 입니다: {}", file.getOriginalFilename());
				throw new FileException(FileError.INVALID_FILE_FORMAT);
			}

			try {
				ReceiptResponse data = processor.process(file);
				log.info("OCR processing completed for file: {}",
					file.getOriginalFilename());

				String filePath = s3StorageService.upload(file, "document");
				documentService.saveDocument(store, file, filePath);
				results.add(data);
			} catch (Exception e) {
				log.error("파일 처리 프로세스 중 오류 발생: {}", file.getOriginalFilename(), e);
				throw new FileException(FileError.STORAGE_UPLOAD_FAILURE);
			}

		}

		log.info("All OCR processing completed. Total files processed: {}", results.size());

		return new OcrResultResponse(results);
	}

	private OcrProcessor findProcessor(MultipartFile file) {
		return processors.stream()
			.filter(p -> p.supports(file))
			.findFirst()
			.orElse(null);
	}
}
