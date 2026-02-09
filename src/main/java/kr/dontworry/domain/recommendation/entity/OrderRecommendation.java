package kr.dontworry.domain.recommendation.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.recommendation.entity.enums.PolicyMode;
import kr.dontworry.domain.recommendation.entity.enums.RecommendationStatus;
import kr.dontworry.domain.store.entity.Store;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "order_recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderRecommendation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private LocalDate targetStartDate;

    @Column(nullable = false)
    private LocalDate targetEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status;

    @Column(precision = 3, scale = 2)
    private BigDecimal safetyStockFactor;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PolicyMode policyMode;

    @Column(columnDefinition = "TEXT")
    private String overallSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedByUser;

    private OffsetDateTime approvedAt;

    private OffsetDateTime expiresAt;

    public static OrderRecommendation create(
            Store store,
            LocalDate targetStartDate,
            LocalDate targetEndDate
    ) {
        OrderRecommendation recommendation = new OrderRecommendation();
        recommendation.store = store;
        recommendation.targetStartDate = targetStartDate;
        recommendation.targetEndDate = targetEndDate;
        recommendation.status = RecommendationStatus.GENERATED;
        return recommendation;
    }
}
