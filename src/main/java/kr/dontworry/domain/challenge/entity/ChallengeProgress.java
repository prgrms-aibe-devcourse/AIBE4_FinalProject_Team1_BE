package kr.dontworry.domain.challenge.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "challenge_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_challenge",
                        columnNames = {"user_id", "challenge_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeProgress extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long progressId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(nullable = false)
    private Long currentCount;

    @Column(nullable = false)
    private Boolean isCompleted;

    private OffsetDateTime completedAt;

    @Column(nullable = false)
    private Long currentStreak;

    @Column(nullable = false)
    private Long maxStreak;

    private LocalDate lastAchievedDate;

    private OffsetDateTime lastResetAt;

    @Column(nullable = false)
    private Boolean recoveryUsedToday;

    public static ChallengeProgress create(User user, Challenge challenge) {
        ChallengeProgress progress = new ChallengeProgress();
        progress.user = user;
        progress.challenge = challenge;
        progress.currentCount = 0L;
        progress.isCompleted = false;
        progress.currentStreak = 0L;
        progress.maxStreak = 0L;
        progress.recoveryUsedToday = false;
        return progress;
    }

    public void incrementCount() {
        this.currentCount++;
    }

    public void incrementStreak() {
        this.currentStreak++;
        if (this.currentStreak > this.maxStreak) {
            this.maxStreak = this.currentStreak;
        }
        this.lastAchievedDate = LocalDate.now();
    }

    public void resetStreak() {
        this.currentStreak = 0L;
        this.recoveryUsedToday = false;
    }

    public void useRecovery() {
        this.recoveryUsedToday = true;
    }

    public void resetRecoveryFlag() {
        this.recoveryUsedToday = false;
    }

    public void complete() {
        this.isCompleted = true;
        this.completedAt = OffsetDateTime.now();
    }

    public void updateResetTime() {
        this.lastResetAt = OffsetDateTime.now();
    }

    public void updateLastAchievedDate(LocalDate date) {
        this.lastAchievedDate = date;
    }

    public void updateMaxStreak(Long streak) {
        this.maxStreak = streak;
    }
}
