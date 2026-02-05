package kr.dontworry.domain.challenge.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.challenge.entity.enums.Tier;
import kr.dontworry.domain.challenge.entity.enums.TierSub;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private TierSub tierSub;

    @Column(nullable = false)
    private Long totalXp;

    @Column(nullable = false)
    private Long recoveryTickets;

    @Column(nullable = false)
    private Long recoveryUsedCount;

    private OffsetDateTime lastRecoveryUsedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representative_badge_id")
    private Badge representativeBadge;

    @Column(nullable = false)
    private Long totalBadgesCount;

    @Column(nullable = false)
    private Long completedChallengesCount;

    @Column(nullable = false)
    private Long totalStreakDays;

    @Column(nullable = false)
    private Long maxStreakDays;

    private OffsetDateTime lastTierUpAt;


    public static UserGameStats create(User user) {
        UserGameStats stats = new UserGameStats();
        stats.user = user;
        stats.tier = Tier.BRONZE;
        stats.tierSub = TierSub.V;
        stats.totalXp = 0L;
        stats.recoveryTickets = 0L;
        stats.recoveryUsedCount = 0L;
        stats.totalBadgesCount = 0L;
        stats.completedChallengesCount = 0L;
        stats.totalStreakDays = 0L;
        stats.maxStreakDays = 0L;
        return stats;
    }

    public void addXp(Long xp) {
        this.totalXp += xp;
    }

    public void addRecoveryTicket(Long count) {
        this.recoveryTickets = Math.min(this.recoveryTickets + count, 3L);
    }

    public void useRecoveryTicket() {
        this.recoveryTickets--;
        this.recoveryUsedCount++;
        this.lastRecoveryUsedAt = OffsetDateTime.now();
    }

    public void upgradeTier(Tier newTier, TierSub newTierSub) {
        this.tier = newTier;
        this.tierSub = newTierSub;
        this.lastTierUpAt = OffsetDateTime.now();
    }

    public void earnBadge() {
        this.totalBadgesCount++;
    }

    public void completeChallenge() {
        this.completedChallengesCount++;
    }

    public void updateStreak(Long currentStreak) {
        this.totalStreakDays = currentStreak;
        if (currentStreak > this.maxStreakDays) {
            this.maxStreakDays = currentStreak;
        }
    }

    public void updateRepresentativeBadge(Badge badge) {
        this.representativeBadge = badge;
    }
}
