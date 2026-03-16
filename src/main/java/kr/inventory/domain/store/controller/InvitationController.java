package kr.inventory.domain.store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.store.controller.dto.request.InvitationAcceptRequest;
import kr.inventory.domain.store.controller.dto.response.InvitationAcceptResponse;
import kr.inventory.domain.store.controller.dto.response.InvitationCreateResponse;
import kr.inventory.domain.store.controller.dto.response.InvitationItemResponse;
import kr.inventory.domain.store.permission.PermissionCode;
import kr.inventory.domain.store.permission.RequirePermission;
import kr.inventory.domain.store.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "초대(Invitation)", description = "멤버 초대 API")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @Operation(
        summary = "초대 생성/갱신",
        description = "OWNER만 가능. 매장당 1개의 초대만 존재하며, 재발급 시 기존 초대가 자동으로 갱신됩니다. 초대는 생성 후 30분간 유효합니다."
    )
    @RequirePermission(PermissionCode.INVITE_ISSUE)
    @PostMapping("/stores/{storePublicId}/invitations")
    public ResponseEntity<InvitationCreateResponse> createInvitation(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID storePublicId
    ) {
        InvitationCreateResponse response = invitationService.createInvitation(principal.getUserId(), storePublicId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "초대 수락",
        description = "token 또는 코드로 초대를 수락합니다. 둘 중 하나만 입력해야 합니다. 초대는 만료 전까지 여러 번 사용 가능합니다."
    )
    @PostMapping("/invitations/accept")
    public ResponseEntity<InvitationAcceptResponse> acceptInvitation(
        @AuthenticationPrincipal CustomUserDetails principal,
        @RequestBody InvitationAcceptRequest request
    ) {
        InvitationAcceptResponse response = invitationService.acceptInvitation(
            principal.getUserId(),
            request
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "현재 초대 조회")
    @RequirePermission(PermissionCode.INVITE_ISSUE)
    @GetMapping("/stores/{storePublicId}/invitations/active")
    public ResponseEntity<InvitationItemResponse> getActiveInvitation(
        @PathVariable UUID storePublicId
    ) {
        InvitationItemResponse response = invitationService.getActiveInvitation(storePublicId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "현재 초대 취소")
    @RequirePermission(PermissionCode.INVITE_ISSUE)
    @DeleteMapping("/stores/{storePublicId}/invitations/active")
    public ResponseEntity<Void> revokeActiveInvitation(
        @PathVariable UUID storePublicId
    ) {
        invitationService.revokeActiveInvitation(storePublicId);
        return ResponseEntity.noContent().build();
    }
}
