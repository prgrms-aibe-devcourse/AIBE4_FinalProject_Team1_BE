package kr.inventory.domain.purchase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderCreateRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderItemRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderSearchRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderUpdateRequest;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderDetailResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderItemResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderSummaryResponse;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.purchase.service.PurchaseOrderService;
import kr.inventory.global.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseOrderController.class)
@DisplayName("발주서 컨트롤러 테스트")
class PurchaseOrderControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private PurchaseOrderService purchaseOrderService;

        private UUID storePublicId;
        private UUID vendorPublicId;
        private UUID purchaseOrderPublicId;
        private CustomUserDetails userDetails;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void setUp() {
                storePublicId = UUID.randomUUID();
                vendorPublicId = UUID.randomUUID();
                purchaseOrderPublicId = UUID.randomUUID();

                // CustomUserDetails Mocking
                userDetails = mock(CustomUserDetails.class);
                given(userDetails.getUserId()).willReturn(1L);
        }

        @Test
        @WithMockUser
        @DisplayName("발주서 생성 성공")
        void givenValidRequest_whenCreate_thenReturnsOk() throws Exception {
                // given
                PurchaseOrderItemRequest item = new PurchaseOrderItemRequest("양파", 10, "EA", new BigDecimal("1000"));
                PurchaseOrderCreateRequest request = new PurchaseOrderCreateRequest(vendorPublicId, List.of(item));

                PurchaseOrderDetailResponse response = new PurchaseOrderDetailResponse(
                                purchaseOrderPublicId,
                                vendorPublicId,
                                "테스트 거래처",
                                "PO-20250303-001",
                                PurchaseOrderStatus.ORDERED,
                                new BigDecimal("10000"),
                                null,
                                null,
                                Collections.emptyList());

                given(purchaseOrderService.create(eq(1L), eq(storePublicId), any(PurchaseOrderCreateRequest.class)))
                                .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/purchase-orders/{storePublicId}", storePublicId)
                                .with(user(userDetails))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.purchaseOrderPublicId").value(purchaseOrderPublicId.toString()))
                                .andExpect(jsonPath("$.status").value("ORDERED"));
        }

        @Test
        @WithMockUser
        @DisplayName("발주서 목록 조회 성공")
        void givenValidStore_whenGetList_thenReturnsOk() throws Exception {
                // given
                PurchaseOrderSummaryResponse summary = new PurchaseOrderSummaryResponse(
                                purchaseOrderPublicId,
                                vendorPublicId,
                                "테스트 거래처",
                                "PO-20250303-001",
                                PurchaseOrderStatus.ORDERED,
                                new BigDecimal("10000"));

                PageResponse<PurchaseOrderSummaryResponse> pageResponse = PageResponse.from(
                                new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1));

                given(purchaseOrderService.getPurchaseOrders(eq(1L), eq(storePublicId),
                                any(PurchaseOrderSearchRequest.class), any(Pageable.class)))
                                .willReturn(pageResponse);

                // when & then
                mockMvc.perform(get("/api/purchase-orders/{storePublicId}", storePublicId)
                                .with(user(userDetails)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].purchaseOrderPublicId")
                                                .value(purchaseOrderPublicId.toString()));
        }

        @Test
        @WithMockUser
        @DisplayName("발주서 상세 조회 성공")
        void givenValidPurchaseOrder_whenGetDetail_thenReturnsOk() throws Exception {
                // given
                PurchaseOrderDetailResponse response = new PurchaseOrderDetailResponse(
                                purchaseOrderPublicId,
                                vendorPublicId,
                                "테스트 거래처",
                                "PO-20250303-001",
                                PurchaseOrderStatus.ORDERED,
                                new BigDecimal("10000"),
                                null,
                                null,
                                List.of(new PurchaseOrderItemResponse("양파", 10, "EA", new BigDecimal("1000"),
                                                new BigDecimal("10000"))));

                given(purchaseOrderService.getPurchaseOrder(eq(1L), eq(storePublicId), eq(purchaseOrderPublicId)))
                                .willReturn(response);

                // when & then
                mockMvc.perform(get("/api/purchase-orders/{storePublicId}/{purchaseOrderPublicId}", storePublicId,
                                purchaseOrderPublicId)
                                .with(user(userDetails)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.purchaseOrderPublicId").value(purchaseOrderPublicId.toString()));
        }

        @Test
        @WithMockUser
        @DisplayName("발주서 수정 성공")
        void givenValidRequest_whenUpdate_thenReturnsOk() throws Exception {
                // given
                PurchaseOrderItemRequest item = new PurchaseOrderItemRequest("당근", 15, "G", new BigDecimal("1200"));
                PurchaseOrderUpdateRequest request = new PurchaseOrderUpdateRequest(vendorPublicId, List.of(item));

                PurchaseOrderDetailResponse response = new PurchaseOrderDetailResponse(
                                purchaseOrderPublicId,
                                vendorPublicId,
                                "테스트 거래처",
                                "PO-20250303-001",
                                PurchaseOrderStatus.ORDERED,
                                new BigDecimal("18000"),
                                null,
                                null,
                                Collections.emptyList());

                given(purchaseOrderService.update(eq(1L), eq(storePublicId), eq(purchaseOrderPublicId),
                                any(PurchaseOrderUpdateRequest.class)))
                                .willReturn(response);

                // when & then
                mockMvc.perform(put("/api/purchase-orders/{storePublicId}/{purchaseOrderPublicId}", storePublicId,
                                purchaseOrderPublicId)
                                .with(user(userDetails))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("ORDERED"));
        }

        @Test
        @WithMockUser
        @DisplayName("발주서 취소 성공")
        void givenValidPurchaseOrder_whenCancel_thenReturnsOk() throws Exception {
                // given
                UUID canceledByUserPublicId = UUID.randomUUID();
                PurchaseOrderDetailResponse response = new PurchaseOrderDetailResponse(
                                purchaseOrderPublicId,
                                vendorPublicId,
                                "테스트 거래처",
                                "PO-20250303-001",
                                PurchaseOrderStatus.CANCELED,
                                new BigDecimal("10000"),
                                canceledByUserPublicId,
                                null,
                                Collections.emptyList());

                given(purchaseOrderService.cancel(eq(1L), eq(storePublicId), eq(purchaseOrderPublicId)))
                                .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/purchase-orders/{storePublicId}/{purchaseOrderPublicId}/cancel",
                                storePublicId, purchaseOrderPublicId)
                                .with(user(userDetails))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CANCELED"));
        }

        @Test
        @WithMockUser
        @DisplayName("발주서 PDF 다운로드 성공")
        void givenValidPurchaseOrder_whenDownloadPdf_thenReturnsPdf() throws Exception {
                // given
                byte[] pdfBytes = "PDF_CONTENT".getBytes();
                given(purchaseOrderService.downloadPdf(eq(1L), eq(storePublicId), eq(purchaseOrderPublicId)))
                                .willReturn(pdfBytes);

                // when & then
                mockMvc.perform(get("/api/purchase-orders/{storePublicId}/{purchaseOrderPublicId}/pdf", storePublicId,
                                purchaseOrderPublicId)
                                .with(user(userDetails)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                                .andExpect(header().string("Content-Disposition",
                                                org.hamcrest.Matchers.containsString("attachment")))
                                .andExpect(content().bytes(pdfBytes));
        }
}