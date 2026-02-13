package kr.inventory.domain.store.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.domain.store.entity.enums.StoreStatus;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

import static kr.inventory.domain.store.entity.QStore.store;
import static kr.inventory.domain.store.entity.QStoreMember.storeMember;

@RequiredArgsConstructor
public class StoreMemberRepositoryImpl implements StoreMemberRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Long> findStoreIdByUserAndPublicId(Long userId, UUID publicId) {
        return Optional.ofNullable(
                queryFactory
                        .select(store.storeId)
                        .from(storeMember)
                        .join(storeMember.store, store)
                        .where(
                                storeMember.user.userId.eq(userId),
                                store.publicId.eq(publicId),
                                storeMember.status.eq(StoreMemberStatus.ACTIVE),
                                store.status.eq(StoreStatus.ACTIVE)
                        )
                        .fetchOne()
        );
    }
}
