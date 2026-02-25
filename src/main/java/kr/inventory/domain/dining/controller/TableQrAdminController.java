package kr.inventory.domain.dining.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.dining.controller.dto.TableQrIssueRequest;
import kr.inventory.domain.dining.controller.dto.TableQrIssueResponse;
import kr.inventory.domain.dining.controller.dto.TableQrRevokeResponse;
import kr.inventory.domain.dining.service.TableQrManagerFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "테이블 QR(Table QR)", description = "테이블 QR 발급/회수 등 관리자 기능 API입니다.")
@RestController
@RequestMapping("/api/dining/{storePublicId}/table-qrs")
@RequiredArgsConstructor
public class TableQrAdminController {

    private final TableQrManagerFacade tableQrManagerFacade;

    @Operation(
            summary = "테이블 QR 발급/로테이션",
            description = "특정 매장/테이블에 대한 entryToken을 발급하고 QR URL을 반환합니다. 기존 활성 QR이 있으면 revoke 후 새로 발급합니다."
    )
    @PostMapping("/issue")
    public ResponseEntity<TableQrIssueResponse> issue(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @RequestBody @Valid TableQrIssueRequest request
    ) {
        TableQrIssueResponse res = tableQrManagerFacade.issueOrRotate(
                principal.getUserId(),
                storePublicId,
                request.tablePublicId()
        );
        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "QR 회수(revoke)",
            description = "유출/훼손/교체 등의 사유로 특정 QR을 회수합니다."
    )
    @PostMapping("/{qrPublicId}/revoke")
    public ResponseEntity<TableQrRevokeResponse> revoke(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID storePublicId,
            @PathVariable UUID qrPublicId
    ) {
        tableQrManagerFacade.revoke(principal.getUserId(), storePublicId, qrPublicId);
        return ResponseEntity.ok(new TableQrRevokeResponse(qrPublicId, "QR이 폐기되었습니다."));
    }
}