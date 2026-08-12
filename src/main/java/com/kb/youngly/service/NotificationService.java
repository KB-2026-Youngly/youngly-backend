package com.kb.youngly.service;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.notification.NotificationResponse;
import com.kb.youngly.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> getNotifications(String userId);

    MessageResponse readNotification(
            String userId,
            Long notificationId);

    MessageResponse deleteNotification(
            String userId,
            Long notificationId);

    void createNotification(
            String userId,
            NotificationType type,
            String content);
}