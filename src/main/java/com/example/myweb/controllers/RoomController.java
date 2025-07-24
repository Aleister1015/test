/**
 * RoomController.java
 *
 * ▶ 此檔案為多人遊戲的核心控制器，處理所有「房間相關功能」的 REST API。
 *
 * ▶ 功能總覽：
 *   - 房間建立、加入、退出與查詢（create-room, join-room, getAllRooms...）
 *   - 頭貼選擇、角色指派與隨機領袖設定
 *   - 開始遊戲、開始投票、投票、票數統計與結果
 *
 * ▶ 與此控制器互動的單元：
 *   - RoomRepository：存取房間資料
 *   - RoomService：封裝較複雜的遊戲邏輯（例如投票流程）
 *   - SimpMessagingTemplate：用來透過 WebSocket 廣播開始訊息與事件更新
 *   - 前端 JavaScript 呼叫 `/api/` 下的路由與 WebSocket 訂閱 `/topic/room/{roomId}`
 *
 * ▶ 特色說明：
 *   - 支援動態角色分配，支援 5~10 人不同配置
 *   - 房主判定、動態人數驗證、所有玩家頭貼確認後才廣播開始
 *   - 投票過程包含發起投票、投票行為、投票狀態查詢與票數統計
 *
 * ▶ 備註：
 *   - 本控制器幾乎涵蓋整個遊戲流程，是邏輯密度最高的類別之一
 *   - 若有修改遊戲流程或房間管理邏輯，請從這裡進入
 */

package com.example.myweb.controllers;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.myweb.dto.AvatarSelectionRequest;
import com.example.myweb.models.MissionRecord;
import com.example.myweb.models.Room;
import com.example.myweb.repositories.RoomRepository;
import com.example.myweb.service.RoomService;

@RestController
@RequestMapping("/api")
public class RoomController {

    @Autowired private RoomRepository        roomRepository;
    @Autowired private RoomService           roomService;          // ★ 新增
    @Autowired private SimpMessagingTemplate simpMessagingTemplate;

    /* -------------------- 建房 -------------------- */
// 前端建立房間時會送出房名與創建者名稱，若重複就回錯誤訊息，否則存入資料庫。
    @PostMapping("/create-room")
    public ResponseEntity<Object> createRoom(@RequestBody Room room,
                                             @RequestParam String playerName) {

        String formattedRoomName = room.getRoomName() + "房間";
        room.setRoomName(formattedRoomName);

        boolean exists = roomRepository.findAll().stream()
                            .anyMatch(r -> r.getRoomName().equals(room.getRoomName()));
        if (exists)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body("房間名稱已存在，請選擇其他名稱！");

        room.setId(UUID.randomUUID().toString());
        if (!"private".equals(room.getRoomType())) room.setRoomPassword(null);

        room.setPlayers(new ArrayList<>(List.of(playerName)));
        roomRepository.save(room);
        return ResponseEntity.ok(room);
    }

   /* -------------------- 取得房間資料 -------------------- */
    // 根據房間 ID 回傳對應房間資料，或 404。
private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    @GetMapping("/room/{roomId}")
    public ResponseEntity<Room> getRoomById(@PathVariable String roomId) {
        return roomRepository.findById(roomId)
               .map(ResponseEntity::ok)
               .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }
// 回傳所有尚未開始的房間，用於大廳顯示。
    @GetMapping("/rooms")
    public ResponseEntity<List<Room>> getAllRooms() {
        List<Room> rooms = roomRepository.findAll().stream()
                             .filter(r -> !r.isStarted())
                             .collect(Collectors.toList());
        return ResponseEntity.ok(rooms);
    }

    /* -------------------- 加入與退出房間 -------------------- */
    // 加入房間時檢查：是否存在、人數是否滿、玩家是否重複。
    @Value("${daily.api.key}")
private String dailyApiKey;
private void createDailyRoomIfNotExists(String roomId) {
    try {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.daily.co/v1/rooms"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + dailyApiKey)
            .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"" + roomId + "\", \"properties\": {\"enable_chat\": false, \"enable_screenshare\": false, \"start_video_off\": true}}"))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 409) {
            System.out.println("🔁 語音房間已存在: " + roomId);
        } else if (response.statusCode() != 200) {
            System.err.println("❌ 無法建立 Daily 房間：" + response.body());
        } else {
            System.out.println("✅ 成功建立 Daily 語音房：" + roomId);
        }
    } catch (IOException | InterruptedException e) {
    log.error("Daily 房間建立失敗", e); // 建議使用 log 替代 printStackTrace()
}

}


    

    @PostMapping("/join-room")
    public ResponseEntity<Object> joinRoom(@RequestParam String roomId,
                                           @RequestParam String playerName,
                                           @RequestParam(required = false) String roomPassword) {

        Optional<Room> opt = roomRepository.findById(roomId);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("找不到房間");

        Room room = opt.get();
        List<String> players = room.getPlayers();

        if (players.size() >= room.getPlayerCount())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("房間人數已滿");

        if (players.contains(playerName))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("玩家已經加入房間");

        players.add(playerName);
        roomRepository.save(room);
        return ResponseEntity.ok(Map.of("success", true, "message", "加入房間成功"));
    }
// 離開房間；若是最後一人則刪除房間。
    @PostMapping("/exit-room")
    public ResponseEntity<Object> exitRoom(@RequestParam String roomId,
                                           @RequestParam String playerName) {

        Optional<Room> opt = roomRepository.findById(roomId);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("找不到房間");

        Room room = opt.get();
        List<String> players = room.getPlayers();

        if (!players.remove(playerName))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("該玩家不在此房間");

        if (players.isEmpty()) {
            roomRepository.delete(room);
            return ResponseEntity.ok(Map.of("success", true, "message", "退出房間成功，房間已刪除"));
        }
        roomRepository.save(room);
        return ResponseEntity.ok(Map.of("success", true, "message", "退出房間成功"));
    }

   /* -------------------- 房主開始遊戲（發送 WebSocket） -------------------- */
    // 僅房主（players[0]）可啟動遊戲，並向所有人廣播 startGame 訊息。

    @PostMapping("/start-game")
    public ResponseEntity<Object> startGame(@RequestParam String roomId,
                                            @RequestParam String playerName) {

        Optional<Room> opt = roomRepository.findById(roomId);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("找不到房間");

        Room room = opt.get();
        List<String> players = room.getPlayers();
        if (players.isEmpty() || !players.get(0).equals(playerName))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("只有房主可以開始遊戲");

        room.setStarted(true);
        roomRepository.save(room);
        simpMessagingTemplate.convertAndSend("/topic/room/" + roomId, "startGame");
        return ResponseEntity.ok(Map.of("success", true, "message", "遊戲開始訊息已廣播"));
    }

   /* -------------------- 玩家選頭貼邏輯 -------------------- */
    // 玩家選擇頭像後儲存，並廣播「誰選好了」；若所有人都選好，再廣播 allAvatarSelected。

    @PostMapping("/room/{roomId}/select-avatar")
    public ResponseEntity<?> selectAvatar(@PathVariable String roomId,
                                          @RequestBody AvatarSelectionRequest req) {

        String playerName = req.getPlayerName();
        String avatar     = req.getAvatar();

        Optional<Room> opt = roomRepository.findById(roomId);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("房間不存在");

        Room room = opt.get();
        if (!room.getPlayers().contains(playerName))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("該玩家不在此房間");

        room.getAvatarMap().put(playerName, avatar);
        roomRepository.save(room);

        simpMessagingTemplate.convertAndSend("/topic/room/" + roomId, "avatarSelected:" + playerName);

        if (room.getAvatarMap().size() >= room.getPlayerCount())
            simpMessagingTemplate.convertAndSend("/topic/room/" + roomId, "allAvatarSelected");

        return ResponseEntity.ok().build();
    }

    /* -------------------- 角色一次分配（舊流程） -------------------- */

        /** 角色一次分配（舊流程，改成 5–10 人都支援） */
    @PostMapping("/start-real-game")
    public ResponseEntity<Map<String, Room.RoleInfo>> startRealGame(
            @RequestParam String roomId,
            @RequestParam String playerName) {

        Optional<Room> opt = roomRepository.findById(roomId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Room room = opt.get();

        System.out.println("✅ 進入 startRealGame：roomId = " + roomId);
        System.out.println("👉 目前已指派角色數量：" + (room.getAssignedRoles() == null ? 0 : room.getAssignedRoles().size()));

        // 如果已指派過，就直接回傳舊結果
        if (room.getAssignedRoles() != null && !room.getAssignedRoles().isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(room.getAssignedRoles());
        }

        List<String> players = new ArrayList<>(room.getPlayers());
        List<Room.RoleInfo> roles;

        switch (players.size()) {
            case 5:
                roles = Arrays.asList(
                    new Room.RoleInfo("工程師",      "goodpeople1.png"),
                    new Room.RoleInfo("普通倖存者", "goodpeople4.png"),
                    new Room.RoleInfo("普通倖存者", "goodpeople4.png"),
                    new Room.RoleInfo("潛伏者",     "badpeople1.png"),
                    new Room.RoleInfo("邪惡平民",   "badpeople4.png")
                );
                break;
            case 6 :
                roles = Arrays.asList(
                    new Room.RoleInfo("指揮官",     "goodpeople3.png"),
                    new Room.RoleInfo("工程師",     "goodpeople1.png"),
                    new Room.RoleInfo("普通倖存者","goodpeople4.png"),
                    new Room.RoleInfo("破壞者","badpeople2.png"),
                    new Room.RoleInfo("潛伏者",     "badpeople1.png"),
                    new Room.RoleInfo("邪惡平民",   "badpeople4.png")
                );
                break;
            case 7:
                roles = Arrays.asList(
                    new Room.RoleInfo("指揮官",     "goodpeople3.png"),
                    new Room.RoleInfo("工程師",     "goodpeople1.png"),
                    new Room.RoleInfo("醫護兵",     "goodpeople2.png"),
                    new Room.RoleInfo("普通倖存者","goodpeople4.png"),
                    new Room.RoleInfo("潛伏者",     "badpeople1.png"),
                    new Room.RoleInfo("破壞者",     "badpeople2.png"),
                    new Room.RoleInfo("邪惡平民",   "badpeople4.png")
                );
                break;
            case 8:
                roles = Arrays.asList(
                    new Room.RoleInfo("指揮官",     "goodpeople3.png"),
                    new Room.RoleInfo("工程師",     "goodpeople1.png"),
                    new Room.RoleInfo("醫護兵",     "goodpeople2.png"),
                    new Room.RoleInfo("普通倖存者","goodpeople4.png"),
                    new Room.RoleInfo("普通倖存者","goodpeople4.png"),
                    new Room.RoleInfo("潛伏者",     "badpeople1.png"),
                    new Room.RoleInfo("破壞者",     "badpeople2.png"),
                    new Room.RoleInfo("邪惡平民",   "badpeople4.png")
                );
                break;
            
            case 9:
                roles = Arrays.asList(
                    new Room.RoleInfo("指揮官",     "goodpeople3.png"),
                    new Room.RoleInfo("工程師",     "goodpeople1.png"),
                    new Room.RoleInfo("醫護兵",     "goodpeople2.png"),
                    new Room.RoleInfo("普通倖存者","goodpeople4.png"),
                    new Room.RoleInfo("普通倖存者","goodpeople4.png"),
                    new Room.RoleInfo("普通倖存者","goodpeople4.png"),
                    new Room.RoleInfo("潛伏者",     "badpeople1.png"),
                    new Room.RoleInfo("破壞者",     "badpeople2.png"),
                    new Room.RoleInfo("影武者",     "badpeople3.png")
                );
                break;
            case 10:
                roles = Arrays.asList(
                    new Room.RoleInfo("指揮官",     "goodpeople3.png"),
                    new Room.RoleInfo("工程師",     "goodpeople1.png"),
                    new Room.RoleInfo("醫護兵",     "goodpeople2.png"),
                    new Room.RoleInfo("普通倖存者","goodpeople4.png"),
                    new Room.RoleInfo("普通倖存者","goodpeople4.png"),
                    new Room.RoleInfo("普通倖存者","goodpeople4.png"),
                    new Room.RoleInfo("潛伏者",     "badpeople1.png"),
                    new Room.RoleInfo("破壞者",     "badpeople2.png"),
                    new Room.RoleInfo("影武者",     "badpeople3.png"),
                    new Room.RoleInfo("邪惡平民",   "badpeople4.png")
                );
                break;
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                            .body(Map.of("error",
                                                new Room.RoleInfo("錯誤", "尚未支援此人數的遊戲模式")));
        }

        // 安全檢查
        if (roles.size() != players.size()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error",
                                    new Room.RoleInfo("錯誤", "角色數量與玩家人數不符")));
        }

        Collections.shuffle(players);
        Collections.shuffle(roles);

        Map<String, Room.RoleInfo> assigned = new HashMap<>();
        for (int i = 0; i < players.size(); i++) {
            assigned.put(players.get(i), roles.get(i));
        }

        room.setAssignedRoles(assigned);
        roomService.generateSkillOrder(room); 
        roomRepository.save(room);
        simpMessagingTemplate.convertAndSend("/topic/room/" + roomId, "startRealGame");

        return ResponseEntity.ok(assigned);
    }


    /* -------------------- 取玩家列表 -------------------- */

    @GetMapping("/room/{roomId}/players")
    public ResponseEntity<List<Map<String, String>>> getAllPlayers(@PathVariable String roomId) {

        return roomRepository.findById(roomId)
            .map(room -> {
                List<Map<String, String>> list = new ArrayList<>();
                room.getAvatarMap().forEach((name, avatar) -> {
                    list.add(Map.of("name", name, "avatar", avatar));
                });
                return ResponseEntity.ok(list);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /* -------------------- 取角色 + 領袖（唯一實作） -------------------- */

    @GetMapping("/room/{roomId}/roles")
    public ResponseEntity<Map<String,Object>> getRolesAndLeader(@PathVariable String roomId){

        return roomRepository.findById(roomId)
            .map(room -> {
                Map<String,Object> res = new HashMap<>();
                res.put("assignedRoles", room.getAssignedRoles());
                res.put("currentLeader", room.getCurrentLeader());
                return ResponseEntity.ok(res);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /* -------------------- 指派角色 + 隨機領袖 -------------------- */


    @PostMapping("/room/{roomId}/assign-roles")
    public Map<String,Object> assignRoles(@PathVariable String roomId){
        createDailyRoomIfNotExists(roomId);

        Room room = roomService.assignRoles(roomId);

        Map<String,Object> res = new HashMap<>();
        res.put("assignedRoles", room.getAssignedRoles());
        res.put("currentLeader", room.getCurrentLeader());
        return res;
    }

    /* =================================================
       🔥 投  票  相  關  端  點
       ================================================= */

    /** 開始投票（領袖送 expedition） */
    @PostMapping("/room/{roomId}/start-vote")
    public ResponseEntity<Void> startVote(
            @PathVariable String roomId,
            @RequestBody Map<String,Object> body) {

        @SuppressWarnings("unchecked")             // 🔥 修正：消除未檢查 cast 警告
        List<String> expedition = (List<String>) body.get("expedition");
        String leader = (String) body.get("leader");

        roomService.startVote(roomId, expedition, leader);   // 🔥 修正：改用 roomService
        return ResponseEntity.ok().build();
    }

    /** 玩家投票 */
    @PostMapping("/room/{roomId}/vote")
    public ResponseEntity<Map<String,Object>> vote(
            @PathVariable String roomId,
            @RequestBody Map<String,Object> body) {

        String voter = (String) body.get("voter");
        boolean agree = (Boolean) body.get("agree");

            Map<String,Object> result = roomService.castVote(roomId, voter, agree); // 🔥 修正
            return ResponseEntity.ok(result);
    }


    /** 取得目前票數與自身能否投票 */
    @GetMapping("/room/{roomId}/vote-state")
    public ResponseEntity<Map<String,Object>> voteState(
            @PathVariable String roomId,
            @RequestParam String player) {

        Map<String,Object> state = roomService.getVoteState(roomId, player);    // 🔥 修正
        return ResponseEntity.ok(state);
    }
    @GetMapping("/game-start/{roomId}")
        public String gameStart(@PathVariable String roomId){
        return "game-front-page";   // 或你真正的遊戲模板名

        
    }
    @GetMapping("/room/{roomId}/vote-result")
    public ResponseEntity<Map<String, Integer>> getVoteResult(@PathVariable String roomId) {
        Room room = roomService.getRoomById(roomId);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }

        int agree = 0;
        int reject = 0;
        Map<String, Boolean> voteMap = room.getVoteMap();
        if (voteMap != null) {
            for (Boolean vote : voteMap.values()) {
                if (vote == null) continue; // 棄票
                if (vote) agree++;
                else reject++;
            }
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("agree", agree);
        result.put("reject", reject);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/room/{roomId}/mission-result")
    public ResponseEntity<Void> submitMissionCard(
            @PathVariable String roomId,
            @RequestBody Map<String, String> payload
    ) {
        String player = payload.get("player");
        String result = payload.get("result");
        roomService.submitMissionCard(roomId, player, result);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/room/{roomId}/generate-skill-order")
    public ResponseEntity<List<String>> generateSkillOrder(@PathVariable String roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        List<String> order = roomService.generateSkillOrder(room);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/room/{roomId}/skill-state")
    public ResponseEntity<Map<String, Object>> getSkillState(@PathVariable String roomId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) return ResponseEntity.notFound().build();

        Room room = roomOpt.get();
        Map<String, Object> result = new HashMap<>();

        

        // ✅ 新增：取出目前技能角色順序（預存在 MongoDB 中，例如在 room.skillOrder）
        List<String> allSkillRoles = room.getSkillOrder();  // 你應該有這個欄位
        result.put("remainingRoles", allSkillRoles);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/room/{roomId}/skill-finish")
    public ResponseEntity<?> finishSkillPhase(@PathVariable String roomId) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) return ResponseEntity.notFound().build();

        Room room = roomOpt.get();
        int currentRound = room.getCurrentRound();
        String roundKey = String.valueOf(currentRound);

        MissionRecord record = room.getMissionResults().get(currentRound);
        if (record == null || record.getCardMap() == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("尚未結算任務結果");
        }

        // ✅ 重新統計 cardMap，確保是技能後狀態
        int success = 0, fail = 0;
        for (String card : record.getCardMap().values()) {
            if ("SUCCESS".equals(card)) success++;
            else if ("FAIL".equals(card)) fail++;
        }

        // ✅ 醫護兵保護判定：本回合是否有人被保護
        String protectedPlayer = room.getMedicProtectionMap().getOrDefault(currentRound, null);
        if (protectedPlayer != null && record.getCardMap().containsKey(protectedPlayer)) {
            Room.RoleInfo roleInfo = room.getAssignedRoles().get(protectedPlayer);
            String roleName = roleInfo != null ? roleInfo.getName() : "";

            boolean isGood = switch (roleName) {
                case "指揮官", "工程師", "醫護兵", "普通倖存者" -> true;
                default -> false;
            };

            if (isGood) {
                success++;  // ✅ 好人被保護 → 成功數 +1
            } else {
                success--;  // ✅ 壞人被保護 → 成功數 -1（等同讓壞人破壞失敗）
            }
        }

        // ✅ 更新 MissionRecord 中的統計數
        record.setSuccessCount(success);
        record.setFailCount(fail);

        // ✅ 累計寫回 Room
        room.setSuccessCount(room.getSuccessCount() + success);
        room.setFailCount(room.getFailCount() + fail);

        // 清除暫存資料
        room.getSubmittedMissionCards().clear();
        room.getMissionSuccess().remove(roundKey);
        room.getMissionFail().remove(roundKey);

        // 回合 +1
        room.setCurrentRound(currentRound + 1);
        roomRepository.save(room);

        // 廣播技能結束
        simpMessagingTemplate.convertAndSend("/topic/room/" + roomId, "allSkillUsed");

        return ResponseEntity.ok().build();
    }


   @PostMapping("/skill/lurker-toggle")
    public ResponseEntity<?> useLurkerSkill(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId");
        String playerName = body.get("playerName");
        String targetName = body.get("targetName");

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();

        // ✅ 技能限用一次
        if (room.getUsedSkillMap().getOrDefault(playerName, false)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("你已使用過潛伏者技能！");
        }

        int round = room.getCurrentRound();
        MissionRecord record = room.getMissionResults().get(round);
        if (record == null || record.getCardMap() == null || !record.getCardMap().containsKey(targetName)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("該玩家尚未提交任務卡");
        }

        // ✅ 判斷是否受到醫護兵保護
        String protectedPlayer = room.getMedicProtectionMap() != null
            ? room.getMedicProtectionMap().getOrDefault(round, null)
            : null;

        if (protectedPlayer != null && protectedPlayer.equals(targetName)) {
            return ResponseEntity.status(403).body("該玩家已受到醫護兵保護，潛伏者無法反轉此卡。");
        }

        // ✅ 反轉卡片內容
        String original = record.getCardMap().get(targetName);
        String toggled = original.equals("SUCCESS") ? "FAIL" : "SUCCESS";
        record.getCardMap().put(targetName, toggled);

        // ✅ 標記技能已用
        room.getUsedSkillMap().put(playerName, true);
        roomRepository.save(room);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/skill/commander-check")
    public ResponseEntity<?> useCommanderSkill(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId");
        String playerName = body.get("playerName");     // 指揮官本人
        String targetName = body.get("targetName");     // 要查看的對象

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();

        // ✅ 不能查看自己
        if (playerName.equals(targetName)) {
            return ResponseEntity.badRequest().body("不能查看自己");
        }

        int currentRound = room.getCurrentRound();

        // ✅ 使用次數限制
        Map<String, Integer> skillCount = room.getCommanderSkillCount();
        int used = skillCount.getOrDefault(playerName, 0);
        if (used >= 2) {
            return ResponseEntity.status(403).body("你已使用過 2 次技能");
        }

        // ✅ 每回合只可用一次
        String usageKey = playerName + "_R" + currentRound;
        Set<String> usedThisRound = room.getCommanderUsedThisRound();
        if (usedThisRound.contains(usageKey)) {
            return ResponseEntity.status(403).body("本回合你已查詢過玩家");
        }

        // ✅ 查詢目標角色陣營
        Room.RoleInfo roleInfo = room.getAssignedRoles().get(targetName);
        if (roleInfo == null) return ResponseEntity.badRequest().body("找不到該玩家角色");

        String roleName = roleInfo.getName();
        String faction = switch (roleName) {
            case "工程師", "醫護兵", "指揮官", "普通倖存者" -> "正義";
            case "潛伏者", "破壞者", "影武者", "邪惡平民" -> "邪惡";
            default -> "未知";
        };

        // ✅ 記錄技能使用
        skillCount.put(playerName, used + 1);
        usedThisRound.add(usageKey);

        room.setCommanderSkillCount(skillCount);
        room.setCommanderUsedThisRound(usedThisRound);
        roomRepository.save(room);

        return ResponseEntity.ok(Map.of(
            "faction", faction,
            "remaining", 2 - (used + 1)
        ));
    }

   @PostMapping("/skill/saboteur-nullify")
    public ResponseEntity<?> useSaboteurSkill(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId");
        String playerName = body.get("playerName");
        String targetName = body.get("targetName");

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();

        int round = room.getCurrentRound();
        MissionRecord record = room.getMissionResults().get(round);
        if (record == null || record.getCardMap() == null || !record.getCardMap().containsKey(targetName))
            return ResponseEntity.status(400).body("該玩家尚未提交卡片");

        String roundKey = playerName + "_R" + round;
        if (room.getSaboteurUsedThisRound().contains(roundKey))
            return ResponseEntity.status(403).body("本回合你已使用過技能");

        int used = room.getSaboteurSkillCount().getOrDefault(playerName, 0);
        if (used >= 2) return ResponseEntity.status(403).body("技能已使用 2 次");

        // ✅ 檢查是否被醫護兵保護
        String protectedPlayer = room.getMedicProtectionMap() != null
            ? room.getMedicProtectionMap().getOrDefault(round, null)
            : null;

        if (protectedPlayer != null && protectedPlayer.equals(targetName)) {
            return ResponseEntity.status(403).body("該玩家已受到醫護兵保護，破壞者無法破壞此卡。");
        }

        // ✅ 執行移除卡片
        String removed = record.getCardMap().remove(targetName);
        room.getSaboteurSkillCount().put(playerName, used + 1);
        room.getSaboteurUsedThisRound().add(roundKey);
        roomRepository.save(room);

        return ResponseEntity.ok(Map.of("removed", removed, "remaining", 1 - used));
    }


    @PostMapping("/skill/medic-protect")
    public ResponseEntity<?> useMedicSkill(@RequestBody Map<String, String> body) {
        String roomId = body.get("roomId");
        String playerName = body.get("playerName");   // 醫護兵自己
        String targetName = body.get("targetName");   // 要保護的對象

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();

        if (room.getMedicSkillUsed().getOrDefault(playerName, false)) {
            return ResponseEntity.status(403).body("你已使用過技能");
        }

        int round = room.getCurrentRound();

        room.getMedicProtectionMap().put(round + 1, targetName); // 下一回合才生效
        room.getMedicSkillUsed().put(playerName, true);

        roomRepository.save(room);
        return ResponseEntity.ok(Map.of("protected", targetName));
    }



}



