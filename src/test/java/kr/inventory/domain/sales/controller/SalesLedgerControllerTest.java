package kr.inventory.domain.sales.controller;

import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderDetailResponse;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderSummaryResponse;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
import kr.inventory.domain.sales.service.SalesLedgerService;
import kr.inventory.global.dto.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalesLedgerController.class)
@DisplayName("매출 내역 컨트롤러 테스트")
class SalesLedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalesLedgerService salesLedgerService;

    @Test
    @WithMockUser
    @DisplayName("매출 내역 목록 조회 API - 성공")
    void givenSearchRequest_whenGetSalesLedgerOrders_thenReturn200() throws Exception {
        Long userId = 1L;
        UUID storePublicId = UUID.randomUUID();
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getUserId()).willReturn(userId);

        SalesLedgerOrderSummaryResponse content = new SalesLedgerOrderSummaryResponse(
                UUID.randomUUID(),
                SalesOrderStatus.COMPLETED,
                SalesOrderType.DINE_IN,
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                "T1",
                2,
                new BigDecimal("23000"),
                BigDecimal.ZERO,
                new BigDecimal("23000")
        );
        PageResponse<SalesLedgerOrderSummaryResponse> response = PageResponse.from(
                new PageImpl<>(List.of(content), PageRequest.of(0, 20), 1)
        );

        given(salesLedgerService.getSalesLedgerOrders(eq(userId), eq(storePublicId), any(), any())).willReturn(response);

        mockMvc.perform(get("/api/sales/{storePublicId}/orders", storePublicId)
                        .with(csrf())
                        .with(user(userDetails))
                        .queryParam("from", OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).toString())
                        .queryParam("to", OffsetDateTime.now(ZoneOffset.UTC).toString())
                        .queryParam("status", "COMPLETED")
                        .queryParam("type", "DINE_IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].itemCount").value(2));
    }

    @Test
    @WithMockUser
    @DisplayName("매출 내역 상세 조회 API - 성공")
    void givenOrderPublicId_whenGetSalesLedgerOrder_thenReturn200() throws Exception {
        Long userId = 1L;
        UUID storePublicId = UUID.randomUUID();
        UUID orderPublicId = UUID.randomUUID();
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getUserId()).willReturn(userId);

        SalesLedgerOrderDetailResponse response = new SalesLedgerOrderDetailResponse(
                orderPublicId,
                SalesOrderStatus.COMPLETED,
                SalesOrderType.DINE_IN,
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                "T1",
                0,
                new BigDecimal("23000"),
                BigDecimal.ZERO,
                new BigDecimal("23000"),
                List.of()
        );

        given(salesLedgerService.getSalesLedgerOrder(userId, storePublicId, orderPublicId)).willReturn(response);

        mockMvc.perform(get("/api/sales/{storePublicId}/orders/{orderPublicId}", storePublicId, orderPublicId)
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderPublicId").value(orderPublicId.toString()))
                .andExpect(jsonPath("$.netAmount").value(23000));
    }
}
