package kr.inventory.domain.store.service;

import kr.inventory.domain.store.exception.StoreErrorCode;
import kr.inventory.domain.store.exception.StoreException;
import kr.inventory.domain.store.repository.StoreMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreAccessValidator {

    private final StoreMemberRepository storeMemberRepository;

    public Long validateAndGetStoreId(Long userId, UUID publicId) {
        return storeMemberRepository.findStoreIdByUserAndPublicId(userId, publicId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND_OR_ACCESS_DENIED));
    }
}