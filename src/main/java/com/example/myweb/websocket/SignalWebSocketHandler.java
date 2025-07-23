package com.example.myweb.websocket;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class SignalWebSocketHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessions = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 連線成功，使用 roomId+playerName 作為 key
        String roomId = getParam(session, "roomId");
        String name = getParam(session, "name");
        sessions.put(roomId + "_" + name, session);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 直接轉發給其他對象
        String roomId = getParam(session, "roomId");
        String name = getParam(session, "name");
        String target = getParam(session, "target");

        WebSocketSession targetSession = sessions.get(roomId + "_" + target);
        if (targetSession != null && targetSession.isOpen()) {
            targetSession.sendMessage(new TextMessage(message.getPayload()));
        }
    }

    private String getParam(WebSocketSession session, String key) {
        return Objects.requireNonNull(session.getUri()).getQuery()
            .replace("&", "?") // 簡單處理 query string
            .replaceFirst("^.*?" + key + "=", "")
            .split("\\?")[0];
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.values().remove(session);
    }
}
