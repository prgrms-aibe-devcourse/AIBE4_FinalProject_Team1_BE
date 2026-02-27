package kr.inventory.domain.document.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inventory.global.config.infrastructure.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File Test API", description = "S3 파일 업로드/다운로드 테스트용 API")
public class FileTestController {

    private final S3StorageService s3StorageService;

    @Operation(
            summary = "파일 업로드 테스트",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = FileUploadRequest.class)
                    )
            )
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestPart("file") MultipartFile file
    ) {
        String fileUrl = s3StorageService.upload(file, "test");

        Map<String, String> response = new HashMap<>();
        response.put("message", "File uploaded successfully");
        response.put("url", fileUrl);
        response.put("fileName", file.getOriginalFilename());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "파일 다운로드 URL 생성")
    @GetMapping("/download")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @RequestParam("key") String key
    ) {
        String presignedUrl = s3StorageService.getPresignedUrl(key);

        Map<String, String> response = new HashMap<>();
        response.put("presignedUrl", presignedUrl);
        response.put("expiresIn", "10 minutes");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "파일 삭제 테스트")
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteFile(
            @RequestParam("url") String fileUrl
    ) {
        s3StorageService.delete(fileUrl);

        Map<String, String> response = new HashMap<>();
        response.put("message", "File deleted successfully");
        response.put("deletedUrl", fileUrl);

        return ResponseEntity.ok(response);
    }

    static class FileUploadRequest {
        @Schema(type = "string", format = "binary")
        public MultipartFile file;
    }
}