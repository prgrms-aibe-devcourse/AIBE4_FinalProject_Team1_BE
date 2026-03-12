package kr.inventory.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.chat.controller.dto.request.ChatSendMessageRequest;
import kr.inventory.domain.chat.service.ChatCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatCommandService chatCommandService;

    @Operation(summary = "채팅 메시지 전송")
    @MessageMapping("/chat.send")
    public void send(
            @Valid ChatSendMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        chatCommandService.acceptUserMessage(
                principal.getUserId(),
                request.threadId(),
                request.clientMessageId(),
                request.content()
        );
    }
}