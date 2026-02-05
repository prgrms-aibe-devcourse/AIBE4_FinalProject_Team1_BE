package kr.dontworry.domain.challenge.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.common.CreatedAtEntity;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "badges_user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_badge",
                        columnNames = {"user_id", "badge_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BadgeUser extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long badgeUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;


    public static BadgeUser earn(User user, Badge badge) {
        BadgeUser badgeUser = new BadgeUser();
        badgeUser.user = user;
        badgeUser.badge = badge;
        return badgeUser;
    }
}
