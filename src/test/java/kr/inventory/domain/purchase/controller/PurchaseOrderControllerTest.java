package kr.inventory.domain.purchase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderCreateRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderItemRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderUpdateRequest;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderDetailResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderSummaryResponse;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.purchase.service.PurchaseOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseOrderController.class)
@DisplayName("PurchaseOrderController 통합 테스트")
class PurchaseOrderControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private PurchaseOrderService purchaseOrderService;

        private final Long userId = 1L;
        private final UUID storePublicId = UUID.randomUUID();
        private final UUID vendorPublicId = UUID.randomUUID();
        private final UUID purchaseOrderPublicId = UUID.randomUUID();

        @Test
        @DisplayName("발주서 초안 생성 성공")
        void givenValidRequest_whenCreateDraft_thenReturnsOk() throws Exception {
                // given
                PurchaseOrderItemRequest item1 = new PurchaseOrderItemRequest("양파", 10, new BigDecimal("1000"));
                PurchaseOrderItemRequest item2 = new PurchaseOrderItemRequest("감자", 20, new BigDecimal("800"));
                PurchaseOrderCreateRequest request = new PurchaseOrderCreateRequest(vendorPublicId,
                                List.of(item1, item2));

                PurchaseOrderDetailResponse response = new PurchaseOrderDetailResponse(
                                purchaseOrderPublicId,
                                1L,
                                vendorPublicId,
                                "테스트 거래처",
                                null,
                                PurchaseOrderStatus.DRAFT,
                                new BigDecimal("26000"),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                Collections.emptyList());

                CustomUserDetails userDetails = mock(CustomUserDetails.class);
                given(userDetails.getUserId()).willReturn(userId);

                given(purchaseOrderService.createDraft(eq(userId), eq(storePublicId),
                                any(PurchaseOrderCreateRequest.class)))
                                .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/purchase-orders/{storePublicId}", storePublicId)
                                .with(csrf())
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.purchaseOrderPublicId").value(purchaseOrderPublicId.toString()))
                                .andExpect(jsonPath("$.status").value("DRAFT"))
                                .andExpect(jsonPath("$.totalAmount").value(26000))
                                .andExpect(jsonPath("$.vendorPublicId").value(vendorPublicId.toString()));
        }

        @Test
        @DisplayName("발주서 초안 생성 실패 - Validation 오류 (빈 항목 리스트)")
        void givenEmptyItems_whenCreateDraft_thenReturnsBadRequest() throws Exception {
                // given
                PurchaseOrderCreateRequest request = new PurchaseOrderCreateRequest(vendorPublicId,
                                Collections.emptyList());

                CustomUserDetails userDetails = mock(CustomUserDetails.class);
                given(userDetails.getUserId()).willReturn(userId);

                // when & then
                mockMvc.perform(post("/api/purchase-orders/{storePublicId}", storePublicId)
                                .with(csrf())
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("발주서 목록 조회 성공")
        void givenValidUser_whenGetList_thenReturnsOk() throws Exception {
                // given
                PurchaseOrderSummaryResponse summary1 = new PurchaseOrderSummaryResponse(
                                purchaseOrderPublicId,
                                1L,
                                vendorPublicId,
                                "테스트 거래처",
                                "PO-20250303-001",
                                PurchaseOrderStatus.SUBMITTED,
                                new BigDecimal("26000"),
                                OffsetDateTime.now(ZoneOffset.UTC));

                List<PurchaseOrderSummaryResponse> responses = List.of(summary1);

                CustomUserDetails userDetails = mock(CustomUserDetails.class);
                given(userDetails.getUserId()).willReturn(userId);

                given(purchaseOrderService.getPurchaseOrders(userId, storePublicId))
                                .willReturn(responses);

                // when & then
                mockMvc.perform(get("/api/purchase-orders/{storePublicId}", storePublicId)
                                .with(csrf())
                                .with(user(userDetails)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].purchaseOrderPublicId")
                                                .value(purchaseOrderPublicId.toString()))
                                .andExpect(jsonPath("$[0].status").value("SUBMITTED"))
                                .andExpect(jsonPath("$[0].totalAmount").value(26000));
        }

        @Test
        @DisplayName("발주서 상세 조회 성공")
        void givenValidPurchaseOrderId_whenGetDetail_thenReturnsOk() throws Exception {
                // given
                PurchaseOrderDetailResponse response = new PurchaseOrderDetailResponse(
                                purchaseOrderPublicId,
                                1L,
                                vendorPublicId,
                                "테스트 거래처",
                                "PO-20250303-001",
                                PurchaseOrderStatus.SUBMITTED,
                                new BigDecimal("26000"),
                                userId,
                                OffsetDateTime.now(ZoneOffset.UTC),
                                null,
                                null,
                                null,
                                null,
                                Collections.emptyList());

                CustomUserDetails userDetails = mock(CustomUserDetails.class);
                given(userDetails.getUserId()).willReturn(userId);

                given(purchaseOrderService.getPurchaseOrder(userId, storePublicId, purchaseOrderPublicId))
                                .willReturn(response);

                // when & then
                mockMvc.perform(get("/api/purchase-orders/{storePublicId}/{purchaseOrderPublicId}",
                                storePublicId, purchaseOrderPublicId)
                                .with(csrf())
                                .with(user(userDetails)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.purchaseOrderPublicId").value(purchaseOrderPublicId.toString()))
                                .andExpect(jsonPath("$.orderNo").value("PO-20250303-001"))
                                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        }

        @Test
        @DisplayName("발주서 초안 수정 성공")
        void givenValidRequest_whenUpdate_thenReturnsOk() throws Exception {
                // given
                PurchaseOrderItemRequest item = new PurchaseOrderItemRequest("당근", 15, new BigDecimal("1200"));
                PurchaseOrderUpdateRequest request = new PurchaseOrderUpdateRequest(vendorPublicId, List.of(item));

                PurchaseOrderDetailResponse response = new PurchaseOrderDetailResponse(
                                purchaseOrderPublicId,
                                1L,
                                vendorPublicId,
                                "테스트 거래처",
                                null,
                                PurchaseOrderStatus.DRAFT,
                                new BigDecimal("18000"),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                Collections.emptyList());

                CustomUserDetails userDetails = mock(CustomUserDetails.class);
                given(userDetails.getUserId()).willReturn(userId);

                given(purchaseOrderService.updateDraft(eq(userId), eq(storePublicId), eq(purchaseOrderPublicId),
                                any(PurchaseOrderUpdateRequest.class)))
                                .willReturn(response);

                // when & then
                mockMvc.perform(put("/api/purchase-orders/{storePublicId}/{purchaseOrderPublicId}",
                                storePublicId, purchaseOrderPublicId)
                                .with(csrf())
                                .with(user(userDetails))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalAmount").value(18000));
        }

        @Test
        @DisplayName("발주서 제출 성공")
        void givenDraftOrder_whenSubmit_thenReturnsOk() throws Exception {
                // given
                PurchaseOrderDetailResponse response = new PurchaseOrderDetailResponse(
                                purchaseOrderPublicId,
                                1L,
                                vendorPublicId,
                                "테스트 거래처",
                                "PO-20250303-001",
                                PurchaseOrderStatus.SUBMITTED,
                                new BigDecimal("26000"),
                                userId,
                                OffsetDateTime.now(ZoneOffset.UTC),
                                null,
                                null,
                                null,
                                null,
                                Collections.emptyList());

                CustomUserDetails userDetails = mock(CustomUserDetails.class);
                given(userDetails.getUserId()).willReturn(userId);

                given(purchaseOrderService.submit(userId, storePublicId, purchaseOrderPublicId))
                                .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/purchase-orders/{storePublicId}/{purchaseOrderPublicId}/submit",
                                storePublicId, purchaseOrderPublicId)
                                .with(csrf())
                                .with(user(userDetails)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                                .andExpect(jsonPath("$.orderNo").value("PO-20250303-001"))
                                .andExpect(jsonPath("$.submittedAt").exists());
        }

        @Test
        @DisplayName("발주서 확정 성공")
        void givenSubmittedOrder_whenConfirm_thenReturnsOk() throws Exception {
                // given
                PurchaseOrderDetailResponse response = new PurchaseOrderDetailResponse(
                                purchaseOrderPublicId,
                                1L,
                                vendorPublicId,
                                "테스트 거래처",
                                "PO-20250303-001",
                                PurchaseOrderStatus.CONFIRMED,
                                new BigDecimal("26000"),
                                userId,
                                OffsetDateTime.now(ZoneOffset.UTC),
                                userId,
                                OffsetDateTime.now(ZoneOffset.UTC),
                                null,
                                null,
                                Collections.emptyList());

                CustomUserDetails userDetails = mock(CustomUserDetails.class);
                given(userDetails.getUserId()).willReturn(userId);

                given(purchaseOrderService.confirm(userId, storePublicId, purchaseOrderPublicId))
                                .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/purchase-orders/{storePublicId}/{purchaseOrderPublicId}/confirm",
                                storePublicId, purchaseOrderPublicId)
                                .with(csrf())
                                .with(user(userDetails)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                                .andExpect(jsonPath("$.confirmedAt").exists());
        }

        @Test
        @DisplayName("발주서 취소 성공")
        void givenCancelableOrder_whenCancel_thenReturnsOk() throws Exception {
                // given
                PurchaseOrderDetailResponse response = new PurchaseOrderDetailResponse(
                                purchaseOrderPublicId,
                                1L,
                                vendorPublicId,
                                "테스트 거래처",
                                "PO-20250303-001",
                                PurchaseOrderStatus.CANCELED,
                                new BigDecimal("26000"),
                                userId,
                                OffsetDateTime.now(ZoneOffset.UTC),
                                null,
                                null,
                                userId,
                                OffsetDateTime.now(ZoneOffset.UTC),
                                Collections.emptyList());

                CustomUserDetails userDetails = mock(CustomUserDetails.class);
                given(userDetails.getUserId()).willReturn(userId);

                given(purchaseOrderService.cancel(userId, storePublicId, purchaseOrderPublicId))
                                .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/purchase-orders/{storePublicId}/{purchaseOrderPublicId}/cancel",
                                storePublicId, purchaseOrderPublicId)
                                .with(csrf())
                                .with(user(userDetails)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CANCELED"))
                                .andExpect(jsonPath("$.canceledAt").exists());
        }

        @Test
        @DisplayName("PDF 다운로드 성공")
        void givenValidPurchaseOrder_whenDownloadPdf_thenReturnsPdf() throws Exception {
                // given
                byte[] pdfBytes = "PDF_CONTENT".getBytes();

                CustomUserDetails userDetails = mock(CustomUserDetails.class);
                given(userDetails.getUserId()).willReturn(userId);

                given(purchaseOrderService.downloadPdf(userId, storePublicId, purchaseOrderPublicId))
                                .willReturn(pdfBytes);

                // when & then
                mockMvc.perform(get("/api/purchase-orders/{storePublicId}/{purchaseOrderPublicId}/pdf",
                                storePublicId, purchaseOrderPublicId)
                                .with(csrf())
                                .with(user(userDetails)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                                .andExpect(header().exists("Content-Disposition"))
                                .andExpect(content().bytes(pdfBytes));
        }
}