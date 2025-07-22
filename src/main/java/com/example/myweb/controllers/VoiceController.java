package com.example.myweb.controllers;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class VoiceController {

    @MessageMapping("/voice")
    @SendTo("/topic/voice")
    public byte[] broadcastVoice(Message<byte[]> message) {
        byte[] audio = message.getPayload();
        System.out.println("📥 收到語音封包：" + audio.length + " bytes");
        return audio;
    }
}
