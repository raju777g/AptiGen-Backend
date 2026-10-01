package com.aptigen.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActivityService {
    private final UserActivityEventRepository events;

    public void record(Long userId, String type) {
        if (userId != null) events.save(UserActivityEvent.builder().userId(userId).eventType(type).build());
    }
}
