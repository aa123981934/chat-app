package com.example.demo2.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// service/OnlineUserService.java
@Service
public class OnlineUserService {

    private final Set<String> users = ConcurrentHashMap.newKeySet();

    public void addUser(String username) {
        users.add(username);
    }

    public void removeUser(String username) {
        users.remove(username);
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> map = new HashMap<>();
        map.put("users", new ArrayList<>(users));
        map.put("count", users.size());
        return map;
    }
}
