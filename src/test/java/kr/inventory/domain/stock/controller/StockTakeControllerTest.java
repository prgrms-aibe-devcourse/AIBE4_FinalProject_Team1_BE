package kr.inventory.domain.stock.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.request.StockTakeCreateRequest;
import kr.inventory.domain.stock.controller.dto.request.StockTakeItemRequest;
import kr.inventory.domain.stock.service.StockTakeService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockTakeController.class)
class StockTakeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private StockTakeService stockTakeService;

	private final UUID storePublicId = UUID.randomUUID();
	private final Long userId = 1L;

	@Test
	@DisplayName("새로운 실사 시트를 성공적으로 생성한다.")
	void createSheet_Success() throws Exception {
		// given
		StockTakeItemRequest item = new StockTakeItemRequest(10L, new BigDecimal("50.0"));
		StockTakeCreateRequest request = new StockTakeCreateRequest("2026-02-14 정기 실사", List.of(item));

		CustomUserDetails userDetails = createMockUser();
		given(stockTakeService.createStockTakeSheet(eq(userId), eq(storePublicId), any(StockTakeCreateRequest.class)))
			.willReturn(100L);

		// when & then
		mockMvc.perform(post("/api/stocktakes/{storePublicId}", storePublicId)
				.with(user(userDetails))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON_VALUE)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(content().string("100"));
	}

	private CustomUserDetails createMockUser() {
		return new CustomUserDetails(
			userId,
			List.of(new SimpleGrantedAuthority("ROLE_OWNER")),
			Map.of()
		);
	}
}