package kr.dontworry.domain.challenge.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_game_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGameStats extends AuditableEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Integer totalExperience;

    @Column(nullable = false)
    private Integer currentLevelExp;

    @Column(nullable = false)
    private Integer completedChallenges;

    @Column(nullable = false)
    private Integer currentStreak;

    @Column(nullable = false)
    private Integer maxStreak;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode badges;

    public static UserGameStats create(User user) {
        UserGameStats stats = new UserGameStats();
        stats.user = user;
        stats.level = 1;
        stats.totalExperience = 0;
        stats.currentLevelExp = 0;
        stats.completedChallenges = 0;
        stats.currentStreak = 0;
        stats.maxStreak = 0;
        return stats;
    }
}
