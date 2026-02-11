package kr.inventory.domain.document.service;

import kr.inventory.domain.document.controller.dto.ocr.OcrResultResponse;
import kr.inventory.domain.document.controller.dto.ocr.ReceiptData;
import kr.inventory.domain.document.repository.DocumentRepository;
import kr.inventory.domain.document.service.processor.OcrProcessor;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentOcrService {

	private final List<OcrProcessor> processors;
	private final DocumentRepository documentRepository;
	private final StoreRepository storeRepository;
	private final UserRepository userRepository;

	public OcrResultResponse processOcr(
		Long storeId,
		Long userId,
		List<MultipartFile> files) {
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> new IllegalArgumentException("Store not found"));
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("User not found"));

		List<ReceiptData> results = new ArrayList<>();

		for (MultipartFile file : files) {
			if (file.isEmpty())
				continue;

			// TODO: s3 연동 후 저장 기능 구현
			// // 1. Document 생성 및 저장
			// Document document = Document.create(
			// 	store,
			// 	DocumentType.RECEIPT, // 기본값, 필요 시 로직 추가
			// 	file.getOriginalFilename(),
			// 	file.getContentType(),
			// 	user
			// );
			// documentRepository.save(document);

			// 2. OCR 처리
			OcrProcessor processor = findProcessor(file);
			ReceiptData data;
			if (processor != null) {
				data = processor.process(file);
				log.info("OCR processing completed for file: {}",
					file.getOriginalFilename());
			} else {
				log.warn("지원하지 않는 파일 형식 입니다: {}", file.getOriginalFilename());
				data = ReceiptData.empty("데이터를 불러오는 중 오류가 발생했습니다.");
			}

			results.add(data);
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
