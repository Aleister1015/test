package com.example.myweb.controllers;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class VoiceController {

    @MessageMapping("/voice")
    @SendTo("/topic/voice")
    public byte[] broadcastVoice(@Payload byte[] audio, SimpMessageHeaderAccessor headerAccessor) {
        return audio; // 直接轉送原始音訊
    }
}
