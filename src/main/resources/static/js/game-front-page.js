// /js/game-front-page.js
const urlParams = new URLSearchParams(window.location.search);
const roomId = urlParams.get("roomId");
const playerName = sessionStorage.getItem("playerName");

let players = [];
let myRole = null;
let leaderId = null;
let currentRound = 1;
let selectedOrder = [];

const positionMap = {
  5: [
    { top: '3%', left: '55%' },
    { top: '3%', right: '55%' },
    { top: '40%', left: '20%' },
    { top: '40%', right: '20%' },
    { bottom: '30px', left: '50%', transform: 'translateX(-50%)' }
  ],
  6: [
    { top: '55%', left: '15%' },
    { top: '15%', left: '15%' },
    { top: '3%', left: '50%', transform: 'translateX(-50%)' },
    { top: '15%', right: '15%' },
    { top: '55%', right: '15%' },
    { bottom: '5%', left: '50%', transform: 'translateX(-50%)' },
  ],
  7: [
    { top: '55%', left: '75%' },
    { top: '15%', left: '75%' },
    { top: '5%', right: '55%' },
    { top: '5%', left: '55%' },
    { top: '15%', left: '10%' },
    { top: '55%', left: '10%' },
    { bottom: '30px', left: '50%', transform: 'translateX(-50%)' },
  ],
  8: [
    { bottom: '10%', left: '25%' },
    { top: '30%', left: '10%' },
    { top: '10%', left: '25%' },
    { top: '5%', left: '50%', transform: 'translateX(-50%)' },
    { top: '10%', right: '25%' },
    { top: '30%', right: '10%' },
    { bottom: '10%', right: '25%' },
    { bottom: '5%', left: '50%', transform: 'translateX(-50%)' },
  ],
  9: [
    { bottom: '8%', left: '30%' },
    { bottom: '15%', left: '15%' },
    { bottom: '55%', left: '20%' },
    { top: '5%', left: '35%' },
    { top: '5%', right: '35%' },
    { bottom: '55%', right: '20%' },
    { bottom: '15%', right: '15%' },
    { bottom: '8%', right: '30%' },
    { bottom: '5%', left: '50%', transform: 'translateX(-50%)' },
  ],
  10: [
    { top: '3%', left: '50%', transform: 'translateX(-50%)' },
    { top: '10%', right: '15%' },
    { top: '30%', right: '3%' },
    { top: '60%', right: '3%' },
    { bottom: '10%', right: '15%' },
    { bottom: '3%', left: '50%', transform: 'translateX(-50%)' },
    { bottom: '10%', left: '15%' },
    { top: '60%', left: '3%' },
    { top: '30%', left: '3%' },
    { top: '10%', left: '15%' },
  ],
};

const expeditionConfig = {
  5: { totalRounds: 5, picks: [2, 2, 2, 3, 3] },
  6: { totalRounds: 6, picks: [2, 2, 3, 3, 4, 3] },
  7: { totalRounds: 6, picks: [3, 3, 4, 4, 4, 4] },
  8: { totalRounds: 7, picks: [3, 3, 4, 4, 4, 5, 5] },
  9: { totalRounds: 7, picks: [4, 4, 4, 5, 5, 5, 5] },
};

function getMaxPick(currentRound, count) {
  const config = expeditionConfig[count];
  if (!config) return 2;
  return config.picks[currentRound - 1] || config.picks.at(-1);
}

function reorderPlayers(arr) {
  const meIdx = arr.findIndex(p => p.name === playerName);
  if (meIdx === -1) return arr;
  const ordered = [];
  for (let i = 1; i < arr.length; i++) ordered.push(arr[(meIdx + i) % arr.length]);
  ordered.push(arr[meIdx]);
  return ordered;
}

function renderPlayers(arr) {
  const container = document.getElementById("player-container");
  container.innerHTML = "";

  const ordered = reorderPlayers(arr);
  const positions = positionMap[ordered.length] || [];

  ordered.forEach((p, idx) => {
    const isSelf = p.name === playerName;
    const isLeader = p.name === leaderId;
    const card = document.createElement("div");
    card.className = `player-card${isLeader ? " leader" : ""}${isSelf ? " player-self" : ""}`;
    Object.entries(positions[idx] || {}).forEach(([k, v]) => card.style[k] = v);
    card.innerHTML = `
      <div class="avatar"><img src="/images/${p.avatar}" alt="${p.name}"></div>
      <div class="name">${p.name}</div>
      ${isSelf && p.role ? `<div class="role-label">角色：${p.role}</div>` : ""}
    `;
    container.appendChild(card);
  });

  document.getElementById("leader-action")?.classList.toggle("hidden", leaderId !== playerName);
}

function openSelectModal() {
  const maxPick = getMaxPick(currentRound, players.length);
  const candidates = players;
  const listEl = document.getElementById('candidate-list');
  listEl.innerHTML = '';
  selectedOrder = [];

  candidates.forEach(p => {
    const li = document.createElement('li');
    li.dataset.name = p.name;
    li.innerHTML = `<span class="order"></span><span>${p.name}</span>`;
    li.addEventListener('click', () => toggleSelect(li, maxPick));
    listEl.appendChild(li);
  });

  document.getElementById('select-title').textContent = `請選擇 ${maxPick} 名出戰人員 (剩 ${maxPick})`;
  document.getElementById('select-modal').classList.remove('hidden');
}

function toggleSelect(li, maxPick) {
  const name = li.dataset.name;
  const idx = selectedOrder.indexOf(name);
  if (idx === -1) {
    if (selectedOrder.length >= maxPick) return;
    selectedOrder.push(name);
  } else {
    selectedOrder.splice(idx, 1);
  }

  document.querySelectorAll('#candidate-list li').forEach(li2 => {
    const orderEl = li2.querySelector('.order');
    const i = selectedOrder.indexOf(li2.dataset.name);
    if (i === -1) {
      li2.classList.remove('selected'); orderEl.textContent = '';
    } else {
      li2.classList.add('selected'); orderEl.textContent = i + 1;
    }
  });

  const remain = maxPick - selectedOrder.length;
  document.getElementById('select-title').textContent = `請選擇 ${maxPick} 名出戰人員 (剩 ${remain})`;
}

function closeSelectModal() {
  document.getElementById('select-modal').classList.add('hidden');
}

async function confirmSelection() {
  const maxPick = getMaxPick(currentRound, players.length);
  if (selectedOrder.length !== maxPick) {
    alert(`請選滿 ${maxPick} 人！`);
    return;
  }
  try {
    await fetch(`/api/room/${roomId}/start-vote`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ leader: playerName, expedition: selectedOrder })
    });
    closeSelectModal();
    window.location.href = `/vote.html?roomId=${roomId}`;
  } catch (err) {
    console.error("❌ 無法開始投票", err);
    alert("後端連線失敗，請稍後再試！");
  }
}

function applyRolesToPlayers(roleMap) {
  players = players.map(p => ({ ...p, role: roleMap[p.name]?.name }));
  renderPlayers(players);
  const self = players.find(p => p.name === playerName);
  if (self) {
    myRole = self.role;
    localStorage.setItem('myRole', myRole || "");
  }
}

async function fetchPlayers() {
  try {
    const res = await fetch(`/api/room/${roomId}/players`);
    players = await res.json();
    renderPlayers(players);
  } catch (err) {
    console.error("❌ 無法載入玩家資料", err);
  }
}

async function fetchAssignedRoles() {
  try {
    const res = await fetch(`/api/room/${roomId}/roles`);
    if (!res.ok) throw new Error();
    const { assignedRoles, currentLeader } = await res.json();
    leaderId = currentLeader;
    applyRolesToPlayers(assignedRoles);
  } catch (err) {
    console.error("❌ 無法取得角色資料", err);
  }
}

function connectWebSocket() {
  if (!window.stompClient) {
    const socket = new SockJS('/ws');
    window.stompClient = Stomp.over(socket);
  }
  const stompClient = window.stompClient;
  stompClient.connect({}, () => {
    stompClient.subscribe(`/topic/room/${roomId}`, async msg => {
      const body = msg.body.trim();
      if (body === "allSkillUsed") {
        await fetchAssignedRoles();
        await fetchMissionSummary();
      }
      if (body === "startRealGame") {
        await fetchAssignedRoles();
        window.location.href = `/game-front-page.html?roomId=${roomId}`;
      }
    });

    stompClient.subscribe(`/topic/leader/${roomId}`, msg => {
      leaderId = msg.body;
      renderPlayers(players);
    });

    stompClient.subscribe(`/topic/vote/${roomId}`, () => {
      if (!location.pathname.startsWith("/vote")) {
        window.location.href = `/vote.html?roomId=${roomId}`;
      }
    });
  });
}

function showRoundResult(success, fail) {
  const resultText = `本回合結果：成功 ${success} 張，失敗 ${fail} 張`;
  const popup = document.getElementById("round-result-popup");
  const text = document.getElementById("round-result-text");
  text.textContent = resultText;
  popup.classList.remove("hidden");
  setTimeout(() => popup.classList.add("hidden"), 5000);
}

function updateOverallStats(successCount, failCount) {
  document.getElementById("success-count").textContent = successCount;
  document.getElementById("fail-count").textContent = failCount;
}

function updateRoundLabel(round, totalRounds) {
  const label = document.getElementById("round-label");
  if (label && round) {
    label.textContent = totalRounds ? `第 ${round} 輪 / 共 ${totalRounds} 輪` : `第 ${round} 輪`;
  }
}

async function fetchMissionSummary() {
  try {
    const res = await fetch(`/api/room/${roomId}`);
    const room = await res.json();
    currentRound = room.currentRound;
    const count = room.playerCount || players.length;
    const config = expeditionConfig[count] || { totalRounds: 5 };
    const totalRounds = config.totalRounds;
    updateRoundLabel(currentRound, totalRounds);
    updateOverallStats(room.successCount || 0, room.failCount || 0);

    const skip = sessionStorage.getItem("skipMission");
    if (skip === "true") {
      sessionStorage.removeItem("skipMission");
      return;
    }

    const lastRound = currentRound - 1;
    if (room.missionResults && room.missionResults[lastRound]) {
      const { successCount, failCount } = room.missionResults[lastRound];
      showRoundResult(successCount, failCount);
    }

    if (currentRound > totalRounds) {
      window.location.href = `/game-end.html?roomId=${roomId}`;
      return;
    }
  } catch (err) {
    console.error("❌ 無法取得任務結果", err);
  }
}

async function startVoiceCall() {
  const container = document.getElementById("jitsi-container");
  container.innerHTML = "";

  for (let i = 1; i <= 10; i++) {
    const roomName = `room${i}`;
    try {
      const res = await fetch(`/api/check-daily-room/${roomName}`);
      const data = await res.json();

      if (data.participantCount === 0) {
        const iframe = document.createElement("iframe");
        iframe.src = `https://jitsigame.daily.co/${roomName}?video=off&audioSource=mic&videoSource=none`;
        iframe.allow = "microphone; camera; autoplay; display-capture";
        iframe.width = "400";
        iframe.height = "300";
        iframe.style.border = "0";
        container.appendChild(iframe);
        return;
      }
    } catch (err) {
      console.error(`❌ 檢查 room${i} 時失敗`, err);
    }
  }

  alert("❌ 所有語音房間都已滿，請稍後再試！");
}


document.addEventListener("DOMContentLoaded", async () => {
  try {
    await fetch(`/api/room/${roomId}/assign-roles`, { method: 'POST' });

    const res = await fetch(`/api/room/${roomId}`);
    if (res.ok) {
      const room = await res.json();
      localStorage.setItem("roomName", room.roomName || "");
    }

    await fetchPlayers();
    await fetchAssignedRoles();

    const avatar = sessionStorage.getItem("playerAvatar");
    if (playerName) localStorage.setItem("username", playerName);
    if (avatar) localStorage.setItem("selectedAvatar", avatar);

    const my = players.find(p => p.name === playerName);
    if (my && my.role) {
      localStorage.setItem("myRole", my.role);
    }

    document.getElementById("select-expedition-btn")?.addEventListener("click", openSelectModal);
    document.getElementById("join-voice-btn")?.addEventListener("click", startVoiceCall);

    connectWebSocket();
    await fetchMissionSummary();
  } catch (err) {
    console.error("❌ 初始化失敗", err);
  }
});
