package com.kb.youngly.dto.notification;

import com.kb.youngly.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long notificationId;
    private NotificationType notificationType;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;

}