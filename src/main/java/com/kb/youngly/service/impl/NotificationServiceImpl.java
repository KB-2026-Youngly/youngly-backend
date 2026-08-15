package com.kb.youngly.service.impl;

import com.kb.youngly.dto.common.MessageResponse;
import com.kb.youngly.dto.notification.NotificationResponse;
import com.kb.youngly.enums.NotificationType;
import com.kb.youngly.mapper.NotificationMapper;
import com.kb.youngly.service.NotificationService;
import com.kb.youngly.vo.user.NotificationVO;
import com.kb.youngly.websocket.NotificationPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationPublisher notificationPublisher;

    public NotificationServiceImpl(
            NotificationMapper notificationMapper,
            NotificationPublisher notificationPublisher) {

        this.notificationMapper = notificationMapper;
        this.notificationPublisher = notificationPublisher;
    }

    /**
     * 알림 조회
     */
    @Override
    public List<NotificationResponse> getNotifications(String userId) {

        return notificationMapper.findNotifications(userId);
    }

    /**
     * 읽음 처리
     */
    @Override
    @Transactional
    public MessageResponse readNotification(
            String userId,
            Long notificationId) {

        NotificationVO notification =
                notificationMapper.findNotificationById(notificationId);

        validateNotification(notification, userId);

        notificationMapper.readNotification(notificationId);

        return MessageResponse.builder()
                .message("Success")
                .build();
    }

    /**
     * 알림 삭제
     */
    @Override
    @Transactional
    public MessageResponse deleteNotification(
            String userId,
            Long notificationId) {

        NotificationVO notification =
                notificationMapper.findNotificationById(notificationId);

        validateNotification(notification, userId);

        notificationMapper.deleteNotification(notificationId);

        return MessageResponse.builder()
                .message("Success")
                .build();
    }

    /**
     * 알림 존재 여부 및 권한 확인
     */
    private void validateNotification(
            NotificationVO notification,
            String userId) {

        if (notification == null) {
            throw new IllegalArgumentException("존재하지 않는 알림입니다.");
        }

        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 알림에 접근 권한이 없습니다.");
        }
    }

    /**
     * 알림 생성
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(
            String userId,
            NotificationType type,
            String content) {

        NotificationVO notification =
                NotificationVO.builder()
                        .userId(userId)
                        .notificationType(type)
                        .content(content)
                        .isRead(false)
                        .build();

        notificationMapper.insertNotification(notification);

        NotificationResponse response =
                NotificationResponse.builder()
                        .notificationId(notification.getNotificationId())
                        .notificationType(notification.getNotificationType())
                        .content(notification.getContent())
                        .isRead(notification.getIsRead())
                        .build();

        notificationPublisher.send(
                userId,
                response
        );
    }
}