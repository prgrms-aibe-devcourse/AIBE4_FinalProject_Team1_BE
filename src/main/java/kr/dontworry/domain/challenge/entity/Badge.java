package kr.dontworry.domain.challenge.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import kr.dontworry.domain.challenge.entity.enums.BadgeCategory;
import kr.dontworry.domain.challenge.entity.enums.BadgeRarity;
import kr.dontworry.domain.common.CreatedAtEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "badges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Badge extends CreatedAtEntity {

    @Id
    @Column(length = 30)
    private String badgeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10)
    private String icon;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BadgeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BadgeRarity rarity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode unlockCondition;

    public static Badge create(
            String badgeId,
            String name,
            String icon,
            String description,
            BadgeCategory category,
            BadgeRarity rarity
    ) {
        Badge badge = new Badge();
        badge.badgeId = badgeId;
        badge.name = name;
        badge.icon = icon;
        badge.description = description;
        badge.category = category;
        badge.rarity = rarity;
        return badge;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateUnlockCondition(JsonNode condition) {
        this.unlockCondition = condition;
    }
}
