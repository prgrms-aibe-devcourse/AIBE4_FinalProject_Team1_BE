package kr.inventory.domain.store.service;

import kr.inventory.domain.store.repository.StoreMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreMemberQueryService {

    private final StoreMemberRepository storeMemberRepository;

    public List<Long> findActiveMemberUserIds(Long storeId) {
        return storeMemberRepository.findActiveUserIdsByStoreId(storeId);
    }
}
