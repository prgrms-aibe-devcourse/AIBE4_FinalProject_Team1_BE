package kr.inventory.domain.analytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.analytics.controller.dto.request.ReportSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.ReportSummaryResponse;
import kr.inventory.domain.analytics.exception.AnalyticsErrorCode;
import kr.inventory.domain.analytics.exception.AnalyticsException;
import kr.inventory.domain.analytics.service.ReportService;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.auth.service.CustomOAuth2UserService;
import kr.inventory.global.auth.filter.OAuth2AuthorizationRedirectFilter;
import kr.inventory.global.auth.handler.OAuth2SuccessHandler;
import kr.inventory.global.auth.jwt.JwtProvider;
import kr.inventory.global.config.CorsProperties;
import kr.inventory.global.security.handler.RestAccessDeniedHandler;
import kr.inventory.global.security.handler.RestAuthenticationEntryPoint;
import kr.inventory.global.util.CookieUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@DisplayName("리포트 컨트롤러 테스트")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private CorsProperties corsProperties;

    @MockitoBean
    private CookieUtil cookieUtil;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private OAuth2AuthorizationRedirectFilter oAuth2AuthorizationRedirectFilter;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    private CustomUserDetails createMockUser() {
        CustomUserDetails mockUser = mock(CustomUserDetails.class);
        given(mockUser.getUserId()).willReturn(100L);
        return mockUser;
    }

    @Nested
    @DisplayName("리포트 발행 API")
    class GenerateReport {

        @Test
        @WithMockUser
        @DisplayName("정상적인 요청으로 리포트를 발행하면 200 OK와 PDF가 반환된다")
        void givenValidRequest_whenGenerateReport_thenReturn200WithPdf() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            ReportSearchRequest request = new ReportSearchRequest(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 31)
            );
            byte[] pdfBytes = "test-pdf-content".getBytes();
            CustomUserDetails mockUser = createMockUser();

            given(reportService.generateReport(eq(100L), eq(storePublicId), any(ReportSearchRequest.class)))
                    .willReturn(pdfBytes);

            // when & then
            mockMvc.perform(post("/api/analytics/{storePublicId}/reports", storePublicId)
                            .with(user(mockUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                    .andExpect(header().exists("Content-Disposition"))
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"report-2025-01-01-2025-01-31.pdf\""))
                    .andExpect(content().bytes(pdfBytes));
        }

        @Test
        @WithMockUser
        @DisplayName("from이 null이면 400 Bad Request가 반환된다")
        void givenNullFrom_whenGenerateReport_thenReturn400() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            String invalidRequest = """
                    {
                        "to": "2025-01-31"
                    }
                    """;
            CustomUserDetails mockUser = createMockUser();

            // when & then
            mockMvc.perform(post("/api/analytics/{storePublicId}/reports", storePublicId)
                            .with(user(mockUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("to가 null이면 400 Bad Request가 반환된다")
        void givenNullTo_whenGenerateReport_thenReturn400() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            String invalidRequest = """
                    {
                        "from": "2025-01-01"
                    }
                    """;
            CustomUserDetails mockUser = createMockUser();

            // when & then
            mockMvc.perform(post("/api/analytics/{storePublicId}/reports", storePublicId)
                            .with(user(mockUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("미래 날짜로 요청하면 예외가 발생한다")
        void givenFutureDate_whenGenerateReport_thenThrowException() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            ReportSearchRequest request = new ReportSearchRequest(
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(10)
            );
            CustomUserDetails mockUser = createMockUser();

            given(reportService.generateReport(eq(100L), eq(storePublicId), any(ReportSearchRequest.class)))
                    .willThrow(new AnalyticsException(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED));

            // when & then
            mockMvc.perform(post("/api/analytics/{storePublicId}/reports", storePublicId)
                            .with(user(mockUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("날짜 범위가 잘못되면 예외가 발생한다")
        void givenInvalidDateRange_whenGenerateReport_thenThrowException() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            ReportSearchRequest request = new ReportSearchRequest(
                    LocalDate.of(2025, 2, 1),
                    LocalDate.of(2025, 1, 1)  // from > to
            );
            CustomUserDetails mockUser = createMockUser();

            given(reportService.generateReport(eq(100L), eq(storePublicId), any(ReportSearchRequest.class)))
                    .willThrow(new AnalyticsException(AnalyticsErrorCode.INVALID_DATE_RANGE));

            // when & then
            mockMvc.perform(post("/api/analytics/{storePublicId}/reports", storePublicId)
                            .with(user(mockUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("월간 리포트 조회 API")
    class GetMonthlyReport {

        @Test
        @WithMockUser
        @DisplayName("전월 yearMonth로 월간 리포트를 조회하면 200 OK와 PDF가 반환된다")
        void givenValidYearMonth_whenGetMonthlyReport_thenReturn200WithPdf() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            String yearMonth = "2025-01";
            byte[] pdfBytes = "test-pdf-content".getBytes();
            CustomUserDetails mockUser = createMockUser();

            given(reportService.generateMonthlyReport(eq(100L), eq(storePublicId), eq(yearMonth)))
                    .willReturn(pdfBytes);

            // when & then
            mockMvc.perform(get("/api/analytics/{storePublicId}/reports/monthly/{yearMonth}", storePublicId, yearMonth)
                            .with(user(mockUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                    .andExpect(header().exists("Content-Disposition"))
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"monthly-report-2025-01.pdf\""))
                    .andExpect(content().bytes(pdfBytes));
        }

        @Test
        @WithMockUser
        @DisplayName("당월을 조회하면 예외가 발생한다")
        void givenCurrentMonth_whenGetMonthlyReport_thenThrowException() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            String currentMonth = LocalDate.now().toString().substring(0, 7);  // yyyy-MM
            CustomUserDetails mockUser = createMockUser();

            given(reportService.generateMonthlyReport(eq(100L), eq(storePublicId), eq(currentMonth)))
                    .willThrow(new AnalyticsException(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED));

            // when & then
            mockMvc.perform(get("/api/analytics/{storePublicId}/reports/monthly/{yearMonth}", storePublicId, currentMonth)
                            .with(user(mockUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("미래 월을 조회하면 예외가 발생한다")
        void givenFutureMonth_whenGetMonthlyReport_thenThrowException() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            String futureMonth = "2026-12";
            CustomUserDetails mockUser = createMockUser();

            given(reportService.generateMonthlyReport(eq(100L), eq(storePublicId), eq(futureMonth)))
                    .willThrow(new AnalyticsException(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED));

            // when & then
            mockMvc.perform(get("/api/analytics/{storePublicId}/reports/monthly/{yearMonth}", storePublicId, futureMonth)
                            .with(user(mockUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("잘못된 yearMonth 형식이면 예외가 발생한다")
        void givenInvalidYearMonthFormat_whenGetMonthlyReport_thenThrowException() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            String invalidYearMonth = "2025-13";  // 잘못된 월
            CustomUserDetails mockUser = createMockUser();

            given(reportService.generateMonthlyReport(eq(100L), eq(storePublicId), eq(invalidYearMonth)))
                    .willThrow(new AnalyticsException(AnalyticsErrorCode.INVALID_DATE_RANGE));

            // when & then
            mockMvc.perform(get("/api/analytics/{storePublicId}/reports/monthly/{yearMonth}", storePublicId, invalidYearMonth)
                            .with(user(mockUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("yyyy-MM 형식이 아니면 예외가 발생한다")
        void givenWrongFormat_whenGetMonthlyReport_thenThrowException() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            String wrongFormat = "202501";  // 하이픈 없음
            CustomUserDetails mockUser = createMockUser();

            given(reportService.generateMonthlyReport(eq(100L), eq(storePublicId), eq(wrongFormat)))
                    .willThrow(new AnalyticsException(AnalyticsErrorCode.INVALID_DATE_RANGE));

            // when & then
            mockMvc.perform(get("/api/analytics/{storePublicId}/reports/monthly/{yearMonth}", storePublicId, wrongFormat)
                            .with(user(mockUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("리포트 요약 조회 API")
    class GetReportSummary {

        @Test
        @WithMockUser
        @DisplayName("정상적인 요청으로 리포트 요약을 조회하면 200 OK와 JSON이 반환된다")
        void givenValidRequest_whenGetReportSummary_thenReturn200WithJson() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            LocalDate from = LocalDate.of(2025, 1, 1);
            LocalDate to = LocalDate.of(2025, 1, 31);
            CustomUserDetails mockUser = createMockUser();

            ReportSummaryResponse response = new ReportSummaryResponse(
                    from, to,
                    100L, new BigDecimal("1000000.00"), new BigDecimal("10000.00"),
                    Collections.emptyList(),
                    5L, 5.0,
                    new BigDecimal("30000.00"), Collections.emptyList(),
                    10L
            );

            given(reportService.getReportSummary(eq(100L), eq(storePublicId), any(ReportSearchRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/analytics/{storePublicId}/reports/summary", storePublicId)
                            .with(user(mockUser))
                            .with(csrf())
                            .param("from", "2025-01-01")
                            .param("to", "2025-01-31"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.from").value("2025-01-01"))
                    .andExpect(jsonPath("$.to").value("2025-01-31"))
                    .andExpect(jsonPath("$.totalOrderCount").value(100))
                    .andExpect(jsonPath("$.totalAmount").value(1000000.00))
                    .andExpect(jsonPath("$.refundCount").value(5))
                    .andExpect(jsonPath("$.totalInboundCount").value(10));
        }

        @Test
        @WithMockUser
        @DisplayName("미래 날짜로 요청하면 예외가 발생한다")
        void givenFutureDate_whenGetReportSummary_thenThrowException() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            CustomUserDetails mockUser = createMockUser();

            given(reportService.getReportSummary(eq(100L), eq(storePublicId), any(ReportSearchRequest.class)))
                    .willThrow(new AnalyticsException(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED));

            // when & then
            mockMvc.perform(get("/api/analytics/{storePublicId}/reports/summary", storePublicId)
                            .with(user(mockUser))
                            .with(csrf())
                            .param("from", LocalDate.now().plusDays(1).toString())
                            .param("to", LocalDate.now().plusDays(10).toString()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("월간 리포트 요약 조회 API")
    class GetMonthlyReportSummary {

        @Test
        @WithMockUser
        @DisplayName("전월 yearMonth로 월간 리포트 요약을 조회하면 200 OK와 JSON이 반환된다")
        void givenValidYearMonth_whenGetMonthlyReportSummary_thenReturn200WithJson() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            String yearMonth = "2025-01";
            CustomUserDetails mockUser = createMockUser();

            ReportSummaryResponse response = new ReportSummaryResponse(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 31),
                    200L, new BigDecimal("2000000.00"), new BigDecimal("10000.00"),
                    Collections.emptyList(),
                    10L, 5.0,
                    new BigDecimal("60000.00"), Collections.emptyList(),
                    20L
            );

            given(reportService.getMonthlyReportSummary(eq(100L), eq(storePublicId), eq(yearMonth)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/analytics/{storePublicId}/reports/monthly/{yearMonth}/summary", storePublicId, yearMonth)
                            .with(user(mockUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.from").value("2025-01-01"))
                    .andExpect(jsonPath("$.to").value("2025-01-31"))
                    .andExpect(jsonPath("$.totalOrderCount").value(200))
                    .andExpect(jsonPath("$.totalAmount").value(2000000.00))
                    .andExpect(jsonPath("$.refundCount").value(10))
                    .andExpect(jsonPath("$.totalInboundCount").value(20));
        }

        @Test
        @WithMockUser
        @DisplayName("당월을 조회하면 예외가 발생한다")
        void givenCurrentMonth_whenGetMonthlyReportSummary_thenThrowException() throws Exception {
            // given
            UUID storePublicId = UUID.randomUUID();
            String currentMonth = LocalDate.now().toString().substring(0, 7);  // yyyy-MM
            CustomUserDetails mockUser = createMockUser();

            given(reportService.getMonthlyReportSummary(eq(100L), eq(storePublicId), eq(currentMonth)))
                    .willThrow(new AnalyticsException(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED));

            // when & then
            mockMvc.perform(get("/api/analytics/{storePublicId}/reports/monthly/{yearMonth}/summary", storePublicId, currentMonth)
                            .with(user(mockUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }
}