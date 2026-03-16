package kr.inventory.domain.analytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inventory.domain.analytics.controller.dto.request.ReportSearchRequest;
import kr.inventory.domain.analytics.exception.AnalyticsErrorCode;
import kr.inventory.domain.analytics.exception.AnalyticsException;
import kr.inventory.domain.analytics.service.ReportService;
import kr.inventory.domain.auth.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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
}