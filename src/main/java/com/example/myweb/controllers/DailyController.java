package com.example.myweb.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class DailyController {

    @Value("${daily.api.key}")
    private String dailyApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/check-daily-room/{roomName}")
    public Map<String, Object> checkRoomParticipants(@PathVariable String roomName) {
        String url = "https://api.daily.co/v1/rooms/" + roomName;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + dailyApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> roomInfo = response.getBody();
            Object participantCount = roomInfo.getOrDefault("participants_count", 0);
            return Map.of("room", roomName, "participantCount", participantCount);
        } catch (Exception e) {
            return Map.of("room", roomName, "participantCount", -1, "error", "Room not found or API error");
        }
    }
}
