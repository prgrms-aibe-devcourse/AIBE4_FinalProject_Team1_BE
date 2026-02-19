package kr.inventory.domain.store.repository;

import kr.inventory.domain.store.entity.StoreMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreMemberRepository extends JpaRepository<StoreMember, Long>, StoreMemberRepositoryCustom {
}
