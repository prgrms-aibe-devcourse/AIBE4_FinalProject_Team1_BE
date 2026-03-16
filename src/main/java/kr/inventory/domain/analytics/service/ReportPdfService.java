package kr.inventory.domain.analytics.service;

import kr.inventory.domain.analytics.constant.ReportConstants;
import kr.inventory.domain.analytics.exception.AnalyticsErrorCode;
import kr.inventory.domain.analytics.exception.AnalyticsException;
import kr.inventory.domain.analytics.service.report.ReportData;
import kr.inventory.domain.analytics.service.report.SalesSection;
import kr.inventory.domain.analytics.service.report.StockInboundSection;
import kr.inventory.domain.analytics.service.report.WasteSection;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class ReportPdfService {

    /**
     * ReportData → PDF byte[] 변환
     * - Page 1: 매출 섹션 + 환불 섹션
     * - Page 2: 폐기 섹션 + 입고 섹션
     */
    public byte[] generate(ReportData data, LocalDate from, LocalDate to) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDType0Font regularFont = loadKoreanFont(document, "fonts/NanumGothic.ttf");
            PDType0Font boldFont    = loadKoreanFont(document, "fonts/NanumGothicBold.ttf");

            // ── Page 1: 타이틀 + 매출 + 환불 ──
            PDPage page1 = new PDPage(PDRectangle.A4);
            document.addPage(page1);
            try (PDPageContentStream stream1 = new PDPageContentStream(document, page1)) {
                float cursorY = ReportConstants.PDF_START_Y;
                cursorY = renderTitle(stream1, boldFont, regularFont, from, to, cursorY);
                cursorY = renderSalesSection(stream1, regularFont, boldFont, data, cursorY);
                renderRefundSection(stream1, regularFont, boldFont, data, cursorY);
            }

            // ── Page 2: 폐기 + 입고 ──
            PDPage page2 = new PDPage(PDRectangle.A4);
            document.addPage(page2);
            try (PDPageContentStream stream2 = new PDPageContentStream(document, page2)) {
                float cursorY = ReportConstants.PDF_START_Y;
                cursorY = renderWasteSection(stream2, regularFont, boldFont, data, cursorY);
                renderInboundSection(stream2, regularFont, boldFont, data, cursorY);
            }

            document.save(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("[ReportPdf] PDF 생성 실패", e);
            throw new AnalyticsException(AnalyticsErrorCode.REPORT_GENERATION_FAILED);
        }
    }

    // ──────────────────────────────────────────────────────
    // Render Methods
    // ──────────────────────────────────────────────────────

    private float renderTitle(
            PDPageContentStream stream,
            PDType0Font boldFont,
            PDType0Font regularFont,
            LocalDate from,
            LocalDate to,
            float startY) throws IOException {

        String title = "운영 리포트";
        float textWidth = boldFont.getStringWidth(title) / 1000 * ReportConstants.PDF_TITLE_FONT_SIZE;
        float centerX = (ReportConstants.PDF_PAGE_WIDTH - textWidth) / 2;

        stream.beginText();
        stream.setFont(boldFont, ReportConstants.PDF_TITLE_FONT_SIZE);
        stream.newLineAtOffset(centerX, startY);
        stream.showText(title);
        stream.endText();

        float cursorY = startY - ReportConstants.PDF_TITLE_SPACING;

        String period = from + " ~ " + to;
        float periodWidth = regularFont.getStringWidth(period) / 1000 * ReportConstants.PDF_BODY_FONT_SIZE;
        float periodX = (ReportConstants.PDF_PAGE_WIDTH - periodWidth) / 2;

        stream.beginText();
        stream.setFont(regularFont, ReportConstants.PDF_BODY_FONT_SIZE);
        stream.newLineAtOffset(periodX, cursorY);
        stream.showText(period);
        stream.endText();

        cursorY -= ReportConstants.PDF_PERIOD_SPACING;
        drawHorizontalLine(stream, cursorY);

        return cursorY - 14f;
    }

    private float renderSalesSection(
            PDPageContentStream stream,
            PDType0Font regularFont,
            PDType0Font boldFont,
            ReportData data,
            float startY) throws IOException {

        float cursorY = renderSectionHeader(stream, boldFont, "[ 매출 현황 ]", startY);

        cursorY = writeLine(stream, regularFont,
                "총 주문건수: " + data.sales().totalOrderCount() + "건", cursorY);
        cursorY = writeLine(stream, regularFont,
                "총 매출금액: " + formatCurrency(data.sales().totalAmount()), cursorY);
        cursorY = writeLine(stream, regularFont,
                "평균 객단가: " + formatCurrency(data.sales().averageOrderAmount()), cursorY);
        cursorY = writeLine(stream, regularFont,
                "최대 주문금액: " + formatCurrency(data.sales().maxOrderAmount()), cursorY);

        cursorY -= 6f;
        cursorY = writeLine(stream, boldFont, "메뉴 TOP " + ReportConstants.REPORT_TOP_N_MENU, cursorY);

        List<SalesSection.MenuEntry> menuTop5 = data.sales().menuTop5();
        if (menuTop5.isEmpty()) {
            cursorY = writeLine(stream, regularFont, "  데이터 없음", cursorY);
        } else {
            for (SalesSection.MenuEntry entry : menuTop5) {
                String line = "  " + entry.rank() + ". " + entry.menuName()
                        + "  " + entry.totalQuantity() + "개"
                        + "  " + formatCurrency(entry.totalAmount());
                cursorY = writeLine(stream, regularFont, line, cursorY);
            }
        }

        cursorY -= ReportConstants.PDF_SECTION_GAP;
        drawHorizontalLine(stream, cursorY);
        return cursorY - 14f;
    }

    private float renderRefundSection(
            PDPageContentStream stream,
            PDType0Font regularFont,
            PDType0Font boldFont,
            ReportData data,
            float startY) throws IOException {

        float cursorY = renderSectionHeader(stream, boldFont, "[ 환불 현황 ]", startY);

        cursorY = writeLine(stream, regularFont,
                "환불 건수: " + data.refund().refundCount() + "건", cursorY);
        cursorY = writeLine(stream, regularFont,
                "환불 금액: " + formatCurrency(data.refund().totalRefundAmount()), cursorY);
        writeLine(stream, regularFont,
                "환불율: " + data.refund().refundRate() + "%", cursorY);

        return cursorY;
    }

    private float renderWasteSection(
            PDPageContentStream stream,
            PDType0Font regularFont,
            PDType0Font boldFont,
            ReportData data,
            float startY) throws IOException {

        float cursorY = renderSectionHeader(stream, boldFont, "[ 폐기 현황 ]", startY);

        cursorY = writeLine(stream, regularFont,
                "총 폐기금액: " + formatCurrency(data.waste().totalWasteAmount()), cursorY);
        cursorY = writeLine(stream, regularFont,
                "총 폐기수량: " + data.waste().totalWasteQuantity(), cursorY);

        cursorY -= 6f;
        cursorY = writeLine(stream, boldFont, "사유별 분석", cursorY);

        List<WasteSection.ReasonEntry> reasons = data.waste().reasonBreakdown();
        if (reasons.isEmpty()) {
            cursorY = writeLine(stream, regularFont, "  데이터 없음", cursorY);
        } else {
            for (WasteSection.ReasonEntry entry : reasons) {
                String line = "  " + entry.reason()
                        + "  " + entry.count() + "건"
                        + "  " + formatCurrency(entry.wasteAmount())
                        + "  (" + entry.ratio() + "%)";
                cursorY = writeLine(stream, regularFont, line, cursorY);
            }
        }

        cursorY -= 6f;
        cursorY = writeLine(stream, boldFont, "폐기 TOP " + ReportConstants.REPORT_TOP_N_WASTE_INGREDIENT + " 식재료", cursorY);

        List<WasteSection.IngredientEntry> top5 = data.waste().top5Ingredients();
        if (top5.isEmpty()) {
            cursorY = writeLine(stream, regularFont, "  데이터 없음", cursorY);
        } else {
            int rank = 1;
            for (WasteSection.IngredientEntry entry : top5) {
                String line = "  " + rank++ + ". " + entry.ingredientName()
                        + "  " + entry.wasteQuantity()
                        + "  " + formatCurrency(entry.wasteAmount());
                cursorY = writeLine(stream, regularFont, line, cursorY);
            }
        }

        cursorY -= ReportConstants.PDF_SECTION_GAP;
        drawHorizontalLine(stream, cursorY);
        return cursorY - 14f;
    }

    private float renderInboundSection(
            PDPageContentStream stream,
            PDType0Font regularFont,
            PDType0Font boldFont,
            ReportData data,
            float startY) throws IOException {

        float cursorY = renderSectionHeader(stream, boldFont, "[ 입고 현황 ]", startY);

        cursorY = writeLine(stream, regularFont,
                "총 입고건수: " + data.inbound().totalInboundCount() + "건", cursorY);

        cursorY -= 6f;
        cursorY = writeLine(stream, boldFont, "거래처별 현황", cursorY);

        List<StockInboundSection.VendorEntry> vendors = data.inbound().vendorBreakdown();
        if (vendors.isEmpty()) {
            writeLine(stream, regularFont, "  데이터 없음", cursorY);
        } else {
            for (StockInboundSection.VendorEntry entry : vendors) {
                String line = "  " + entry.vendorName() + "  " + entry.inboundCount() + "건";
                cursorY = writeLine(stream, regularFont, line, cursorY);
            }
        }

        return cursorY;
    }

    // ──────────────────────────────────────────────────────
    // Helper Methods
    // ──────────────────────────────────────────────────────

    private float renderSectionHeader(
            PDPageContentStream stream, PDType0Font boldFont, String header, float y) throws IOException {
        stream.beginText();
        stream.setFont(boldFont, ReportConstants.PDF_SECTION_FONT_SIZE);
        stream.newLineAtOffset(ReportConstants.PDF_MARGIN_LEFT, y);
        stream.showText(header);
        stream.endText();
        return y - ReportConstants.PDF_SECTION_HEADER_SPACING;
    }

    private float writeLine(
            PDPageContentStream stream, PDType0Font font, String text, float y) throws IOException {
        stream.beginText();
        stream.setFont(font, ReportConstants.PDF_BODY_FONT_SIZE);
        stream.newLineAtOffset(ReportConstants.PDF_MARGIN_LEFT, y);
        stream.showText(text);
        stream.endText();
        return y - ReportConstants.PDF_LINE_HEIGHT;
    }

    private void drawHorizontalLine(PDPageContentStream stream, float y) throws IOException {
        stream.moveTo(ReportConstants.PDF_MARGIN_LEFT, y);
        stream.lineTo(ReportConstants.PDF_PAGE_WIDTH - ReportConstants.PDF_MARGIN_RIGHT, y);
        stream.stroke();
    }

    private PDType0Font loadKoreanFont(PDDocument document, String fontPath) throws IOException {
        try (InputStream fontStream = new ClassPathResource(fontPath).getInputStream()) {
            return PDType0Font.load(document, fontStream);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₩0";
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.KOREA);
        return "₩" + formatter.format(amount);
    }
}
