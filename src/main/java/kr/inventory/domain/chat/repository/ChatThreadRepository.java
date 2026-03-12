package kr.inventory.domain.chat.repository;

import kr.inventory.domain.chat.entity.ChatThread;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Long>, ChatThreadRepositoryCustom {
}