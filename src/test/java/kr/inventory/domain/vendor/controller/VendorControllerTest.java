package kr.inventory.domain.vendor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.vendor.controller.dto.request.VendorCreateRequest;
import kr.inventory.domain.vendor.controller.dto.response.VendorResponse;
import kr.inventory.domain.vendor.controller.dto.request.VendorUpdateRequest;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.vendor.service.VendorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VendorController.class)
@DisplayName("VendorController 통합 테스트")
class VendorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VendorService vendorService;

    private final Long userId = 1L;
    private final UUID storePublicId = UUID.randomUUID();
    private final UUID vendorPublicId = UUID.randomUUID();

    @Test
    @DisplayName("거래처 등록 성공")
    void givenValidRequest_whenCreateVendor_thenReturnsCreated() throws Exception {
        // given
        VendorCreateRequest request = new VendorCreateRequest(
                "신선마트",
                "김철수",
                "010-1234-5678",
                "fresh@market.com",
                2
        );

        VendorResponse response = new VendorResponse(
                vendorPublicId,
                "신선마트",
                "김철수",
                "010-1234-5678",
                "fresh@market.com",
                2,
                VendorStatus.ACTIVE,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getUserId()).willReturn(userId);

        given(vendorService.createVendor(eq(userId), eq(storePublicId), any(VendorCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/vendors/{storePublicId}", storePublicId)
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("신선마트"))
                .andExpect(jsonPath("$.contactPerson").value("김철수"))
                .andExpect(jsonPath("$.phone").value("010-1234-5678"))
                .andExpect(jsonPath("$.email").value("fresh@market.com"))
                .andExpect(jsonPath("$.leadTimeDays").value(2))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("거래처 등록 실패 - Validation 오류")
    void givenInvalidRequest_whenCreateVendor_thenReturnsBadRequest() throws Exception {
        // given
        VendorCreateRequest request = new VendorCreateRequest(
                "",  // 빈 문자열 (NotBlank 위반)
                "김철수",
                "010-1234-5678",
                "invalid-email",  // 이메일 형식 위반
                2
        );

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getUserId()).willReturn(userId);

        // when & then
        mockMvc.perform(post("/api/vendors/{storePublicId}", storePublicId)
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("매장별 거래처 목록 조회 성공")
    void givenVendorsExist_whenGetVendorsByStore_thenReturnsVendorList() throws Exception {
        // given
        VendorResponse vendor1 = new VendorResponse(UUID.randomUUID(), "농협마트", "이영희", "010-2222-2222", "v2@test.com", 2, VendorStatus.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now());
        VendorResponse vendor2 = new VendorResponse(UUID.randomUUID(), "신선마트", "김철수", "010-1111-1111", "v1@test.com", 1, VendorStatus.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now());

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getUserId()).willReturn(userId);

        given(vendorService.getVendorsByStore(userId, storePublicId, VendorStatus.ACTIVE))
                .willReturn(List.of(vendor1, vendor2));

        // when & then
        mockMvc.perform(get("/api/vendors/{storePublicId}", storePublicId)
                        .with(csrf())
                        .with(user(userDetails))
                        .param("status", "ACTIVE"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("농협마트"))
                .andExpect(jsonPath("$[1].name").value("신선마트"));
    }

    @Test
    @DisplayName("매장별 거래처 목록 조회 - ACTIVE 필터링")
    void givenMixedStatus_whenGetVendorsByStoreWithActiveFilter_thenReturnsActiveOnly() throws Exception {
        // given
        VendorResponse activeVendor = new VendorResponse(vendorPublicId, "신선마트", "김철수", "010-1111-1111", "v1@test.com", 1, VendorStatus.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now());

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getUserId()).willReturn(userId);

        given(vendorService.getVendorsByStore(userId, storePublicId, VendorStatus.ACTIVE))
                .willReturn(List.of(activeVendor));

        // when & then
        mockMvc.perform(get("/api/vendors/{storePublicId}", storePublicId)
                        .with(csrf())
                        .with(user(userDetails))
                        .param("status", "ACTIVE"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("신선마트"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("거래처 상세 조회 성공")
    void givenVendorExists_whenGetVendor_thenReturnsVendor() throws Exception {
        // given
        VendorResponse response = new VendorResponse(
                vendorPublicId,
                "신선마트",
                "김철수",
                "010-1234-5678",
                "fresh@market.com",
                2,
                VendorStatus.ACTIVE,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getUserId()).willReturn(userId);

        given(vendorService.getVendor(storePublicId, vendorPublicId, userId))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/vendors/{storePublicId}/{vendorPublicId}", storePublicId, vendorPublicId)
                        .with(csrf())
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("신선마트"))
                .andExpect(jsonPath("$.contactPerson").value("김철수"));
    }

    @Test
    @DisplayName("거래처 수정 성공")
    void givenValidRequest_whenUpdateVendor_thenReturnsUpdatedVendor() throws Exception {
        // given
        VendorUpdateRequest request = new VendorUpdateRequest(
                "박영수",
                "010-9999-9999",
                "updated@market.com",
                3
        );

        VendorResponse response = new VendorResponse(
                vendorPublicId,
                "신선마트",  // 거래처명은 변경 불가
                "박영수",
                "010-9999-9999",
                "updated@market.com",
                3,
                VendorStatus.ACTIVE,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getUserId()).willReturn(userId);

        given(vendorService.updateVendor(eq(storePublicId), eq(vendorPublicId), eq(userId), any(VendorUpdateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/vendors/{storePublicId}/{vendorPublicId}", storePublicId, vendorPublicId)
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactPerson").value("박영수"))
                .andExpect(jsonPath("$.phone").value("010-9999-9999"))
                .andExpect(jsonPath("$.email").value("updated@market.com"))
                .andExpect(jsonPath("$.leadTimeDays").value(3));
    }

    @Test
    @DisplayName("거래처 비활성화 성공")
    void givenVendorExists_whenDeactivateVendor_thenReturnsNoContent() throws Exception {
        // given
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getUserId()).willReturn(userId);

        doNothing().when(vendorService).deactivateVendor(storePublicId, vendorPublicId, userId);

        // when & then
        mockMvc.perform(delete("/api/vendors/{storePublicId}/{vendorPublicId}", storePublicId, vendorPublicId)
                        .with(csrf())
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}