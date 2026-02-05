package kr.dontworry.domain.challenge.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.common.CreatedAtEntity;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "daily_challenge_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_challenge_date",
                        columnNames = {"user_id", "challenge_id", "log_date"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyChallengeLog extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(nullable = false)
    private LocalDate logDate;

    @Column(nullable = false)
    private Boolean isAchieved;

    @Column(nullable = false)
    private Boolean recoveryUsed;

    @Column(nullable = false)
    private Long xpEarned;

    public static DailyChallengeLog createAchieved(
            User user,
            Challenge challenge,
            LocalDate logDate,
            Long xpEarned
    ) {
        DailyChallengeLog log = new DailyChallengeLog();
        log.user = user;
        log.challenge = challenge;
        log.logDate = logDate;
        log.isAchieved = true;
        log.recoveryUsed = false;
        log.xpEarned = xpEarned;
        return log;
    }

    public static DailyChallengeLog createRecovered(
            User user,
            Challenge challenge,
            LocalDate logDate
    ) {
        DailyChallengeLog log = new DailyChallengeLog();
        log.user = user;
        log.challenge = challenge;
        log.logDate = logDate;
        log.isAchieved = false;
        log.recoveryUsed = true;
        log.xpEarned = 0L;
        return log;
    }

    public static DailyChallengeLog createFailed(
            User user,
            Challenge challenge,
            LocalDate logDate
    ) {
        DailyChallengeLog log = new DailyChallengeLog();
        log.user = user;
        log.challenge = challenge;
        log.logDate = logDate;
        log.isAchieved = false;
        log.recoveryUsed = false;
        log.xpEarned = 0L;
        return log;
    }
}
