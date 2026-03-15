package kr.inventory.ai.context;

import kr.inventory.ai.context.dto.ChatToolContext;
import org.springframework.stereotype.Component;

@Component
public class ChatToolContextProvider {

    private static final ThreadLocal<ChatToolContext> CONTEXT = new ThreadLocal<>();

    public void set(ChatToolContext context) {
        CONTEXT.set(context);
    }

    public ChatToolContext getRequired() {
        ChatToolContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("채팅 도구 실행 컨텍스트가 없습니다.");
        }
        return context;
    }

    public void clear() {
        CONTEXT.remove();
    }
}