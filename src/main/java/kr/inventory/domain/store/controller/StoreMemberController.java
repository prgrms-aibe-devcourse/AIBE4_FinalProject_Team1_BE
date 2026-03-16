package kr.inventory.domain.store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.store.controller.dto.request.*;
import kr.inventory.domain.store.controller.dto.response.*;
import kr.inventory.domain.store.permission.PermissionCode;
import kr.inventory.domain.store.permission.RequirePermission;
import kr.inventory.domain.store.service.StoreMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storePublicId}/members")
@Tag(name = "매장 멤버(Store Member)", description = "매장 멤버 관리 API")
@RequiredArgsConstructor
public class StoreMemberController {

    private final StoreMemberService storeMemberService;

    @Operation(summary = "매장 멤버 목록 조회")
    @RequirePermission(PermissionCode.MEMBER_MANAGE)
    @GetMapping
    public ResponseEntity<List<StoreMemberResponse>> getStoreMembers(
            @PathVariable UUID storePublicId
    ) {
        List<StoreMemberResponse> members =
                storeMemberService.getStoreMembers(storePublicId);
        return ResponseEntity.ok(members);
    }

    @Operation(summary = "매장 멤버 단건 조회")
    @RequirePermission(PermissionCode.MEMBER_MANAGE)
    @GetMapping("/{memberId}")
    public ResponseEntity<StoreMemberResponse> getStoreMember(
            @PathVariable UUID storePublicId,
            @PathVariable Long memberId
    ) {
        StoreMemberResponse response =
                storeMemberService.getStoreMember(storePublicId, memberId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "매장 멤버 상태 변경")
    @RequirePermission(PermissionCode.MEMBER_MANAGE)
    @PatchMapping("/{memberId}/status")
    public ResponseEntity<StoreMemberResponse> updateMemberStatus(
            @PathVariable UUID storePublicId,
            @PathVariable Long memberId,
            @Valid @RequestBody MemberStatusUpdateRequest request
    ) {
        StoreMemberResponse response =
                storeMemberService.updateMemberStatus(storePublicId, memberId, request);
        return ResponseEntity.ok(response);
    }
}
