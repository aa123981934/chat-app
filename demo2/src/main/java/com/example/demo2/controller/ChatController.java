package com.example.demo2.controller;

import com.example.demo2.model.ChatMessage;
import com.example.demo2.model.PrivateMessage;
import com.example.demo2.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

// controller/ChatController.java
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messaging;
    private final OnlineUserService onlineUserService;

    // 公開聊天室
    @MessageMapping("/chat")
    @SendTo("/topic/chatroom")
    public ChatMessage publicChat(ChatMessage msg) {
        msg.setTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        return msg;
    }

    // 私訊
    @MessageMapping("/private")
    public void privateChat(PrivateMessage msg) {
        msg.setTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        // 推給接收者
        messaging.convertAndSend("/topic/private." + msg.getTo(), (Object) msg);
        // 也推給發送者（這樣自己也看得到）
        messaging.convertAndSend("/topic/private." + msg.getSender(), (Object) msg);
    }

    // 用戶加入
    @MessageMapping("/join")
    public void userJoin(@Payload Map<String, String> payload,
                         SimpMessageHeaderAccessor accessor) {
        String username = payload.get("username");
        accessor.getSessionAttributes().put("username", username);
        onlineUserService.addUser(username);
        messaging.convertAndSend("/topic/online", (Object)onlineUserService.getStatus());
    }
}
