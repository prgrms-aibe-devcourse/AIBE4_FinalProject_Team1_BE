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
     * - Page 1: 타이틀 + 매출 섹션 + 환불 섹션
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
                cursorY = renderSalesSection(stream1, boldFont, regularFont, data, cursorY);
                renderRefundSection(stream1, boldFont, regularFont, data, cursorY);
            }

            // ── Page 2: 폐기 + 입고 ──
            PDPage page2 = new PDPage(PDRectangle.A4);
            document.addPage(page2);
            try (PDPageContentStream stream2 = new PDPageContentStream(document, page2)) {
                float cursorY = ReportConstants.PDF_START_Y;
                cursorY = renderWasteSection(stream2, boldFont, regularFont, data, cursorY);
                renderInboundSection(stream2, boldFont, regularFont, data, cursorY);
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

        // 제목 (가운데 정렬)
        String title = "운영 리포트";
        float titleWidth = boldFont.getStringWidth(title) / 1000 * ReportConstants.PDF_TITLE_FONT_SIZE;
        float titleX = (ReportConstants.PDF_PAGE_WIDTH - titleWidth) / 2;
        setColor(stream, ReportConstants.PDF_COLOR_DARK);
        writeText(stream, boldFont, ReportConstants.PDF_TITLE_FONT_SIZE, titleX, startY, title);

        float cursorY = startY - ReportConstants.PDF_TITLE_SPACING;

        // 기간 (가운데 정렬, 회색)
        String period = from + "  ~  " + to;
        float periodWidth = regularFont.getStringWidth(period) / 1000 * ReportConstants.PDF_BODY_FONT_SIZE;
        float periodX = (ReportConstants.PDF_PAGE_WIDTH - periodWidth) / 2;
        setColor(stream, ReportConstants.PDF_COLOR_GRAY);
        writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE, periodX, cursorY, period);

        cursorY -= ReportConstants.PDF_PERIOD_SPACING;
        setColor(stream, ReportConstants.PDF_COLOR_DARK);
        drawLine(stream, ReportConstants.PDF_MARGIN_LEFT, cursorY,
                ReportConstants.PDF_PAGE_WIDTH - ReportConstants.PDF_MARGIN_RIGHT, cursorY, 1.5f);

        return cursorY - 14f;
    }

    private float renderSalesSection(
            PDPageContentStream stream,
            PDType0Font boldFont,
            PDType0Font regularFont,
            ReportData data,
            float startY) throws IOException {

        float cursorY = renderSectionHeader(stream, boldFont, "매출 현황", startY);
        float contentWidth = ReportConstants.PDF_PAGE_WIDTH - ReportConstants.PDF_MARGIN_LEFT - ReportConstants.PDF_MARGIN_RIGHT;

        // KPI 3개 가로 배열
        float kpiWidth = contentWidth / 3;
        float[] kpiX = {
                ReportConstants.PDF_MARGIN_LEFT,
                ReportConstants.PDF_MARGIN_LEFT + kpiWidth,
                ReportConstants.PDF_MARGIN_LEFT + kpiWidth * 2
        };
        String[] kpiLabels = {"총 매출금액", "총 주문건수", "평균 객단가"};
        String[] kpiValues = {
                formatCurrency(data.sales().totalAmount()),
                data.sales().totalOrderCount() + "건",
                formatCurrency(data.sales().averageOrderAmount())
        };

        for (int i = 0; i < 3; i++) {
            drawFilledRect(stream, kpiX[i], cursorY - ReportConstants.PDF_KPI_ROW_HEIGHT,
                    kpiWidth - 4f, ReportConstants.PDF_KPI_ROW_HEIGHT, ReportConstants.PDF_COLOR_LIGHT_BG);
            setColor(stream, ReportConstants.PDF_COLOR_GRAY);
            writeText(stream, regularFont, ReportConstants.PDF_FONT_LABEL, kpiX[i] + 8f, cursorY - 14f, kpiLabels[i]);
            setColor(stream, ReportConstants.PDF_COLOR_DARK);
            writeText(stream, boldFont, ReportConstants.PDF_FONT_VALUE, kpiX[i] + 8f, cursorY - 34f, kpiValues[i]);
        }
        cursorY -= ReportConstants.PDF_KPI_ROW_HEIGHT + 16f;

        // 메뉴 TOP5 테이블
        setColor(stream, ReportConstants.PDF_COLOR_DARK);
        cursorY -= ReportConstants.PDF_SUB_TITLE_TOP_GAP;
        writeText(stream, boldFont, ReportConstants.PDF_SECTION_FONT_SIZE,
                ReportConstants.PDF_MARGIN_LEFT, cursorY, "메뉴 TOP " + ReportConstants.REPORT_TOP_N_MENU);
        cursorY -= 14f;

        cursorY = renderTableHeader(stream, regularFont, cursorY,
                new float[]{
                        ReportConstants.PDF_TABLE_COL_RANK,
                        ReportConstants.PDF_TABLE_COL_NAME,
                        ReportConstants.PDF_TABLE_COL_QUANTITY,
                        ReportConstants.PDF_TABLE_COL_AMOUNT
                },
                new String[]{"순위", "메뉴명", "판매수량", "매출금액"});

        List<SalesSection.MenuEntry> menuTop5 = data.sales().menuTop5();
        if (menuTop5.isEmpty()) {
            setColor(stream, ReportConstants.PDF_COLOR_GRAY);
            writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                    ReportConstants.PDF_MARGIN_LEFT + 4f, cursorY, "데이터 없음");
            cursorY -= ReportConstants.PDF_LINE_HEIGHT;
        } else {
            for (SalesSection.MenuEntry entry : menuTop5) {
                setColor(stream, ReportConstants.PDF_COLOR_DARK);
                writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_RANK, cursorY, entry.rank() + ".");
                writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_NAME, cursorY, entry.menuName());
                writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_QUANTITY, cursorY, entry.totalQuantity() + "개");
                writeText(stream, boldFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_AMOUNT, cursorY, formatCurrency(entry.totalAmount()));
                cursorY -= ReportConstants.PDF_LINE_HEIGHT;
                drawRowDivider(stream, cursorY);
            }
        }

        cursorY -= ReportConstants.PDF_SECTION_GAP;
        setColor(stream, ReportConstants.PDF_COLOR_BOX_BORDER);
        drawLine(stream, ReportConstants.PDF_MARGIN_LEFT, cursorY,
                ReportConstants.PDF_PAGE_WIDTH - ReportConstants.PDF_MARGIN_RIGHT, cursorY, 0.5f);
        return cursorY - 16f;
    }

    private float renderRefundSection(
            PDPageContentStream stream,
            PDType0Font boldFont,
            PDType0Font regularFont,
            ReportData data,
            float startY) throws IOException {

        float cursorY = renderSectionHeader(stream, boldFont, "환불 현황", startY);
        float contentWidth = ReportConstants.PDF_PAGE_WIDTH - ReportConstants.PDF_MARGIN_LEFT - ReportConstants.PDF_MARGIN_RIGHT;

        float kpiWidth = contentWidth / 3;
        float[] kpiX = {
                ReportConstants.PDF_MARGIN_LEFT,
                ReportConstants.PDF_MARGIN_LEFT + kpiWidth,
                ReportConstants.PDF_MARGIN_LEFT + kpiWidth * 2
        };
        String[] labels = {"환불 건수", "환불 금액", "환불율"};
        String[] values = {
                data.refund().refundCount() + "건",
                formatCurrency(data.refund().totalRefundAmount()),
                data.refund().refundRate() + "%"
        };

        for (int i = 0; i < 3; i++) {
            drawFilledRect(stream, kpiX[i], cursorY - ReportConstants.PDF_KPI_ROW_HEIGHT,
                    kpiWidth - 4f, ReportConstants.PDF_KPI_ROW_HEIGHT, ReportConstants.PDF_COLOR_LIGHT_BG);
            setColor(stream, ReportConstants.PDF_COLOR_GRAY);
            writeText(stream, regularFont, ReportConstants.PDF_FONT_LABEL, kpiX[i] + 8f, cursorY - 14f, labels[i]);
            setColor(stream, ReportConstants.PDF_COLOR_DARK);
            writeText(stream, boldFont, ReportConstants.PDF_FONT_VALUE, kpiX[i] + 8f, cursorY - 34f, values[i]);
        }

        return cursorY - ReportConstants.PDF_KPI_ROW_HEIGHT - 16f;
    }

    private float renderWasteSection(
            PDPageContentStream stream,
            PDType0Font boldFont,
            PDType0Font regularFont,
            ReportData data,
            float startY) throws IOException {

        float cursorY = renderSectionHeader(stream, boldFont, "폐기 현황", startY);
        float contentWidth = ReportConstants.PDF_PAGE_WIDTH - ReportConstants.PDF_MARGIN_LEFT - ReportConstants.PDF_MARGIN_RIGHT;

        // KPI 2개
        float kpiWidth = contentWidth / 2;
        float[] kpiX = {ReportConstants.PDF_MARGIN_LEFT, ReportConstants.PDF_MARGIN_LEFT + kpiWidth};
        String[] kpiLabels = {"총 폐기금액", "총 폐기수량"};
        String[] kpiValues = {
                formatCurrency(data.waste().totalWasteAmount()),
                data.waste().totalWasteQuantity().toPlainString()
        };

        for (int i = 0; i < 2; i++) {
            drawFilledRect(stream, kpiX[i], cursorY - ReportConstants.PDF_KPI_ROW_HEIGHT,
                    kpiWidth - 4f, ReportConstants.PDF_KPI_ROW_HEIGHT, ReportConstants.PDF_COLOR_LIGHT_BG);
            setColor(stream, ReportConstants.PDF_COLOR_GRAY);
            writeText(stream, regularFont, ReportConstants.PDF_FONT_LABEL, kpiX[i] + 8f, cursorY - 14f, kpiLabels[i]);
            setColor(stream, ReportConstants.PDF_COLOR_DARK);
            writeText(stream, boldFont, ReportConstants.PDF_FONT_VALUE, kpiX[i] + 8f, cursorY - 34f, kpiValues[i]);
        }
        cursorY -= ReportConstants.PDF_KPI_ROW_HEIGHT + 16f;

        // 사유별 분석 테이블
        setColor(stream, ReportConstants.PDF_COLOR_DARK);
        cursorY -= ReportConstants.PDF_SUB_TITLE_TOP_GAP;
        writeText(stream, boldFont, ReportConstants.PDF_SECTION_FONT_SIZE,
                ReportConstants.PDF_MARGIN_LEFT, cursorY, "사유별 분석");
        cursorY -= 14f;

        cursorY = renderTableHeader(stream, regularFont, cursorY,
                new float[]{
                        ReportConstants.PDF_TABLE_COL_RANK,
                        ReportConstants.PDF_TABLE_COL_REASON,
                        ReportConstants.PDF_TABLE_COL_WASTE_AMOUNT,
                        ReportConstants.PDF_TABLE_COL_RATIO
                },
                new String[]{"사유", "건수", "폐기금액", "비율"});

        List<WasteSection.ReasonEntry> reasons = data.waste().reasonBreakdown();
        if (reasons.isEmpty()) {
            setColor(stream, ReportConstants.PDF_COLOR_GRAY);
            writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                    ReportConstants.PDF_MARGIN_LEFT + 4f, cursorY, "데이터 없음");
            cursorY -= ReportConstants.PDF_LINE_HEIGHT;
        } else {
            for (WasteSection.ReasonEntry entry : reasons) {
                String reasonLabel = ReportConstants.WASTE_REASON_LABELS
                        .getOrDefault(entry.reason(), entry.reason());
                setColor(stream, ReportConstants.PDF_COLOR_DARK);
                writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_RANK, cursorY, reasonLabel);
                writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_REASON, cursorY, entry.count() + "건");
                writeText(stream, boldFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_WASTE_AMOUNT, cursorY, formatCurrency(entry.wasteAmount()));
                writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_RATIO, cursorY, entry.ratio() + "%");
                cursorY -= ReportConstants.PDF_LINE_HEIGHT;
                drawRowDivider(stream, cursorY);
            }
        }
        cursorY -= 12f;

        // 폐기 TOP5 테이블
        setColor(stream, ReportConstants.PDF_COLOR_DARK);
        cursorY -= ReportConstants.PDF_SUB_TITLE_TOP_GAP;
        writeText(stream, boldFont, ReportConstants.PDF_SECTION_FONT_SIZE,
                ReportConstants.PDF_MARGIN_LEFT, cursorY, "폐기 TOP " + ReportConstants.REPORT_TOP_N_WASTE_INGREDIENT + " 식재료");
        cursorY -= 14f;

        cursorY = renderTableHeader(stream, regularFont, cursorY,
                new float[]{
                        ReportConstants.PDF_TABLE_COL_RANK,
                        ReportConstants.PDF_TABLE_COL_NAME,
                        ReportConstants.PDF_TABLE_COL_QUANTITY,
                        ReportConstants.PDF_TABLE_COL_AMOUNT
                },
                new String[]{"순위", "식재료명", "폐기수량", "폐기금액"});

        List<WasteSection.IngredientEntry> top5 = data.waste().top5Ingredients();
        if (top5.isEmpty()) {
            setColor(stream, ReportConstants.PDF_COLOR_GRAY);
            writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                    ReportConstants.PDF_MARGIN_LEFT + 4f, cursorY, "데이터 없음");
            cursorY -= ReportConstants.PDF_LINE_HEIGHT;
        } else {
            int rank = 1;
            for (WasteSection.IngredientEntry entry : top5) {
                setColor(stream, ReportConstants.PDF_COLOR_DARK);
                writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_RANK, cursorY, rank++ + ".");
                writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_NAME, cursorY, entry.ingredientName());
                writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_QUANTITY, cursorY, entry.wasteQuantity().toPlainString());
                writeText(stream, boldFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_AMOUNT, cursorY, formatCurrency(entry.wasteAmount()));
                cursorY -= ReportConstants.PDF_LINE_HEIGHT;
                drawRowDivider(stream, cursorY);
            }
        }

        cursorY -= ReportConstants.PDF_SECTION_GAP;
        setColor(stream, ReportConstants.PDF_COLOR_BOX_BORDER);
        drawLine(stream, ReportConstants.PDF_MARGIN_LEFT, cursorY,
                ReportConstants.PDF_PAGE_WIDTH - ReportConstants.PDF_MARGIN_RIGHT, cursorY, 0.5f);
        return cursorY - 16f;
    }

    private float renderInboundSection(
            PDPageContentStream stream,
            PDType0Font boldFont,
            PDType0Font regularFont,
            ReportData data,
            float startY) throws IOException {

        float cursorY = renderSectionHeader(stream, boldFont, "입고 현황", startY);
        float contentWidth = ReportConstants.PDF_PAGE_WIDTH - ReportConstants.PDF_MARGIN_LEFT - ReportConstants.PDF_MARGIN_RIGHT;

        // KPI 1개
        float kpiWidth = contentWidth / 3;
        drawFilledRect(stream, ReportConstants.PDF_MARGIN_LEFT, cursorY - ReportConstants.PDF_KPI_ROW_HEIGHT,
                kpiWidth - 4f, ReportConstants.PDF_KPI_ROW_HEIGHT, ReportConstants.PDF_COLOR_LIGHT_BG);
        setColor(stream, ReportConstants.PDF_COLOR_GRAY);
        writeText(stream, regularFont, ReportConstants.PDF_FONT_LABEL,
                ReportConstants.PDF_MARGIN_LEFT + 8f, cursorY - 14f, "총 입고건수");
        setColor(stream, ReportConstants.PDF_COLOR_DARK);
        writeText(stream, boldFont, ReportConstants.PDF_FONT_VALUE,
                ReportConstants.PDF_MARGIN_LEFT + 8f, cursorY - 34f, data.inbound().totalInboundCount() + "건");
        cursorY -= ReportConstants.PDF_KPI_ROW_HEIGHT + 16f;

        // 거래처별 현황 테이블
        setColor(stream, ReportConstants.PDF_COLOR_DARK);
        cursorY -= ReportConstants.PDF_SUB_TITLE_TOP_GAP;
        writeText(stream, boldFont, ReportConstants.PDF_SECTION_FONT_SIZE,
                ReportConstants.PDF_MARGIN_LEFT, cursorY, "거래처별 현황");
        cursorY -= 14f;

        cursorY = renderTableHeader(stream, regularFont, cursorY,
                new float[]{
                        ReportConstants.PDF_TABLE_COL_RANK,
                        ReportConstants.PDF_TABLE_COL_VENDOR_AMOUNT
                },
                new String[]{"거래처명", "입고건수"});

        List<StockInboundSection.VendorEntry> vendors = data.inbound().vendorBreakdown();
        if (vendors.isEmpty()) {
            setColor(stream, ReportConstants.PDF_COLOR_GRAY);
            writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                    ReportConstants.PDF_MARGIN_LEFT + 4f, cursorY, "데이터 없음");
        } else {
            for (StockInboundSection.VendorEntry entry : vendors) {
                setColor(stream, ReportConstants.PDF_COLOR_DARK);
                writeText(stream, regularFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_RANK, cursorY, entry.vendorName());
                writeText(stream, boldFont, ReportConstants.PDF_BODY_FONT_SIZE,
                        ReportConstants.PDF_MARGIN_LEFT + ReportConstants.PDF_TABLE_COL_VENDOR_AMOUNT, cursorY, entry.inboundCount() + "건");
                cursorY -= ReportConstants.PDF_LINE_HEIGHT;
                drawRowDivider(stream, cursorY);
            }
        }

        return cursorY;
    }

    // ──────────────────────────────────────────────────────
    // Helper Methods
    // ──────────────────────────────────────────────────────

    /**
     * 섹션 헤더 — 진한 배경 박스 + 흰 텍스트
     */
    private float renderSectionHeader(
            PDPageContentStream stream, PDType0Font boldFont, String title, float y) throws IOException {
        float contentWidth = ReportConstants.PDF_PAGE_WIDTH - ReportConstants.PDF_MARGIN_LEFT - ReportConstants.PDF_MARGIN_RIGHT;

        setColor(stream, ReportConstants.PDF_COLOR_DARK);
        stream.addRect(ReportConstants.PDF_MARGIN_LEFT, y - ReportConstants.PDF_SECTION_HEADER_HEIGHT,
                contentWidth, ReportConstants.PDF_SECTION_HEADER_HEIGHT);
        stream.fill();

        stream.setNonStrokingColor(1f, 1f, 1f);
        writeText(stream, boldFont, ReportConstants.PDF_SECTION_FONT_SIZE,
                ReportConstants.PDF_MARGIN_LEFT + 8f, y - 15f, title);

        setColor(stream, ReportConstants.PDF_COLOR_DARK);
        return y - ReportConstants.PDF_SECTION_HEADER_HEIGHT - 12f;
    }

    /**
     * 테이블 헤더 행 — 컬럼 레이블 + 상하 구분선
     */
    private float renderTableHeader(
            PDPageContentStream stream, PDType0Font regularFont,
            float y, float[] colOffsets, String[] colLabels) throws IOException {
        float contentWidth = ReportConstants.PDF_PAGE_WIDTH
                - ReportConstants.PDF_MARGIN_LEFT - ReportConstants.PDF_MARGIN_RIGHT;

        // 위 구분선
        drawLine(stream, ReportConstants.PDF_MARGIN_LEFT, y,
                ReportConstants.PDF_MARGIN_LEFT + contentWidth, y, 0.5f);
        y -= ReportConstants.PDF_TABLE_HEADER_TOP_PAD;  // 3f → 12f

        // 컬럼 레이블
        setColor(stream, ReportConstants.PDF_COLOR_GRAY);
        for (int i = 0; i < colLabels.length; i++) {
            writeText(stream, regularFont, ReportConstants.PDF_FONT_LABEL,
                    ReportConstants.PDF_MARGIN_LEFT + colOffsets[i], y, colLabels[i]);
        }
        y -= ReportConstants.PDF_TABLE_HEADER_BOTTOM_PAD;  // 4f → 6f

        // 아래 구분선
        drawLine(stream, ReportConstants.PDF_MARGIN_LEFT, y,
                ReportConstants.PDF_MARGIN_LEFT + contentWidth, y, 0.5f);
        return y - ReportConstants.PDF_LINE_HEIGHT;
    }

    /**
     * 행 구분선 (연한 색)
     */
    private void drawRowDivider(PDPageContentStream stream, float y) throws IOException {
        float contentWidth = ReportConstants.PDF_PAGE_WIDTH
                - ReportConstants.PDF_MARGIN_LEFT - ReportConstants.PDF_MARGIN_RIGHT;
        setColor(stream, ReportConstants.PDF_COLOR_DIVIDER);
        drawLine(stream, ReportConstants.PDF_MARGIN_LEFT, y + 10f,  // 2f → 10f
                ReportConstants.PDF_MARGIN_LEFT + contentWidth, y + 10f, 0.3f);
        setColor(stream, ReportConstants.PDF_COLOR_DARK);
    }

    private void writeText(
            PDPageContentStream stream, PDType0Font font, int fontSize,
            float x, float y, String text) throws IOException {
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    private void drawLine(
            PDPageContentStream stream,
            float x1, float y1, float x2, float y2, float lineWidth) throws IOException {
        stream.setLineWidth(lineWidth);
        stream.moveTo(x1, y1);
        stream.lineTo(x2, y2);
        stream.stroke();
    }

    private void drawFilledRect(
            PDPageContentStream stream,
            float x, float y, float width, float height, float[] rgb) throws IOException {
        stream.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        stream.addRect(x, y, width, height);
        stream.fill();
        setColor(stream, ReportConstants.PDF_COLOR_DARK);
    }

    private void setColor(PDPageContentStream stream, float[] rgb) throws IOException {
        stream.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        stream.setStrokingColor(rgb[0], rgb[1], rgb[2]);
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
