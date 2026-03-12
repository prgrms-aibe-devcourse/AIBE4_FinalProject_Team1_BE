package kr.inventory.domain.notification.service.publish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kr.inventory.domain.notification.entity.enums.NotificationType;

import java.math.BigDecimal;
import java.util.UUID;

public record NotificationPublishCommand(
        Long userId,
        NotificationType type,
        String title,
        String message,
        String deepLink,
        JsonNode metadata
) {

    private static NotificationPublishCommand storeMember(
            Long userId,
            NotificationType type,
            String title,
            String message,
            String deepLink,
            ObjectNode metadata
    ) {
        return new NotificationPublishCommand(
                userId,
                type,
                title,
                message,
                deepLink,
                metadata
        );
    }

    public static NotificationPublishCommand storeMemberRegistered(
            Long userId,
            UUID storePublicId,
            String storeName,
            String role
    ) {
        String title = "매장 멤버 가입이 완료되었습니다.";
        String message = storeName + " 매장에 가입되었습니다.";
        String deepLink = null;

        ObjectNode metadata = JsonNodeFactory.instance.objectNode();
        metadata.put("storePublicId", storePublicId.toString());
        metadata.put("storeName", storeName);
        metadata.put("role", role);
        metadata.put("displayPolicy", NotificationDisplayPolicy.INBOX_ONLY.name());

        return storeMember(
                userId,
                NotificationType.STORE_MEMBER_REGISTERED,
                title,
                message,
                deepLink,
                metadata
        );
    }

    public static NotificationPublishCommand storeMemberJoined(
            Long ownerUserId,
            UUID storePublicId,
            String storeName,
            Long joinedUserId,
            String joinedUserName,
            String role
    ) {
        String title = "새 매장 멤버가 가입했습니다.";
        String message = joinedUserName + "님이 " + storeName + " 매장에 가입했습니다.";
        String deepLink = "/stores/members";

        ObjectNode metadata = JsonNodeFactory.instance.objectNode();
        metadata.put("storePublicId", storePublicId.toString());
        metadata.put("storeName", storeName);
        metadata.put("joinedUserId", joinedUserId);
        metadata.put("joinedUserName", joinedUserName);
        metadata.put("role", role);
        metadata.put("displayPolicy", NotificationDisplayPolicy.INBOX_ONLY.name());

        return storeMember(
                ownerUserId,
                NotificationType.STORE_MEMBER_JOINED,
                title,
                message,
                deepLink,
                metadata
        );
    }

    public static NotificationPublishCommand stockBelowThreshold(
            Long userId,
            UUID storePublicId,
            UUID ingredientPublicId,
            String ingredientName,
            BigDecimal currentQuantity,
            BigDecimal thresholdQuantity
    ){
        String title = "재고 부족 경고";
        String message = ingredientName + " 재고가 임계치 이하로 내려갔습니다.";
        String deepLink = "/stock";

        ObjectNode metadata = JsonNodeFactory.instance.objectNode();
        metadata.put("storePublicId", storePublicId.toString());
        metadata.put("ingredientPublicId", ingredientPublicId.toString());
        metadata.put("ingredientName", ingredientName);
        metadata.put("currentQuantity", currentQuantity.toPlainString());
        metadata.put("thresholdQuantity", thresholdQuantity.toPlainString());
        metadata.put("displayPolicy", NotificationDisplayPolicy.TOAST_AND_INBOX.name());

        return new NotificationPublishCommand(
                userId,
                NotificationType.STOCK_BELOW_THRESHOLD,
                title,
                message,
                deepLink,
                metadata
        );
    }
}