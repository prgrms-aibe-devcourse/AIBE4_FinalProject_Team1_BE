package kr.inventory.domain.chat.repository;

import java.util.Optional;
import kr.inventory.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {

    Optional<ChatMessage> findByThreadThreadIdAndClientMessageId(Long threadId, String clientMessageId);

    Optional<ChatMessage> findFirstByReplyToMessageIdOrderByMessageIdAsc(Long replyToMessageId);
}