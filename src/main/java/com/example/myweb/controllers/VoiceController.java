package com.example.myweb.controllers;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class VoiceController {

    @MessageMapping("/voice")
    @SendTo("/topic/voice")
    public String broadcastVoice(@Payload String base64Audio) {
        System.out.println("📥 收到語音 base64 字串，長度：" + base64Audio.length());
        return base64Audio;
    }
}
