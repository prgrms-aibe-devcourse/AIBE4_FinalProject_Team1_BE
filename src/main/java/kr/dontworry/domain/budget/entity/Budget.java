package kr.dontworry.domain.budget.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kr.dontworry.domain.category.entity.Category;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.ledger.entity.Ledger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "budgets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long budgetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private LocalDate yearMonth;

    @Column(nullable = false)
    private Long totalBudgetAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode categoryBudgetsJson;

    @Version
    @Column(nullable = false)
    private Long version;

    public static Budget create(
        Ledger ledger,
        LocalDate yearMonth,
        Long totalBudgetAmount
    ) {
        Budget budget = new Budget();
        budget.ledger = ledger;
        budget.yearMonth = yearMonth;
        budget.totalBudgetAmount = totalBudgetAmount;
        budget.version = 0L;
        return budget;
    }
}
