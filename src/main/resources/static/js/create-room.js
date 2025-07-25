/**
 * create-room.js
 *
 * ▶ 此檔案負責「建立房間」頁面的互動邏輯與前端驗證。
 *
 * ▶ 功能摘要：
 *   - 依照房間類型切換密碼輸入欄（公開/私人）
 *   - 檢查欄位有效性（房名、人數、密碼等）
 *   - 透過 fetch 向後端 `/api/create-room` 發送建立請求
 *   - 成功建立後導向 `/room/{roomId}` 頁面
 *
 * ▶ 相依條件：
 *   - 使用者需先登入並儲存 `playerName` 到 sessionStorage
 *   - 對應 HTML 結構需具備對應欄位（room-name、room-type、room-password 等）
 *
 * ▶ 關聯後端控制器：
 *   - `RoomController.createRoom(...)` 會接收本前端傳送的資料並創建房間
 */

document.addEventListener("DOMContentLoaded", function () {
  const roomType = document.getElementById("room-type");
  const passwordContainer = document.getElementById("password-container");

  roomType.addEventListener("change", function () {
    if (roomType.value === "private") {
      passwordContainer.style.display = "flex";
    } else {
      passwordContainer.style.display = "none";
    }
  });
});

// ✅ 使用原本的 createRoom 函數邏輯（完整複製貼上）
async function createRoom() {
  const roomName = document.getElementById("room-name").value;
  const roomType = document.getElementById("room-type").value;
  const roomPassword = document.getElementById("room-password").value;
  const playerCount = parseInt(document.getElementById("player-count").value, 10);

  if (!roomName) {
    alert("請輸入房間名稱！");
    return;
  }
  if (roomType === "private" && !roomPassword) {
    alert("私人房間需要輸入密碼！");
    return;
  }
  if (isNaN(playerCount) || playerCount < 5 || playerCount > 10) {
    alert("玩家人數應在 5 到 10 之間！");
    return;
  }

  // 從 sessionStorage 取得登入玩家名稱
  const playerName = sessionStorage.getItem("playerName");
  if (!playerName) {
    alert("請先登入並設定玩家名稱！");
    return;
  }

  const roomData = {
    roomName,
    roomType,
    roomPassword: roomType === "private" ? roomPassword : null,
    playerCount,
  };

  try {
    // 傳送 playerName 作為 query 參數
    const response = await fetch(`/api/create-room?playerName=${encodeURIComponent(playerName)}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(roomData),
    });
    const result = await response.json();

    if (response.ok) {
      const room = result; // 從後端返回的房間資訊
      window.location.href = `/room/${room.id}`;
    } else {
      alert(result.message || "創建房間失敗，請稍後再試！");
    }
  } catch (error) {
    console.error("創建房間時發生錯誤:", error);
    alert("系統錯誤，請稍後再試！");
  }
}

// ✅ 返回按鈕動作
function goBack() {
  window.location.href = "/game-lobby";
}
