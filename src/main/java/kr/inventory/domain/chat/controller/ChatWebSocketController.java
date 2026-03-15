package kr.inventory.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.security.Principal;
import kr.inventory.domain.chat.controller.dto.request.ChatSendMessageRequest;
import kr.inventory.domain.chat.service.ChatCommandService;
import kr.inventory.global.auth.util.PrincipalUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatCommandService chatCommandService;

    @Operation(summary = "채팅 메시지 전송")
    @MessageMapping("/chat.send")
    public void send(
            @Payload @Valid ChatSendMessageRequest request,
            Principal principal
    ) {
        chatCommandService.acceptUserMessage(
                PrincipalUtils.extractUserId(principal),
                request.threadId(),
                request.clientMessageId(),
                request.content()
        );
    }
}