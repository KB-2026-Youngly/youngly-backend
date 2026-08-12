package com.kb.youngly.controller;

import com.kb.youngly.websocket.NotificationPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class NotificationTestController {

    private final NotificationPublisher publisher;

    public NotificationTestController(NotificationPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/notification")
    public String test(Authentication authentication) {

        publisher.send(
                authentication.getName(),
                "테스트 알림입니다."
        );

        return "Success";
    }
}