package kr.dontworry.domain.ledger.repository;

import kr.dontworry.domain.ledger.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<Ledger, Long>, LedgerRepositoryCustom {
}
