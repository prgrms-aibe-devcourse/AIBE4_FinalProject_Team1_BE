package kr.inventory.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.notification.controller.dto.response.NotificationActionResponse;
import kr.inventory.domain.notification.controller.dto.response.NotificationResponse;
import kr.inventory.domain.notification.service.NotificationService;
import kr.inventory.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "알림(Notification)", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 SSE 스트림 연결")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        SseEmitter emitter = notificationService.connectStream(principal.getUserId());

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    @Operation(summary = "내 알림 목록 조회")
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<NotificationResponse> result = notificationService.list(principal.getUserId(), pageable);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @Operation(summary = "안 읽은 알림 개수 조회")
    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(notificationService.unreadCount(principal.getUserId()));
    }

    @Operation(summary = "알림 개별 읽음 처리")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationActionResponse> readOne(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long notificationId
    ) {
        notificationService.readOne(principal.getUserId(), notificationId);
        return ResponseEntity.ok(NotificationActionResponse.readOne(notificationId));
    }

    @Operation(summary = "알림 전체 읽음 처리")
    @PatchMapping("/read-all")
    public ResponseEntity<NotificationActionResponse> readAll(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        int affectedCount = notificationService.readAll(principal.getUserId());
        return ResponseEntity.ok(NotificationActionResponse.readAll(affectedCount));
    }

    @Operation(summary = "알림 삭제")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<NotificationActionResponse> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long notificationId
    ) {
        notificationService.delete(principal.getUserId(), notificationId);
        return ResponseEntity.ok(NotificationActionResponse.delete(notificationId));
    }
}