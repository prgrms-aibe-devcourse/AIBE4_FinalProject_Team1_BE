package kr.dontworry.domain.challenge.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kr.dontworry.domain.challenge.entity.enums.ParticipationStatus;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "challenge_participations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_participation_challenge_user", columnNames = {"challenge_id", "user_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipationStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode currentProgress;

    @Column(nullable = false)
    private Integer currentStreak;

    @Column(nullable = false)
    private Integer maxStreak;

    private LocalDate lastAchievementDate;

    @Column(precision = 5, scale = 4)
    private BigDecimal contributionRate;

    @Column(nullable = false)
    private Integer earnedExperience;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode earnedBadges;

    @Column(nullable = false)
    private OffsetDateTime joinedAt;

    private OffsetDateTime completedAt;

    @Version
    @Column(nullable = false)
    private Integer version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode finalReport;

    public static ChallengeParticipation create(Challenge challenge, User user) {
        ChallengeParticipation participation = new ChallengeParticipation();
        participation.challenge = challenge;
        participation.user = user;
        participation.status = ParticipationStatus.ACTIVE;
        participation.currentStreak = 0;
        participation.maxStreak = 0;
        participation.earnedExperience = 0;
        participation.joinedAt = OffsetDateTime.now();
        participation.version = 0;
        return participation;
    }
}
