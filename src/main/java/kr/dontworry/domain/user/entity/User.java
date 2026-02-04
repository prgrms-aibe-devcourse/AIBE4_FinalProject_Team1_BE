package kr.dontworry.domain.user.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.user.entity.enums.UserRole;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_public_id", columnNames = "public_id"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private UUID publicId;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    public static User create(String name, String email) {
        User user = new User();
        user.name = name;
        user.email = email;
        user.publicId = UUID.randomUUID();
        user.role = UserRole.USER;
        return user;
    }
}