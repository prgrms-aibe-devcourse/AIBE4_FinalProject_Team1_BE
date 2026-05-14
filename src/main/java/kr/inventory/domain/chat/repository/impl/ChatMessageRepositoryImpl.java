package kr.inventory.domain.chat.repository.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import kr.inventory.domain.chat.entity.ChatMessage;
import kr.inventory.domain.chat.entity.QChatMessage;
import kr.inventory.domain.chat.entity.enums.ChatMessageRole;
import kr.inventory.domain.chat.entity.enums.ChatMessageStatus;
import kr.inventory.domain.chat.repository.ChatMessageRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatMessage> findMessagePage(Long threadId, Long cursor, int size) {
        QChatMessage chatMessage = QChatMessage.chatMessage;

        BooleanBuilder builder = new BooleanBuilder()
                .and(chatMessage.thread.threadId.eq(threadId));

        if (cursor != null) {
            builder.and(chatMessage.messageId.lt(cursor));
        }

        return queryFactory
                .selectFrom(chatMessage)
                .where(builder)
                .orderBy(chatMessage.messageId.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<ChatMessage> findPromptMessages(Long threadId, Long upToMessageId, int size) {
        QChatMessage chatMessage = QChatMessage.chatMessage;

        return queryFactory
                .selectFrom(chatMessage)
                .where(
                        chatMessage.thread.threadId.eq(threadId),
                        chatMessage.messageId.loe(upToMessageId),
                        chatMessage.status.ne(ChatMessageStatus.FAILED),
                        chatMessage.status.ne(ChatMessageStatus.INTERRUPTED)
                )
                .orderBy(chatMessage.messageId.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<Long> findThreadIdsHavingQueuedUserMessages(int limit) {
        QChatMessage chatMessage = QChatMessage.chatMessage;

        return queryFactory
                .select(chatMessage.thread.threadId)
                .from(chatMessage)
                .where(
                        chatMessage.role.eq(ChatMessageRole.USER),
                        chatMessage.status.eq(ChatMessageStatus.QUEUED)
                )
                .groupBy(chatMessage.thread.threadId)
                .orderBy(chatMessage.messageId.min().asc())
                .limit(Math.max(1, limit))
                .fetch();
    }
}