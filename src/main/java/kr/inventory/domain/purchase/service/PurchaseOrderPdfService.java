package kr.inventory.domain.purchase.service;

import kr.inventory.domain.purchase.constant.PurchaseOrderConstant;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.PurchaseOrderItem;
import kr.inventory.domain.purchase.exception.PurchaseOrderErrorCode;
import kr.inventory.domain.purchase.exception.PurchaseOrderException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class PurchaseOrderPdfService {

    public byte[] generate(PurchaseOrder purchaseOrder) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = createPage(document);
            PDType1Font defaultFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            renderPageContent(document, page, defaultFont, purchaseOrder);

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.PDF_GENERATION_FAILED);
        }
    }

    private void renderPageContent(PDDocument document, PDPage page, PDType1Font defaultFont, PurchaseOrder purchaseOrder) throws IOException {
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float cursorY = renderHeader(contentStream, defaultFont, purchaseOrder, PurchaseOrderConstant.PDF_START_Y);
            cursorY = renderItems(contentStream, defaultFont, purchaseOrder, cursorY);
            renderTotal(contentStream, defaultFont, purchaseOrder, cursorY);
        }
    }

    private PDPage createPage(PDDocument document) {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return page;
    }

    private float renderHeader(PDPageContentStream contentStream, PDType1Font defaultFont, PurchaseOrder purchaseOrder, float startY) throws IOException {
        float cursorY = startY;
        cursorY = writeLine(contentStream, defaultFont, PurchaseOrderConstant.PDF_TITLE, PurchaseOrderConstant.PDF_TITLE_FONT_SIZE, PurchaseOrderConstant.PDF_START_X, cursorY);
        cursorY = writeLine(contentStream, defaultFont, PurchaseOrderConstant.PDF_ORDER_NO_LABEL + toPrintable(purchaseOrder.getOrderNo()), PurchaseOrderConstant.PDF_BODY_FONT_SIZE, PurchaseOrderConstant.PDF_START_X, cursorY);
        cursorY = writeLine(contentStream, defaultFont, PurchaseOrderConstant.PDF_STATUS_LABEL + purchaseOrder.getStatus(), PurchaseOrderConstant.PDF_BODY_FONT_SIZE, PurchaseOrderConstant.PDF_START_X, cursorY);
        cursorY = writeLine(contentStream, defaultFont, PurchaseOrderConstant.PDF_STORE_ID_LABEL + purchaseOrder.getStore().getStoreId(), PurchaseOrderConstant.PDF_BODY_FONT_SIZE, PurchaseOrderConstant.PDF_START_X, cursorY);
        return writeLine(contentStream, defaultFont, "", PurchaseOrderConstant.PDF_BODY_FONT_SIZE, PurchaseOrderConstant.PDF_START_X, cursorY);
    }

    private float renderItems(PDPageContentStream contentStream, PDType1Font defaultFont, PurchaseOrder purchaseOrder, float startY) throws IOException {
        float cursorY = writeLine(contentStream, defaultFont, PurchaseOrderConstant.PDF_ITEMS_HEADER, PurchaseOrderConstant.PDF_SECTION_FONT_SIZE, PurchaseOrderConstant.PDF_START_X, startY);
        for (PurchaseOrderItem item : purchaseOrder.getItems()) {
            cursorY = writeLine(contentStream, defaultFont, toItemRow(item), PurchaseOrderConstant.PDF_ITEM_FONT_SIZE, PurchaseOrderConstant.PDF_START_X, cursorY);
        }
        return cursorY;
    }

    private void renderTotal(PDPageContentStream contentStream, PDType1Font defaultFont, PurchaseOrder purchaseOrder, float cursorY) throws IOException {
        writeLine(
                contentStream,
                defaultFont,
                PurchaseOrderConstant.PDF_TOTAL_LABEL + purchaseOrder.getTotalAmount(),
                PurchaseOrderConstant.PDF_BODY_FONT_SIZE,
                PurchaseOrderConstant.PDF_START_X,
                cursorY - PurchaseOrderConstant.PDF_TOTAL_SPACING
        );
    }

    private String toItemRow(PurchaseOrderItem item) {
        return "- " + item.getItemName()
                + " / qty=" + item.getQuantity()
                + " / unit=" + item.getUnitPrice()
                + " / line=" + item.getLineAmount();
    }

    private float writeLine(PDPageContentStream contentStream, PDType1Font defaultFont, String text, int fontSize, float x, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(defaultFont, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - PurchaseOrderConstant.PDF_LINE_HEIGHT;
    }

    private String toPrintable(String value) {
        if (value == null || value.isBlank()) {
            return PurchaseOrderConstant.PDF_NOT_AVAILABLE;
        }
        return value;
    }
}
