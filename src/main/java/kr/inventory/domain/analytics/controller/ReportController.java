package kr.inventory.domain.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.analytics.controller.dto.request.ReportSearchRequest;
import kr.inventory.domain.analytics.service.ReportService;
import kr.inventory.domain.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Report", description = "리포트")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "리포트 발행", description = "사용자 지정 기간(from~to)의 운영 리포트를 PDF로 즉시 다운로드합니다.")
    @PostMapping("/{storePublicId}/reports")
    public ResponseEntity<byte[]> generateReport(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @Valid @RequestBody ReportSearchRequest request
    ) {
        byte[] pdfBytes = reportService.generateReport(principal.getUserId(), storePublicId, request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("report-" + request.from() + "-" + request.to() + ".pdf")
                        .build());

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @Operation(summary = "월간 리포트 조회", description = "전월 기준 월간 운영 리포트를 PDF로 즉시 다운로드합니다. yearMonth 형식: yyyy-MM (예: 2025-06)")
    @GetMapping("/{storePublicId}/reports/monthly/{yearMonth}")
    public ResponseEntity<byte[]> getMonthlyReport(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @PathVariable String yearMonth
    ) {
        byte[] pdfBytes = reportService.generateMonthlyReport(
                principal.getUserId(), storePublicId, yearMonth);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("monthly-report-" + yearMonth + ".pdf")
                        .build());

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
