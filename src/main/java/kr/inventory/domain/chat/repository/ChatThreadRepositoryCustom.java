package kr.inventory.domain.chat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.inventory.domain.chat.controller.dto.response.ChatThreadSummaryResponse;
import kr.inventory.domain.chat.entity.ChatThread;
import kr.inventory.domain.user.entity.User;

public interface ChatThreadRepositoryCustom {

    List<ChatThreadSummaryResponse> findSummariesByUser(User user, UUID storePublicId);

    Optional<ChatThread> findActiveThreadByIdAndUser(Long threadId, User user);
}