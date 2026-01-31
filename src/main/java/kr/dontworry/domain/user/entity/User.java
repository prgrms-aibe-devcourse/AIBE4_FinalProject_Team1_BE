package kr.dontworry.domain.user.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    private String nickname;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    private LocalDateTime deletedAt;

    @PrePersist
    public void generatePublicId() {
        this.publicId = UUID.randomUUID();
    }

    public User(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
    }

    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
    }
}