package com.parkease.notifanalytics.notification.resource;

import com.parkease.notifanalytics.notification.entity.Notification;
import com.parkease.notifanalytics.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification management")
public class NotificationResource {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications for authenticated user")
    public ResponseEntity<List<Notification>> getMyNotifications(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(notificationService.getByRecipient(userId));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Get unread notification count (badge)")
    public ResponseEntity<Integer> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @Operation(summary = "Send broadcast notification (Admin only)")
    public ResponseEntity<Void> sendBulk(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        @SuppressWarnings("unchecked")
        List<Long> recipients = (List<Long>) body.get("recipientIds");
        notificationService.sendBulk(recipients, (String) body.get("title"), (String) body.get("message"));
        return ResponseEntity.noContent().build();
    }
}
