package com.kb.youngly.vo.user;

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
public class NotificationVO {
    private Long notificationId;
    private String userId;
    private NotificationType notificationType;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
