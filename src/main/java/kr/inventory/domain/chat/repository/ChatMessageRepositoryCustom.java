package kr.inventory.domain.chat.repository;

import java.util.List;
import kr.inventory.domain.chat.entity.ChatMessage;

public interface ChatMessageRepositoryCustom {

    List<ChatMessage> findMessagePage(Long threadId, Long cursor, int size);

    List<ChatMessage> findPromptMessages(Long threadId, Long upToMessageId, int size);

    List<Long> findThreadIdsHavingQueuedUserMessages(int limit);
}