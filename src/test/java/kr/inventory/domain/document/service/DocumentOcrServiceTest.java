package kr.inventory.domain.document.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import kr.inventory.domain.document.controller.dto.ocr.ReceiptResponse;
import kr.inventory.domain.document.service.processor.OcrProcessor;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;
import kr.inventory.global.config.infrastructure.S3StorageService;

@ExtendWith(MockitoExtension.class)
class DocumentOcrServiceTest {

	@InjectMocks
	private DocumentOcrService documentOcrService;

	@Mock
	private List<OcrProcessor> processors;

	@Mock
	private DocumentService documentService;

	@Mock
	private S3StorageService s3StorageService;

	@Mock
	private StoreRepository storeRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private OcrProcessor ocrProcessor;

	@Test
	@DisplayName("OCR 처리 및 파일 저장 테스트")
	void processOcr_Success() {
		// given
		Long storeId = 1L;
		UUID storePublicId = UUID.randomUUID();
		Long userId = 1L;
		MockMultipartFile file = new MockMultipartFile(
			"files",
			"test.jpg",
			"image/jpeg",
			"test content".getBytes()
		);
		List<MultipartFile> files = List.of(file);

		Store store = mock(Store.class);
		User user = mock(User.class);
		ReceiptResponse receiptData = ReceiptResponse.empty("test");

		given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
		// given(storeRepository.findByStorePublicId(storePublicId)).willReturn(Optional.of(store));
		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(processors.stream()).willReturn(List.of(ocrProcessor).stream());
		given(ocrProcessor.supports(any(MultipartFile.class))).willReturn(true);
		given(ocrProcessor.process(any(MultipartFile.class), anyLong())).willReturn(receiptData);
		given(s3StorageService.upload(any(MultipartFile.class), anyString())).willReturn("http://s3-url/test.jpg");

		// when
		documentOcrService.processOcr(storePublicId, userId, files);

		// then
		verify(s3StorageService, times(1)).upload(any(MultipartFile.class), anyString());
		verify(documentService, times(1)).saveDocument(eq(store), eq(file), anyString());
	}
}
