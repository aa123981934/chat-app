package com.example.demo2.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// model/PrivateMessage.java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrivateMessage {
    private String sender;
    private String to;       // 接收者
    private String content;
    private String time;
}
