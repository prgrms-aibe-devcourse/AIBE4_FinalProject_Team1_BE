package kr.inventory.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.chat.controller.dto.request.ChatCreateThreadRequest;
import kr.inventory.domain.chat.controller.dto.response.ChatMessageResponse;
import kr.inventory.domain.chat.controller.dto.response.ChatThreadCreateResponse;
import kr.inventory.domain.chat.controller.dto.response.ChatThreadSummaryResponse;
import kr.inventory.domain.chat.service.ChatCommandService;
import kr.inventory.domain.chat.service.ChatQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "챗봇(Chat)", description = "챗봇 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatCommandService chatCommandService;
    private final ChatQueryService chatQueryService;

    @Operation(summary = "채팅 스레드 생성")
    @PostMapping("/{storePublicId}/threads")
    public ResponseEntity<ChatThreadCreateResponse> createThread(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid ChatCreateThreadRequest request
    ) {
        ChatThreadCreateResponse response = chatCommandService.createThread(
                principal.getUserId(),
                request.title(),
                storePublicId
        );

        return ResponseEntity
                .created(URI.create("/api/chat/threads/" + response.threadId()))
                .body(response);
    }

    @Operation(summary = "내 채팅 스레드 목록 조회")
    @GetMapping("/{storePublicId}/threads")
    public ResponseEntity<List<ChatThreadSummaryResponse>> myThreads(
            @PathVariable UUID storePublicId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        List<ChatThreadSummaryResponse> response = chatQueryService.getMyThreads(principal.getUserId(), storePublicId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "채팅 스레드 메시지 조회")
    @GetMapping("/threads/{threadId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> messages(
            @PathVariable Long threadId,
            @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        List<ChatMessageResponse> response = chatQueryService.getMessages(
                principal.getUserId(),
                threadId,
                cursor
        );

        return ResponseEntity.ok(response);
    }
}