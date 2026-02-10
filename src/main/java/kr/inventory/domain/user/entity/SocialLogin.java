package kr.inventory.domain.user.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.CreatedAtEntity;
import kr.inventory.domain.user.entity.enums.SocialProvider;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "social_login",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_social_login_provider", columnNames = {"provider", "provider_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialLogin extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long socialId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SocialProvider provider;

    @Column(nullable = false, length = 100)
    private String providerId;

    public static SocialLogin create(
        User user,
        SocialProvider provider,
        String providerId
    ) {
        SocialLogin socialLogin = new SocialLogin();
        socialLogin.user = user;
        socialLogin.provider = provider;
        socialLogin.providerId = providerId;
        return socialLogin;
    }
}
