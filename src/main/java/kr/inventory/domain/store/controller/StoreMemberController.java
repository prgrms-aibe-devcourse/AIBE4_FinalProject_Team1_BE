package kr.inventory.domain.store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.store.controller.dto.request.*;
import kr.inventory.domain.store.controller.dto.response.*;
import kr.inventory.domain.store.service.StoreMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores/{storeId}/members")
@Tag(name = "Store Member", description = "매장 멤버")
@RequiredArgsConstructor
public class StoreMemberController {

    private final StoreMemberService storeMemberService;

    @Operation(summary = "매장 멤버 목록 조회")
    @GetMapping
    public ResponseEntity<List<StoreMemberResponse>> getStoreMembers(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long storeId
    ) {
        List<StoreMemberResponse> members =
                storeMemberService.getStoreMembers(principal.getUserId(), storeId);
        return ResponseEntity.ok(members);
    }

    @Operation(summary = "매장 멤버 단건 조회")
    @GetMapping("/{memberId}")
    public ResponseEntity<StoreMemberResponse> getStoreMember(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long storeId,
            @PathVariable Long memberId
    ) {
        StoreMemberResponse response =
                storeMemberService.getStoreMember(principal.getUserId(), storeId, memberId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "매장 멤버 상태 변경")
    @PatchMapping("/{memberId}/status")
    public ResponseEntity<StoreMemberResponse> updateMemberStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long storeId,
            @PathVariable Long memberId,
            @Valid @RequestBody MemberStatusUpdateRequest request
    ) {
        StoreMemberResponse response =
                storeMemberService.updateMemberStatus(principal.getUserId(), storeId, memberId, request);
        return ResponseEntity.ok(response);
    }
}