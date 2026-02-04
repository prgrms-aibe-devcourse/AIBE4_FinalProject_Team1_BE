package kr.dontworry.domain.goal.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.category.entity.Category;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.goal.entity.enums.GoalPriority;
import kr.dontworry.domain.goal.entity.enums.GoalStatus;
import kr.dontworry.domain.goal.entity.enums.GoalType;
import kr.dontworry.domain.ledger.entity.Ledger;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "goals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Goal extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long goalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GoalType type;

    @Column(nullable = false)
    private Long targetAmount;

    @Column(nullable = false)
    private Long currentAmount;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GoalPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status;

    @Column(nullable = false)
    private Boolean autoContributionEnabled;

    private Long autoContributionAmount;

    private Integer autoContributionDay;

    private LocalDate lastContributionDate;

    private OffsetDateTime achievedAt;

    public static Goal create(
        User user,
        Ledger ledger,
        Category category,
        String title,
        GoalType type,
        Long targetAmount,
        LocalDate startDate
    ) {
        Goal goal = new Goal();
        goal.user = user;
        goal.ledger = ledger;
        goal.category = category;
        goal.title = title;
        goal.type = type;
        goal.targetAmount = targetAmount;
        goal.currentAmount = 0L;
        goal.startDate = startDate;
        goal.priority = GoalPriority.MEDIUM;
        goal.status = GoalStatus.IN_PROGRESS;
        goal.autoContributionEnabled = false;
        return goal;
    }
}
