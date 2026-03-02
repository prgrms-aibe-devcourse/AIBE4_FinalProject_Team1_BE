package kr.inventory.domain.store.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inventory.domain.store.entity.StoreMember;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.entity.enums.StoreMemberStatus;
import kr.inventory.domain.store.repository.StoreMemberRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static kr.inventory.domain.store.entity.QStore.store;
import static kr.inventory.domain.store.entity.QStoreMember.storeMember;
import static kr.inventory.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class StoreMemberRepositoryImpl  implements StoreMemberRepositoryCustom  {
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
                                store.storePublicId.eq(publicId),
                                storeMember.status.eq(StoreMemberStatus.ACTIVE)
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<StoreMember> findAllByUserUserIdWithStore(Long userId) {
        return queryFactory
                .selectFrom(storeMember)
                .join(storeMember.store, store).fetchJoin()
                .where(storeMember.user.userId.eq(userId))
                .fetch();
    }

    @Override
    public List<StoreMember> findAllByStoreStoreIdWithUser(Long storeId) {
        return queryFactory
                .selectFrom(storeMember)
                .join(storeMember.user, user).fetchJoin()
                .where(storeMember.store.storeId.eq(storeId))
                .fetch();
    }

    @Override
    public boolean isStoreMember(Long storeId, Long userId) {
        Integer count = queryFactory
                .selectOne()
                .from(storeMember)
                .where(
                        storeMember.store.storeId.eq(storeId),
                        storeMember.user.userId.eq(userId),
                        storeMember.status.eq(StoreMemberStatus.ACTIVE)
                )
                .fetchFirst();
        return count != null;
    }

    @Override
    public List<StoreMember> findAllByStorePublicIdWithUser(UUID storePublicId) {
        return queryFactory
                .selectFrom(storeMember)
                .join(storeMember.user, user).fetchJoin()
                .join(storeMember.store, store)
                .where(store.storePublicId.eq(storePublicId))
                .fetch();
    }

    @Override
    public boolean isStoreMemberByPublicId(UUID storePublicId, Long userId) {
        Integer count = queryFactory
                .selectOne()
                .from(storeMember)
                .join(storeMember.store, store)
                .where(
                        store.storePublicId.eq(storePublicId),
                        storeMember.user.userId.eq(userId),
                        storeMember.status.eq(StoreMemberStatus.ACTIVE)
                )
                .fetchFirst();
        return count != null;
    }

    @Override
    public boolean hasRole(Long storeId, Long userId, StoreMemberRole role) {
        Integer count = queryFactory
                .selectOne()
                .from(storeMember)
                .where(
                        storeMember.store.storeId.eq(storeId),
                        storeMember.user.userId.eq(userId),
                        storeMember.status.eq(StoreMemberStatus.ACTIVE),
                        storeMember.role.eq(role)
                )
                .fetchFirst();
        return count != null;
    }

    @Override
    public boolean hasRoleByPublicId(UUID storePublicId, Long userId, StoreMemberRole role) {
        Integer count = queryFactory
                .selectOne()
                .from(storeMember)
                .join(storeMember.store, store)
                .where(
                        store.storePublicId.eq(storePublicId),
                        storeMember.user.userId.eq(userId),
                        storeMember.status.eq(StoreMemberStatus.ACTIVE),
                        storeMember.role.eq(role)
                )
                .fetchFirst();
        return count != null;
    }

    @Override
    public Integer findMaxDisplayOrderByUserUserId(Long userId) {
        Integer maxOrder = queryFactory
                .select(storeMember.displayOrder.max())
                .from(storeMember)
                .where(storeMember.user.userId.eq(userId))
                .fetchOne();
        return maxOrder != null ? maxOrder : -1;
    }

    @Override
    public long unsetAllDefaultsByUserId(Long userId) {
        return queryFactory
                .update(storeMember)
                .set(storeMember.isDefault, false)
                .where(
                        storeMember.user.userId.eq(userId),
                        storeMember.isDefault.isTrue()
                )
                .execute();
    }
}
