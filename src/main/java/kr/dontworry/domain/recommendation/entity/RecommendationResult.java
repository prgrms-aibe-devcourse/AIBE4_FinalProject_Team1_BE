package kr.dontworry.domain.recommendation.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kr.dontworry.domain.category.entity.Category;
import kr.dontworry.domain.common.CreatedAtEntity;
import kr.dontworry.domain.entry.entity.Entry;
import kr.dontworry.domain.ledger.entity.Ledger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "recommendation_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationResult extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private Entry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 20)
    private String engineType;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode recommendedTagsJson;

    @Column(columnDefinition = "text")
    private String reasonText;

    public static RecommendationResult create(
        Ledger ledger,
        Entry entry,
        String engineType,
        BigDecimal confidenceScore
    ) {
        RecommendationResult result = new RecommendationResult();
        result.ledger = ledger;
        result.entry = entry;
        result.engineType = engineType;
        result.confidenceScore = confidenceScore;
        return result;
    }
}
