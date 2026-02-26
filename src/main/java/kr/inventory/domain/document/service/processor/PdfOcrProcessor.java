package kr.inventory.domain.document.service.processor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.inventory.domain.document.service.GeminiService;

@Component
public class PdfOcrProcessor extends AbstractGeminiOcrProcessor {

	public PdfOcrProcessor(GeminiService geminiService,
		ObjectMapper objectMapper, OcrPromptProvider ocrPromptProvider) {
		super(geminiService, objectMapper, ocrPromptProvider);
	}

	@Override
	public boolean supports(MultipartFile file) {
		String contentType = file.getContentType();
		return contentType != null && contentType.equals("application/pdf");
	}

	@Override
	protected byte[] getOptimizedData(MultipartFile file) throws IOException {
		try (PDDocument document = Loader.loadPDF(file.getBytes())) {
			PDFRenderer pdfRenderer = new PDFRenderer(document);
			BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 72);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bim, "jpg", baos);
			return baos.toByteArray();
		}
	}

	@Override
	protected String getTargetContentType(MultipartFile file) {
		return "image/jpeg";
	}
}
