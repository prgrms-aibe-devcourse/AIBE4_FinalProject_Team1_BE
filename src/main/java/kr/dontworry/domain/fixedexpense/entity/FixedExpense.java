package kr.dontworry.domain.fixedexpense.entity;

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

@Entity
@Table(name = "fixed_expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FixedExpense extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fixedExpenseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Long defaultAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode ruleJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode exceptionsJson;

    @Column(nullable = false)
    private Boolean isActive;

    public static FixedExpense create(
        Ledger ledger,
        Category category,
        String name,
        Long defaultAmount,
        JsonNode ruleJson
    ) {
        FixedExpense expense = new FixedExpense();
        expense.ledger = ledger;
        expense.category = category;
        expense.name = name;
        expense.defaultAmount = defaultAmount;
        expense.ruleJson = ruleJson;
        expense.isActive = true;
        return expense;
    }
}
