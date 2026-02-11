package kr.inventory.domain.document.service.processor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ExcelOcrProcessor extends AbstractGeminiOcrProcessor {

	@Override
	public boolean supports(MultipartFile file) {
		String contentType = file.getContentType();
		return contentType != null && (
			contentType.equals("application/vnd.ms-excel") ||
				contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
		);
	}

	@Override
	protected byte[] getOptimizedData(MultipartFile file) throws IOException {
		StringBuilder excelText = new StringBuilder();
		try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);
			for (Row row : sheet) {
				for (Cell cell : row) {
					excelText.append(cell.toString()).append("\t");
				}
				excelText.append("\n");
			}
		}
		return excelText.toString().getBytes(StandardCharsets.UTF_8);
	}

	@Override
	protected String getTargetContentType(MultipartFile file) {
		return "test/plain";
	}
}
