package kr.inventory.domain.chat.repository;

import java.util.Optional;
import kr.inventory.domain.chat.entity.ChatMessage;
import kr.inventory.domain.chat.entity.enums.ChatMessageRole;
import kr.inventory.domain.chat.entity.enums.ChatMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {

    Optional<ChatMessage> findByThreadThreadIdAndClientMessageId(Long threadId, String clientMessageId);

    Optional<ChatMessage> findFirstByReplyToMessageIdOrderByMessageIdAsc(Long replyToMessageId);

    boolean existsByThreadThreadIdAndRoleAndStatus(Long threadId, ChatMessageRole role, ChatMessageStatus status);

    Optional<ChatMessage> findFirstByThreadThreadIdAndRoleAndStatusOrderByMessageIdAsc(Long threadId, ChatMessageRole role, ChatMessageStatus status);
}