package kr.inventory.domain.document.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.document.controller.dto.ocr.OcrResultResponse;
import kr.inventory.domain.document.controller.dto.ocr.ReceiptResponse;
import kr.inventory.domain.document.service.DocumentOcrService;
import kr.inventory.domain.document.service.DocumentService;

@WebMvcTest(DocumentOcrController.class)
class DocumentOcrControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DocumentOcrService documentOcrService;

	@MockitoBean
	private DocumentService documentService;

	@Test
	@DisplayName("OCR 파일 업로드 테스트")
	@WithMockUser
	void uploadOcrFile_Success() throws Exception {
		// given
		MockMultipartFile file = new MockMultipartFile(
			"files",
			"test.jpg",
			MediaType.IMAGE_JPEG_VALUE,
			"test content".getBytes()
		);

		CustomUserDetails userDetails = new CustomUserDetails(1L,
			Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

		OcrResultResponse response = new OcrResultResponse(List.of(ReceiptResponse.empty("test")));

		given(documentOcrService.processOcr(any(), anyLong(), anyList()))
			.willReturn(response);

		// when & then
		mockMvc.perform(multipart("/api/documents/ocr")
				.file(file)
				.param("storeId", "1")
				.with(SecurityMockMvcRequestPostProcessors.user(userDetails))
				.with(csrf())
				.contentType(MediaType.MULTIPART_FORM_DATA))
			.andExpect(status().isOk());
	}
}
