package kr.inventory.domain.stock.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.request.StockOrderDeductionRequest;
import kr.inventory.domain.stock.service.StockManagerFacade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
class StockControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private StockManagerFacade stockManagerFacade;

	private final UUID storePublicId = UUID.randomUUID();
	private final Long userId = 1L;

	@Test
	@WithMockUser
	@DisplayName("재고 차감 요청 성공 - 200 OK 반환")
	void deductStock_Success() throws Exception {
		Long storeId = 10L;
		Long salesOrderId = 100L;

		StockOrderDeductionRequest request = new StockOrderDeductionRequest(storeId, salesOrderId);

		CustomUserDetails userDetails = mock(CustomUserDetails.class);
		given(userDetails.getUserId()).willReturn(userId);

		doNothing().when(stockManagerFacade)
			.processOrderStockDeduction(eq(userId), eq(storePublicId), any());

		mockMvc.perform(post("/api/stock/{storePublicId}/deduct", storePublicId)
				.with(csrf())
				.with(user(userDetails))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.salesOrderId").value(salesOrderId))
			.andExpect(jsonPath("$.message").value("재고 차감 처리가 완료되었습니다."));
	}
}