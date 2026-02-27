package kr.inventory.domain.dining.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.dining.controller.dto.request.TableQrsIssueRequest;
import kr.inventory.domain.dining.controller.dto.response.TableQrIssueResponse;
import kr.inventory.domain.dining.service.TableQrManagerFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "테이블 QR(Table QR)", description = "테이블 QR 발급/회수 등 관리자 기능 API입니다.")
@RestController
@RequestMapping("/api/dining/{storePublicId}/table-qrs")
@RequiredArgsConstructor
public class TableQrAdminController {

    private final TableQrManagerFacade tableQrManagerFacade;

    @Operation(
            summary = "테이블 QR 발급",
            description = "선택된 여러 테이블의 QR을 한 번에 생성합니다."
    )
    @PostMapping("/issue")
    public ResponseEntity<List<TableQrIssueResponse>> issue(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @RequestBody @Valid TableQrsIssueRequest request
    ) {
        List<TableQrIssueResponse> res = tableQrManagerFacade.issueTableQrs(
                principal.getUserId(),
                storePublicId,
                request.tablePublicIds()
        );
        return ResponseEntity.ok(res);
    }
}