package kr.inventory.domain.document.service.processor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.inventory.domain.document.service.GeminiService;
import kr.inventory.domain.document.service.mapper.OcrResultMapper;

@Component
public class ImageOcrProcessor extends AbstractGeminiOcrProcessor {

	public ImageOcrProcessor(GeminiService geminiService,
		ObjectMapper objectMapper, OcrPromptProvider ocrPromptProvider, OcrResultMapper ocrResultMapper) {
		super(geminiService, objectMapper, ocrPromptProvider, ocrResultMapper);
	}

	@Override
	public boolean supports(MultipartFile file) {
		String contentType = file.getContentType();
		return contentType != null && contentType.startsWith("image/");
	}

	@Override
	protected byte[] getOptimizedData(MultipartFile file) throws IOException {
		BufferedImage originalImage = ImageIO.read(file.getInputStream());

		int targetWidth = Math.min(originalImage.getWidth(), 1200);
		int targetHeight = (targetWidth * originalImage.getHeight()) / originalImage.getWidth();

		BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
		g.dispose();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(resizedImage, "jpg", baos);
		return baos.toByteArray();
	}

	@Override
	protected String getTargetContentType(MultipartFile file) {
		return "image/jpeg";
	}
}
