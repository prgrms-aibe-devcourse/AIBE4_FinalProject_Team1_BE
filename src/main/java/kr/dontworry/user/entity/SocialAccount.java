package kr.dontworry.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "social_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String providerId;
    private String provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public SocialAccount(String provider, String providerId, User user) {
        this.providerId = providerId;
        this.provider = provider;
        this.user = user;
    }
}