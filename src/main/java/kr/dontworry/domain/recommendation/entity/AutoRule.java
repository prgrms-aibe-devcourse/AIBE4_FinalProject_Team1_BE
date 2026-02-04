package kr.dontworry.domain.recommendation.entity;

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
@Table(name = "auto_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AutoRule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ruleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer priorityOrder;

    @Column(nullable = false)
    private Boolean isEnabled;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode conditionsJson;

    public static AutoRule create(
        Ledger ledger,
        Category category,
        String name,
        Integer priorityOrder,
        JsonNode conditionsJson
    ) {
        AutoRule rule = new AutoRule();
        rule.ledger = ledger;
        rule.category = category;
        rule.name = name;
        rule.priorityOrder = priorityOrder;
        rule.conditionsJson = conditionsJson;
        rule.isEnabled = true;
        return rule;
    }
}
