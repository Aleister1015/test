package com.example.myweb.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 遊戲房間資料模型（MongoDB）
 */
@Document(collection = "rooms")
public class Room {

    /* ========== 基本欄位 ========== */
    @Id
    private String id;
    private String roomName;
    private int    playerCount;
    private String roomType;          // public / private
    private String roomPassword;

    private List<String>         players   = new ArrayList<>();
    private Map<String,String>   avatarMap = new HashMap<>();

    /* 角色相關 */
    private Map<String, RoleInfo> assignedRoles = new HashMap<>();
    private Map<String, Boolean> usedSkillMap = new HashMap<>();
    private Map<String, Integer> commanderSkillCount = new HashMap<>();
    private Set<String> commanderUsedThisRound = new HashSet<>();
    private Map<String, Integer> saboteurSkillCount = new HashMap<>();
    private Set<String> saboteurUsedThisRound = new HashSet<>();
    private Map<String, Boolean> medicSkillUsed = new HashMap<>();
    private Map<Integer, String> medicProtectionMap = new HashMap<>(); 


    private String voiceRoomName;

public String getVoiceRoomName() {
    return voiceRoomName;
}

public void setVoiceRoomName(String voiceRoomName) {
    this.voiceRoomName = voiceRoomName;
}

    /* 狀態旗標 */
    private boolean started = false;

    /* 當前領袖 */
    private String currentLeader;

    /* ========== 🔥 投票相關欄位 ========== */
    /** 本輪被提名出戰的玩家清單 */
    private List<String> currentExpedition = new ArrayList<>();    // 🔥 新增

    /** 投票結果：玩家 → true(同意) / false(反對) */
    private Map<String, Boolean> voteMap = new HashMap<>();        // 🔥 新增

    private Map<Integer, MissionRecord> missionResults = new HashMap<>();
    private Map<String, String> submittedMissionCards = new HashMap<>();
    private int currentRound = 1;

    private List<String> skillOrder; // 技能順序

    private int skillIndex = 0; // 目前輪到第幾位技能角色（索引）
    private int skillRound = 0; // 技能回合編號（從0開始，每回合遞增）


    private Map<String, Integer> missionSuccess = new HashMap<>();
    private Map<String, Integer> missionFail = new HashMap<>();
    private int successCount = 0;
    private int failCount = 0;

    
    /* ========== Getter / Setter ========== */

    public String getId() { return id; }
    public void   setId(String id) { this.id = id; }

    public String getRoomName() { return roomName; }
    public void   setRoomName(String roomName) { this.roomName = roomName; }

    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }

    public String getRoomType() { return roomType; }
    public void   setRoomType(String roomType) { this.roomType = roomType; }

    public String getRoomPassword() { return roomPassword; }
    public void   setRoomPassword(String roomPassword) { this.roomPassword = roomPassword; }

    public List<String> getPlayers() { return players; }
    public void         setPlayers(List<String> players) { this.players = players; }

    public Map<String,String> getAvatarMap() { return avatarMap; }
    public void               setAvatarMap(Map<String,String> avatarMap) { this.avatarMap = avatarMap; }

    public Map<String,RoleInfo> getAssignedRoles() { return assignedRoles; }
    public void                 setAssignedRoles(Map<String,RoleInfo> assignedRoles) { this.assignedRoles = assignedRoles; }

    public boolean isStarted() { return started; }
    public void    setStarted(boolean started) { this.started = started; }

    public String getCurrentLeader() { return currentLeader; }
    public void   setCurrentLeader(String currentLeader) { this.currentLeader = currentLeader; }

    public Map<String, Boolean> getUsedSkillMap() {
        return usedSkillMap;
    }

    public void setUsedSkillMap(Map<String, Boolean> usedSkillMap) {
        this.usedSkillMap = usedSkillMap;
    }

    public Map<String, Integer> getCommanderSkillCount() {
        return commanderSkillCount;
    }

    public void setCommanderSkillCount(Map<String, Integer> commanderSkillCount) {
        this.commanderSkillCount = commanderSkillCount;
    }

    public Set<String> getCommanderUsedThisRound() {
        return commanderUsedThisRound;
    }

    public void setCommanderUsedThisRound(Set<String> commanderUsedThisRound) {
        this.commanderUsedThisRound = commanderUsedThisRound;
    }

    public Map<String, Integer> getSaboteurSkillCount() {
        return saboteurSkillCount;
    }
    public void setSaboteurSkillCount(Map<String, Integer> saboteurSkillCount) {
        this.saboteurSkillCount = saboteurSkillCount;
    }

    public Set<String> getSaboteurUsedThisRound() {
        return saboteurUsedThisRound;
    }
    public void setSaboteurUsedThisRound(Set<String> saboteurUsedThisRound) {
        this.saboteurUsedThisRound = saboteurUsedThisRound;
    }

    public Map<String, Boolean> getMedicSkillUsed() {
        return medicSkillUsed;
    }
    public void setMedicSkillUsed(Map<String, Boolean> medicSkillUsed) {
        this.medicSkillUsed = medicSkillUsed;
    }

    public Map<Integer, String> getMedicProtectionMap() {
        return medicProtectionMap;
    }
    public void setMedicProtectionMap(Map<Integer, String> medicProtectionMap) {
        this.medicProtectionMap = medicProtectionMap;
    }


    /* ---------- 🔥 投票欄位 Getter / Setter ---------- */
    public List<String> getCurrentExpedition() { return currentExpedition; }
    public void setCurrentExpedition(List<String> currentExpedition) { this.currentExpedition = currentExpedition; }

    public Map<String, Boolean> getVoteMap() { return voteMap; }
    public void setVoteMap(Map<String, Boolean> voteMap) { this.voteMap = voteMap; }

    /* ========== 內部類：角色資訊 ========== */
    public static class RoleInfo {
        private String name;   // 角色名稱
        private String image;  // 對應圖片檔

        public RoleInfo() {}
        public RoleInfo(String name, String image) {
            this.name  = name;
            this.image = image;
        }

        public String getName()  { return name;  }
        public void   setName(String name)  { this.name = name; }

        public String getImage() { return image; }
        public void   setImage(String image) { this.image = image; }
    }
    public int getCurrentRound() {
    return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public Map<Integer, MissionRecord> getMissionResults() {
        return missionResults;
    }

    public void setMissionResults(Map<Integer, MissionRecord> missionResults) {
        this.missionResults = missionResults;
    }

    public Map<String, String> getSubmittedMissionCards() {
        return submittedMissionCards;
    }

    public void setSubmittedMissionCards(Map<String, String> submittedMissionCards) {
        this.submittedMissionCards = submittedMissionCards;
    }
    public List<String> getSkillOrder() {
        return skillOrder;
    }

    public void setSkillOrder(List<String> skillOrder) {
        this.skillOrder = skillOrder;
    }

    public int getSkillIndex() {
        return skillIndex;
    }

    public void setSkillIndex(int skillIndex) {
        this.skillIndex = skillIndex;
    }

    public int getSkillRound() {
        return skillRound;
    }

    public void setSkillRound(int skillRound) {
        this.skillRound = skillRound;
    }
    public Map<String, Integer> getMissionSuccess() {
        return missionSuccess;
    }

    public void setMissionSuccess(Map<String, Integer> missionSuccess) {
        this.missionSuccess = missionSuccess;
    }

    public Map<String, Integer> getMissionFail() {
        return missionFail;
    }

    public void setMissionFail(Map<String, Integer> missionFail) {
        this.missionFail = missionFail;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }
}
