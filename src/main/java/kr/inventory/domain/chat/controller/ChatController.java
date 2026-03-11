package kr.inventory.domain.chat.controller;

import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.chat.controller.dto.request.ChatRequest;
import kr.inventory.domain.chat.controller.dto.response.ChatResponse;
import kr.inventory.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService inventoryChatService;

    @PostMapping
    public ChatResponse chat(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid ChatRequest request
    ) {
        Long userId = principal.getUserId();
        String answer = inventoryChatService.answer(userId, request.message());
        return new ChatResponse(answer);
    }
}