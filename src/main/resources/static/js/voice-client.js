let mediaRecorder;
let socket;
let username = localStorage.getItem("username") || "guest";

// 建立 WebSocket 連線
function connectWebSocket() {
  socket = new WebSocket("ws://localhost:8080/voice/" + username);

  socket.onopen = () => console.log("🎙 WebSocket 連線成功！");
  socket.onerror = (err) => console.error("WebSocket 錯誤", err);
  socket.onmessage = (event) => {
    // 你之後可以在這裡處理收到的音訊（播放）
    console.log("收到語音資料");
  };
}

// 開始錄音
async function startRecording() {
  const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
  mediaRecorder = new MediaRecorder(stream);

 mediaRecorder.ondataavailable = (e) => {
  const reader = new FileReader();
  reader.onloadend = () => {
    const base64 = reader.result.split(',')[1]; // 去掉 data:audio/webm;base64, 開頭
    if (stompClient && stompClient.connected) {
      stompClient.send("/app/voice", {}, base64);
      console.log("📤 發送語音 base64 字串，長度：", base64.length);
    }
  };
  reader.readAsDataURL(e.data);
};
  mediaRecorder.start(250); // 每 250ms 傳送一次
  console.log("🎤 開始錄音...");
}

// 停止錄音
function stopRecording() {
  if (mediaRecorder && mediaRecorder.state !== "inactive") {
    mediaRecorder.stop();
    console.log("🛑 停止錄音");
  }
}

// 初始化 WebSocket
connectWebSocket();
