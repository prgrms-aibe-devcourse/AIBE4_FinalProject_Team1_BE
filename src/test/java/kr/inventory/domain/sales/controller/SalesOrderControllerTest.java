package kr.inventory.domain.sales.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kr.inventory.domain.sales.controller.dto.request.SalesOrderCreateRequest;
import kr.inventory.domain.sales.controller.dto.request.SalesOrderItemRequest;
import kr.inventory.domain.sales.controller.dto.response.SalesOrderResponse;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
import kr.inventory.domain.sales.exception.SalesOrderErrorCode;
import kr.inventory.domain.sales.exception.SalesOrderException;
import kr.inventory.domain.sales.service.SalesOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalesOrderController.class)
@DisplayName("주문 컨트롤러 테스트")
class SalesOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @InjectMocks
    private SalesOrderService salesOrderService;

    @Test
    @DisplayName("주문 생성 API - 성공")
    void givenValidRequest_whenCreateOrder_thenReturn201() throws Exception {
        // given
        UUID menuPublicId1 = UUID.randomUUID();
        UUID menuPublicId2 = UUID.randomUUID();

        SalesOrderItemRequest item1 = new SalesOrderItemRequest(menuPublicId1, 2);
        SalesOrderItemRequest item2 = new SalesOrderItemRequest(menuPublicId2, 1);
        SalesOrderCreateRequest request = new SalesOrderCreateRequest(List.of(item1, item2));

        SalesOrderResponse response = new SalesOrderResponse(
                UUID.randomUUID(),
                SalesOrderStatus.COMPLETED,
                SalesOrderType.DINE_IN,
                new BigDecimal("23000"),
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                "T1",
                Collections.emptyList()
        );

        given(salesOrderService.createOrder(anyString(), anyString(), any(SalesOrderCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .cookie(new Cookie("sessionToken", "test-token"))
                        .header("Idempotency-Key", "idempotency-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalAmount").value(23000))
                .andExpect(jsonPath("$.tableCode").value("T1"));
    }

    @Test
    @DisplayName("주문 생성 API - sessionToken 쿠키 없음")
    void givenNoSessionToken_whenCreateOrder_thenReturn400() throws Exception {
        // given
        SalesOrderCreateRequest request = new SalesOrderCreateRequest(Collections.emptyList());

        // when & then
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .header("Idempotency-Key", "idempotency-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주문 생성 API - Idempotency-Key 헤더 없음")
    void givenNoIdempotencyKey_whenCreateOrder_thenReturn400() throws Exception {
        // given
        SalesOrderCreateRequest request = new SalesOrderCreateRequest(Collections.emptyList());

        // when & then
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .cookie(new Cookie("sessionToken", "test-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주문 생성 API - 빈 items")
    void givenEmptyItems_whenCreateOrder_thenReturn400() throws Exception {
        // given
        SalesOrderCreateRequest request = new SalesOrderCreateRequest(Collections.emptyList());

        // when & then
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .cookie(new Cookie("sessionToken", "test-token"))
                        .header("Idempotency-Key", "idempotency-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주문 생성 API - 유효하지 않은 세션")
    void givenInvalidSession_whenCreateOrder_thenReturn400() throws Exception {
        // given
        UUID menuPublicId = UUID.randomUUID();
        SalesOrderItemRequest item = new SalesOrderItemRequest(menuPublicId, 2);
        SalesOrderCreateRequest request = new SalesOrderCreateRequest(List.of(item));

        given(salesOrderService.createOrder(anyString(), anyString(), any(SalesOrderCreateRequest.class)))
                .willThrow(new SalesOrderException(SalesOrderErrorCode.INVALID_SESSION));

        // when & then
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .cookie(new Cookie("sessionToken", "invalid-token"))
                        .header("Idempotency-Key", "idempotency-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주문 생성 API - 재고 부족")
    void givenInsufficientStock_whenCreateOrder_thenReturn409() throws Exception {
        // given
        UUID menuPublicId = UUID.randomUUID();
        SalesOrderItemRequest item = new SalesOrderItemRequest(menuPublicId, 2);
        SalesOrderCreateRequest request = new SalesOrderCreateRequest(List.of(item));

        given(salesOrderService.createOrder(anyString(), anyString(), any(SalesOrderCreateRequest.class)))
                .willThrow(new SalesOrderException(SalesOrderErrorCode.INSUFFICIENT_STOCK));

        // when & then
        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .cookie(new Cookie("sessionToken", "test-token"))
                        .header("Idempotency-Key", "idempotency-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("주문 목록 조회 API - 성공")
    void givenStorePublicId_whenGetStoreOrders_thenReturn200() throws Exception {
        // given
        UUID storePublicId = UUID.randomUUID();

        SalesOrderResponse response = new SalesOrderResponse(
                UUID.randomUUID(),
                SalesOrderStatus.COMPLETED,
                SalesOrderType.DINE_IN,
                new BigDecimal("23000"),
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                "T1",
                Collections.emptyList()
        );

        given(salesOrderService.getStoreOrders(any(), any()))
                .willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/orders/{storePublicId}", storePublicId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].totalAmount").value(23000));
    }

    @Test
    @WithMockUser
    @DisplayName("주문 상세 조회 API - 성공")
    void givenOrderPublicId_whenGetOrder_thenReturn200() throws Exception {
        // given
        UUID storePublicId = UUID.randomUUID();
        UUID orderPublicId = UUID.randomUUID();

        SalesOrderResponse response = new SalesOrderResponse(
                orderPublicId,
                SalesOrderStatus.COMPLETED,
                SalesOrderType.DINE_IN,
                new BigDecimal("23000"),
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                "T1",
                Collections.emptyList()
        );

        given(salesOrderService.getOrder(any(), any(), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/orders/{storePublicId}/{orderPublicId}", storePublicId, orderPublicId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderPublicId").value(orderPublicId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    @DisplayName("환불 처리 API - 성공")
    void givenOrderPublicId_whenRefund_thenReturn200() throws Exception {
        // given
        UUID storePublicId = UUID.randomUUID();
        UUID orderPublicId = UUID.randomUUID();

        SalesOrderResponse response = new SalesOrderResponse(
                orderPublicId,
                SalesOrderStatus.REFUNDED,
                SalesOrderType.DINE_IN,
                new BigDecimal("23000"),
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC),
                "T1",
                Collections.emptyList()
        );

        given(salesOrderService.refundOrder(any(), any(), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/orders/{storePublicId}/{orderPublicId}/refund", storePublicId, orderPublicId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.refundedAt").exists());
    }
}