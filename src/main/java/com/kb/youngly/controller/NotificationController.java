package com.kb.youngly.controller;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.notification.NotificationResponse;
import com.kb.youngly.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 알림 조회
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            Authentication authentication) {

        return ResponseEntity.ok(
                notificationService.getNotifications(
                        authentication.getName()
                )
        );
    }

    // 읽음 처리
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<MessageResponse> readNotification(
            Authentication authentication,
            @PathVariable Long notificationId) {

        return ResponseEntity.ok(
                notificationService.readNotification(
                        authentication.getName(),
                        notificationId
                )
        );
    }

    // 알림 삭제
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<MessageResponse> deleteNotification(
            Authentication authentication,
            @PathVariable Long notificationId) {

        return ResponseEntity.ok(
                notificationService.deleteNotification(
                        authentication.getName(),
                        notificationId
                )
        );
    }
}