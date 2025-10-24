package com.hanmo.flowplan.user.application;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

// 임시 RT 저장소(메모리), 추후 Redis로 대체
@Component
public class RefreshTokenStore {
    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    public void save(String email, String refreshToken) {
        store.put(email, refreshToken);
    }

    public String get(String email) {
        return store.get(email);
    }

    public void delete(String email) {
        store.remove(email);
    }
}
