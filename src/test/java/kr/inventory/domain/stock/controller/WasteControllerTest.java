package kr.inventory.domain.stock.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.stock.controller.dto.request.WasteRequest;
import kr.inventory.domain.stock.controller.dto.request.WasteSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.WasteResponse;
import kr.inventory.domain.stock.entity.enums.WasteReason;
import kr.inventory.domain.stock.service.WasteService;

@WebMvcTest(WasteController.class)
class WasteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private WasteService wasteService;

	private CustomUserDetails customUserDetails;

	@BeforeEach
	void setUp() {
		Long userId = 1L;
		Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
		customUserDetails = new CustomUserDetails(userId, authorities);
	}

	@Test
	@DisplayName("폐기 등록 성공")
	void recordWaste_Success() throws Exception {
		// given
		UUID storeId = UUID.randomUUID();
		WasteRequest request = new WasteRequest(List.of(
			new WasteRequest.WasteItem(
				UUID.randomUUID(),
				BigDecimal.TEN,
				WasteReason.EXPIRED,
				OffsetDateTime.now()
			)
		));

		// when & then
		mockMvc.perform(post("/api/disposal/{storePublicId}", storeId)
				.with(user(customUserDetails))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("폐기 목록 조회 성공")
	void getWasteRecords_Success() throws Exception {
		// given
		UUID storeId = UUID.randomUUID();
		Page<WasteResponse> responsePage = new PageImpl<>(List.of());

		given(wasteService.getWasteRecords(any(), eq(storeId), any(WasteSearchRequest.class), any(Pageable.class)))
			.willReturn(responsePage);

		// when & then
		mockMvc.perform(get("/api/disposal/{storePublicId}", storeId)
				.with(user(customUserDetails))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk());
	}
}
