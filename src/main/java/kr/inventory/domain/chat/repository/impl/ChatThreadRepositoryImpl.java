package kr.inventory.domain.chat.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import kr.inventory.domain.chat.controller.dto.response.ChatThreadSummaryResponse;
import kr.inventory.domain.chat.entity.ChatThread;
import kr.inventory.domain.chat.entity.QChatMessage;
import kr.inventory.domain.chat.entity.QChatThread;
import kr.inventory.domain.chat.entity.enums.ChatThreadStatus;
import kr.inventory.domain.chat.repository.ChatThreadRepositoryCustom;
import kr.inventory.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatThreadRepositoryImpl implements ChatThreadRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ChatThreadSummaryResponse> findSummariesByUser(User user,UUID storePublicId) {
        QChatThread chatThread = QChatThread.chatThread;
        QChatMessage chatMessage = QChatMessage.chatMessage;
        QChatMessage lastMessage = new QChatMessage("lastMessage");

        return queryFactory
                .select(Projections.constructor(
                        ChatThreadSummaryResponse.class,
                        chatThread.threadId,
                        chatThread.storePublicId,
                        chatThread.title,
                        chatThread.status,
                        chatMessage.content.coalesce(""),
                        chatThread.lastMessageAt,
                        chatThread.createdAt
                ))
                .from(chatThread)
                .leftJoin(chatMessage).on(
                        chatMessage.messageId.eq(
                                JPAExpressions
                                        .select(lastMessage.messageId.max())
                                        .from(lastMessage)
                                        .where(lastMessage.thread.eq(chatThread))
                        )
                )
                .where(
                        chatThread.user.eq(user),
                        chatThread.storePublicId.eq(storePublicId),
                        chatThread.status.eq(ChatThreadStatus.ACTIVE)
                )
                .orderBy(chatThread.lastMessageAt.desc(), chatThread.threadId.desc())
                .fetch();
    }


    @Override
    public Optional<ChatThread> findActiveThreadByIdAndUserForUpdate(Long threadId, User user) {
        QChatThread chatThread = QChatThread.chatThread;

        ChatThread result = queryFactory
                .selectFrom(chatThread)
                .where(
                        chatThread.threadId.eq(threadId),
                        chatThread.user.eq(user),
                        chatThread.status.eq(ChatThreadStatus.ACTIVE)
                )
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<ChatThread> findActiveThreadByIdAndUser(Long threadId, User user) {
        QChatThread chatThread = QChatThread.chatThread;

        ChatThread result = queryFactory
                .selectFrom(chatThread)
                .where(
                        chatThread.threadId.eq(threadId),
                        chatThread.user.eq(user),
                        chatThread.status.eq(ChatThreadStatus.ACTIVE)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }
}