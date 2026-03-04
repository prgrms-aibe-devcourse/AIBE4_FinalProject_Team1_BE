package kr.inventory.domain.purchase.service;

import kr.inventory.domain.purchase.constant.PurchaseOrderConstant;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.PurchaseOrderItem;
import kr.inventory.domain.purchase.exception.PurchaseOrderErrorCode;
import kr.inventory.domain.purchase.exception.PurchaseOrderException;
import kr.inventory.domain.purchase.repository.PurchaseOrderItemRepository;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class PurchaseOrderPdfService {

    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    public byte[] generate(PurchaseOrder purchaseOrder) {
        List<PurchaseOrderItem> items = purchaseOrderItemRepository
                .findByPurchaseOrderPurchaseOrderId(purchaseOrder.getPurchaseOrderId());

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = createPage(document);

            // 한글 폰트 로드
            PDType0Font regularFont = loadKoreanFont(document, "fonts/NanumGothic.ttf");
            PDType0Font boldFont = loadKoreanFont(document, "fonts/NanumGothicBold.ttf");

            renderPageContent(document, page, regularFont, boldFont, purchaseOrder, items);

            document.save(outputStream);
            return outputStream.toByteArray();

        } catch (IOException exception) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.PDF_GENERATION_FAILED);
        }
    }

    /**
     * 한글 폰트 로드
     */
    private PDType0Font loadKoreanFont(PDDocument document, String fontPath) throws IOException {
        try (InputStream fontStream = new ClassPathResource(fontPath).getInputStream()) {
            return PDType0Font.load(document, fontStream);
        }
    }

    private void renderPageContent(
            PDDocument document,
            PDPage page,
            PDType0Font regularFont,
            PDType0Font boldFont,
            PurchaseOrder purchaseOrder,
            List<PurchaseOrderItem> items
    ) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float cursorY = PurchaseOrderConstant.PDF_START_Y;

            // 1. 타이틀 (가운데 정렬)
            cursorY = renderTitle(contentStream, boldFont, cursorY);

            // 2. 헤더 정보 (주문번호, 매장명, 거래처)
            cursorY = renderHeader(contentStream, regularFont, purchaseOrder, cursorY);

            // 3. 테이블 (품목 목록)
            cursorY = renderTable(contentStream, regularFont, boldFont, items, cursorY);

            // 4. 총액
            renderTotal(contentStream, boldFont, purchaseOrder, cursorY);
        }
    }

    private PDPage createPage(PDDocument document) {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return page;
    }

    /**
     * 타이틀 렌더링 (가운데 정렬)
     */
    private float renderTitle(PDPageContentStream contentStream, PDType0Font boldFont, float startY) throws IOException {
        String title = PurchaseOrderConstant.PDF_TITLE;
        int fontSize = PurchaseOrderConstant.PDF_TITLE_FONT_SIZE;

        // 텍스트 너비 계산
        float textWidth = boldFont.getStringWidth(title) / 1000 * fontSize;

        // 가운데 정렬 X 좌표
        float centerX = (PurchaseOrderConstant.PDF_PAGE_WIDTH - textWidth) / 2;

        contentStream.beginText();
        contentStream.setFont(boldFont, fontSize);
        contentStream.newLineAtOffset(centerX, startY);
        contentStream.showText(title);
        contentStream.endText();

        // 타이틀 후 간격 증가!
        return startY - PurchaseOrderConstant.PDF_TITLE_SPACING;
    }

    /**
     * 헤더 정보 렌더링 (주문번호, 매장명, 거래처)
     */
    private float renderHeader(
            PDPageContentStream contentStream,
            PDType0Font regularFont,
            PurchaseOrder purchaseOrder,
            float startY
    ) throws IOException {
        float cursorY = startY;
        int fontSize = PurchaseOrderConstant.PDF_HEADER_FONT_SIZE;
        float x = PurchaseOrderConstant.PDF_MARGIN_LEFT;

        // 주문번호
        String orderNo = PurchaseOrderConstant.PDF_ORDER_NO_LABEL +
                toPrintable(purchaseOrder.getOrderNo());
        cursorY = writeLine(contentStream, regularFont, orderNo, fontSize, x, cursorY);

        // 매장명
        String storeName = PurchaseOrderConstant.PDF_STORE_NAME_LABEL +
                purchaseOrder.getStore().getName();
        cursorY = writeLine(contentStream, regularFont, storeName, fontSize, x, cursorY);

        // 거래처명 (있는 경우에만)
        if (purchaseOrder.getVendor() != null) {
            String vendorName = PurchaseOrderConstant.PDF_VENDOR_NAME_LABEL +
                    purchaseOrder.getVendor().getName();
            cursorY = writeLine(contentStream, regularFont, vendorName, fontSize, x, cursorY);
        }

        return cursorY - PurchaseOrderConstant.PDF_SECTION_SPACING;
    }

    /**
     * 테이블 렌더링
     */
    private float renderTable(
            PDPageContentStream contentStream,
            PDType0Font regularFont,
            PDType0Font boldFont,
            List<PurchaseOrderItem> items,
            float startY
    ) throws IOException {
        float tableWidth = PurchaseOrderConstant.PDF_PAGE_WIDTH -
                PurchaseOrderConstant.PDF_MARGIN_LEFT -
                PurchaseOrderConstant.PDF_MARGIN_RIGHT;
        float x = PurchaseOrderConstant.PDF_MARGIN_LEFT;
        float cursorY = startY;

        // 컬럼 너비 계산
        float col1Width = tableWidth * PurchaseOrderConstant.PDF_TABLE_COL_ITEM_NAME_RATIO;
        float col2Width = tableWidth * PurchaseOrderConstant.PDF_TABLE_COL_QUANTITY_RATIO;
        float col3Width = tableWidth * PurchaseOrderConstant.PDF_TABLE_COL_UNIT_PRICE_RATIO;
        float col4Width = tableWidth * PurchaseOrderConstant.PDF_TABLE_COL_LINE_AMOUNT_RATIO;

        // 테이블 헤더
        cursorY = drawTableHeader(contentStream, boldFont, x, cursorY, col1Width, col2Width, col3Width, col4Width, tableWidth);

        // 테이블 바디
        for (PurchaseOrderItem item : items) {
            cursorY = drawTableRow(
                    contentStream,
                    regularFont,
                    x,
                    cursorY,
                    col1Width,
                    col2Width,
                    col3Width,
                    col4Width,
                    tableWidth,
                    item.getItemName(),
                    String.valueOf(item.getQuantity()),
                    formatCurrency(item.getUnitPrice()),
                    formatCurrency(item.getLineAmount())
            );
        }

        // 테이블 하단 선
        drawLine(contentStream, x, cursorY, x + tableWidth, cursorY);

        return cursorY - PurchaseOrderConstant.PDF_SECTION_SPACING;
    }

    /**
     * 테이블 헤더 그리기
     */
    private float drawTableHeader(
            PDPageContentStream contentStream,
            PDType0Font boldFont,
            float x,
            float y,
            float col1Width,
            float col2Width,
            float col3Width,
            float col4Width,
            float tableWidth
    ) throws IOException {
        int fontSize = PurchaseOrderConstant.PDF_TABLE_HEADER_FONT_SIZE;
        float rowHeight = PurchaseOrderConstant.PDF_TABLE_HEADER_HEIGHT;

        // 상단 선
        drawLine(contentStream, x, y, x + tableWidth, y);

        // 헤더 텍스트
        float textY = y - rowHeight + PurchaseOrderConstant.PDF_TABLE_CELL_PADDING;

        contentStream.beginText();
        contentStream.setFont(boldFont, fontSize);

        // 품목명
        contentStream.newLineAtOffset(x + PurchaseOrderConstant.PDF_TABLE_CELL_PADDING, textY);
        contentStream.showText(PurchaseOrderConstant.PDF_TABLE_HEADER_ITEM_NAME);

        // 수량 (가운데 정렬)
        float qtyWidth = boldFont.getStringWidth(PurchaseOrderConstant.PDF_TABLE_HEADER_QUANTITY) / 1000 * fontSize;
        float qtyX = x + col1Width + (col2Width / 2) - (qtyWidth / 2);
        contentStream.newLineAtOffset(qtyX - (x + PurchaseOrderConstant.PDF_TABLE_CELL_PADDING), 0);
        contentStream.showText(PurchaseOrderConstant.PDF_TABLE_HEADER_QUANTITY);

        // 단가 (오른쪽 정렬)
        String unitPriceHeader = PurchaseOrderConstant.PDF_TABLE_HEADER_UNIT_PRICE;
        float unitPriceWidth = boldFont.getStringWidth(unitPriceHeader) / 1000 * fontSize;
        float unitPriceX = x + col1Width + col2Width + col3Width - unitPriceWidth -
                PurchaseOrderConstant.PDF_TABLE_CELL_PADDING;
        contentStream.newLineAtOffset(unitPriceX - qtyX, 0);
        contentStream.showText(unitPriceHeader);

        // 금액 (오른쪽 정렬)
        String lineAmountHeader = PurchaseOrderConstant.PDF_TABLE_HEADER_LINE_AMOUNT;
        float lineAmountWidth = boldFont.getStringWidth(lineAmountHeader) / 1000 * fontSize;
        float lineAmountX = x + tableWidth - lineAmountWidth - PurchaseOrderConstant.PDF_TABLE_CELL_PADDING;
        contentStream.newLineAtOffset(lineAmountX - unitPriceX, 0);
        contentStream.showText(lineAmountHeader);

        contentStream.endText();

        float newY = y - rowHeight;

        // 하단 선
        drawLine(contentStream, x, newY, x + tableWidth, newY);

        // 세로 구분선 (왼쪽부터 오른쪽으로)
        drawLine(contentStream, x, y, x, newY);  // ← 왼쪽 선 추가!
        drawLine(contentStream, x + col1Width, y, x + col1Width, newY);
        drawLine(contentStream, x + col1Width + col2Width, y, x + col1Width + col2Width, newY);
        drawLine(contentStream, x + col1Width + col2Width + col3Width, y, x + col1Width + col2Width + col3Width, newY);
        drawLine(contentStream, x + tableWidth, y, x + tableWidth, newY);  // ← 오른쪽 선 추가!

        return newY;
    }

    /**
     * 테이블 행 그리기
     */
    private float drawTableRow(
            PDPageContentStream contentStream,
            PDType0Font regularFont,
            float x,
            float y,
            float col1Width,
            float col2Width,
            float col3Width,
            float col4Width,
            float tableWidth,
            String itemName,
            String quantity,
            String unitPrice,
            String lineAmount
    ) throws IOException {
        int fontSize = PurchaseOrderConstant.PDF_TABLE_BODY_FONT_SIZE;
        float rowHeight = PurchaseOrderConstant.PDF_TABLE_ROW_HEIGHT;

        float textY = y - rowHeight + PurchaseOrderConstant.PDF_TABLE_CELL_PADDING;

        contentStream.beginText();
        contentStream.setFont(regularFont, fontSize);

        // 품목명 (왼쪽 정렬)
        contentStream.newLineAtOffset(x + PurchaseOrderConstant.PDF_TABLE_CELL_PADDING, textY);
        contentStream.showText(itemName);

        // 수량 (가운데 정렬)
        float qtyWidth = regularFont.getStringWidth(quantity) / 1000 * fontSize;
        float qtyX = x + col1Width + (col2Width / 2) - (qtyWidth / 2);
        contentStream.newLineAtOffset(qtyX - (x + PurchaseOrderConstant.PDF_TABLE_CELL_PADDING), 0);
        contentStream.showText(quantity);

        // 단가 (오른쪽 정렬)
        float unitPriceWidth = regularFont.getStringWidth(unitPrice) / 1000 * fontSize;
        float unitPriceX = x + col1Width + col2Width + col3Width - unitPriceWidth -
                PurchaseOrderConstant.PDF_TABLE_CELL_PADDING;
        contentStream.newLineAtOffset(unitPriceX - qtyX, 0);
        contentStream.showText(unitPrice);

        // 금액 (오른쪽 정렬)
        float lineAmountWidth = regularFont.getStringWidth(lineAmount) / 1000 * fontSize;
        float lineAmountX = x + tableWidth - lineAmountWidth - PurchaseOrderConstant.PDF_TABLE_CELL_PADDING;
        contentStream.newLineAtOffset(lineAmountX - unitPriceX, 0);
        contentStream.showText(lineAmount);

        contentStream.endText();

        float newY = y - rowHeight;

        // 하단 선
        drawLine(contentStream, x, newY, x + tableWidth, newY);

        // 세로 구분선 (왼쪽부터 오른쪽으로)
        drawLine(contentStream, x, y, x, newY);  // ← 왼쪽 선 추가!
        drawLine(contentStream, x + col1Width, y, x + col1Width, newY);
        drawLine(contentStream, x + col1Width + col2Width, y, x + col1Width + col2Width, newY);
        drawLine(contentStream, x + col1Width + col2Width + col3Width, y, x + col1Width + col2Width + col3Width, newY);
        drawLine(contentStream, x + tableWidth, y, x + tableWidth, newY);  // ← 오른쪽 선 추가!

        return newY;
    }

    /**
     * 총액 렌더링
     */
    private void renderTotal(
            PDPageContentStream contentStream,
            PDType0Font boldFont,
            PurchaseOrder purchaseOrder,
            float cursorY
    ) throws IOException {
        String totalText = PurchaseOrderConstant.PDF_TOTAL_LABEL +
                formatCurrency(purchaseOrder.getTotalAmount());

        int fontSize = PurchaseOrderConstant.PDF_HEADER_FONT_SIZE;

        // 오른쪽 정렬
        float textWidth = boldFont.getStringWidth(totalText) / 1000 * fontSize;
        float x = PurchaseOrderConstant.PDF_PAGE_WIDTH -
                PurchaseOrderConstant.PDF_MARGIN_RIGHT - textWidth;

        contentStream.beginText();
        contentStream.setFont(boldFont, fontSize);
        contentStream.newLineAtOffset(x, cursorY);
        contentStream.showText(totalText);
        contentStream.endText();
    }

    /**
     * 선 그리기
     */
    private void drawLine(PDPageContentStream contentStream, float x1, float y1, float x2, float y2) throws IOException {
        contentStream.moveTo(x1, y1);
        contentStream.lineTo(x2, y2);
        contentStream.stroke();
    }

    /**
     * 텍스트 한 줄 쓰기
     */
    private float writeLine(
            PDPageContentStream contentStream,
            PDType0Font font,
            String text,
            int fontSize,
            float x,
            float y
    ) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - PurchaseOrderConstant.PDF_LINE_HEIGHT;
    }

    /**
     * 금액 포맷팅 (₩ + 콤마)
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "₩0";
        }
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.KOREA);
        return "₩" + formatter.format(amount);
    }

    /**
     * null/빈 문자열 처리
     */
    private String toPrintable(String value) {
        if (value == null || value.isBlank()) {
            return PurchaseOrderConstant.PDF_NOT_AVAILABLE;
        }
        return value;
    }
}
