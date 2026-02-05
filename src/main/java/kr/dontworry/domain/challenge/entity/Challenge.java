package kr.dontworry.domain.challenge.entity;

import co.elastic.clients.elasticsearch.watcher.ConditionType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kr.dontworry.domain.challenge.entity.enums.ChallengeCategory;
import kr.dontworry.domain.challenge.entity.enums.ChallengeDifficulty;
import kr.dontworry.domain.challenge.entity.enums.Tier;
import kr.dontworry.domain.challenge.entity.enums.TierSub;
import kr.dontworry.domain.common.CreatedAtEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Challenge extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long challengeId;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeDifficulty difficulty;

    @Column(nullable = false)
    private Long rewardXp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tier unlockTier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private TierSub unlockTierSub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ConditionType conditionType;

    private Long conditionTarget;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode conditionParams;

    @Column(nullable = false)
    private Boolean isStreak;

    @Column(nullable = false)
    private Boolean isHidden;

    public static Challenge create(
            String code,
            String name,
            String description,
            ChallengeCategory category,
            ChallengeDifficulty difficulty,
            Long rewardXp,
            Tier unlockTier,
            TierSub unlockTierSub,
            ConditionType conditionType,
            Boolean isStreak,
            Boolean isHidden
    ) {
        Challenge challenge = new Challenge();
        challenge.code = code;
        challenge.name = name;
        challenge.description = description;
        challenge.category = category;
        challenge.difficulty = difficulty;
        challenge.rewardXp = rewardXp;
        challenge.unlockTier = unlockTier;
        challenge.unlockTierSub = unlockTierSub;
        challenge.conditionType = conditionType;
        challenge.isStreak = isStreak;
        challenge.isHidden = isHidden;
        return challenge;
    }

    public void updateConditionTarget(Long target) {
        this.conditionTarget = target;
    }

    public void updateConditionParams(JsonNode params) {
        this.conditionParams = params;
    }

    public void updateRewardXp(Long xp) {
        this.rewardXp = xp;
    }
}
