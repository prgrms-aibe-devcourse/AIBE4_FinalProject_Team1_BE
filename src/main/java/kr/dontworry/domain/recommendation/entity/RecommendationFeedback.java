package kr.dontworry.domain.recommendation.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kr.dontworry.domain.category.entity.Category;
import kr.dontworry.domain.common.CreatedAtEntity;
import kr.dontworry.domain.entry.entity.Entry;
import kr.dontworry.domain.ledger.entity.Ledger;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "recommendation_feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationFeedback extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feedbackId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private Entry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private RecommendationResult recommendation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode finalTagsJson;

    @Column(nullable = false)
    private Boolean isAccepted;

    public static RecommendationFeedback create(
        User user,
        Ledger ledger,
        Entry entry,
        RecommendationResult recommendation,
        Boolean isAccepted
    ) {
        RecommendationFeedback feedback = new RecommendationFeedback();
        feedback.user = user;
        feedback.ledger = ledger;
        feedback.entry = entry;
        feedback.recommendation = recommendation;
        feedback.isAccepted = isAccepted;
        return feedback;
    }
}
