package com.kb.youngly.mapper;

import com.kb.youngly.dto.notification.NotificationResponse;
import com.kb.youngly.vo.user.NotificationVO;

import java.util.List;

public interface NotificationMapper {

    List<NotificationResponse> findNotifications(String userId);

    NotificationVO findNotificationById(Long notificationId);

    void readNotification(Long notificationId);

    void deleteNotification(Long notificationId);

}