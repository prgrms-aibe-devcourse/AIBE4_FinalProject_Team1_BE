package kr.dontworry.domain.ocr.controller;

import kr.dontworry.domain.ocr.controller.dto.OcrResultResponse;
import kr.dontworry.domain.ocr.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OcrResultResponse> processOcr(
            @RequestPart("files") List<MultipartFile> files
    ) {
        OcrResultResponse response = ocrService.processOcr(files);
        return ResponseEntity.ok(response);
    }
}
