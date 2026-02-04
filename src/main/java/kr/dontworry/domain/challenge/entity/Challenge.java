package kr.dontworry.domain.challenge.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kr.dontworry.domain.challenge.entity.enums.ChallengeStatus;
import kr.dontworry.domain.challenge.entity.enums.ChallengeType;
import kr.dontworry.domain.challenge.entity.enums.EvaluationPeriod;
import kr.dontworry.domain.common.AuditableEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Challenge extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long challengeId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeType type;

    @Column(nullable = false, length = 50)
    private String challengeCategory;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ChallengeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationPeriod evaluationPeriod;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode ruleJson;

    @Column(nullable = false)
    private Integer rewardExperience;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode rewardBadges;

    @Column(nullable = false)
    private Integer maxParticipants;

    public static Challenge create(
        String title,
        ChallengeType type,
        String challengeCategory,
        LocalDate startDate,
        LocalDate endDate,
        EvaluationPeriod evaluationPeriod,
        JsonNode ruleJson
    ) {
        Challenge challenge = new Challenge();
        challenge.title = title;
        challenge.type = type;
        challenge.challengeCategory = challengeCategory;
        challenge.startDate = startDate;
        challenge.endDate = endDate;
        challenge.evaluationPeriod = evaluationPeriod;
        challenge.ruleJson = ruleJson;
        challenge.status = ChallengeStatus.UPCOMING;
        challenge.rewardExperience = 0;
        challenge.maxParticipants = 0;
        return challenge;
    }
}
