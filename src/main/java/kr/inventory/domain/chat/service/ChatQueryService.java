package kr.inventory.domain.chat.service;

import jakarta.persistence.EntityManager;
import java.util.List;
import kr.inventory.domain.chat.constant.ChatConstants;
import kr.inventory.domain.chat.controller.dto.response.ChatMessageResponse;
import kr.inventory.domain.chat.controller.dto.response.ChatThreadSummaryResponse;
import kr.inventory.domain.chat.exception.ChatErrorCode;
import kr.inventory.domain.chat.exception.ChatException;
import kr.inventory.domain.chat.repository.ChatMessageRepository;
import kr.inventory.domain.chat.repository.ChatThreadRepository;
import kr.inventory.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatQueryService {

    private final EntityManager entityManager;
    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public List<ChatThreadSummaryResponse> getMyThreads(Long userId) {
        User userReference = entityManager.getReference(User.class, userId);
        return chatThreadRepository.findSummariesByUser(userReference);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long userId, Long threadId, Long rawCursor) {
        Long cursor = normalizeCursor(rawCursor);
        User userReference = entityManager.getReference(User.class, userId);

        chatThreadRepository.findActiveThreadByIdAndUser(threadId, userReference)
                .orElseThrow(() -> new ChatException(ChatErrorCode.THREAD_NOT_FOUND));

        return chatMessageRepository.findMessagePage(threadId, cursor, ChatConstants.MESSAGE_PAGE_SIZE)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    private Long normalizeCursor(Long cursor) {
        if (cursor == null) {
            return null;
        }

        if (cursor < 1L) {
            throw new ChatException(ChatErrorCode.CURSOR_INVALID);
        }

        return cursor;
    }
}