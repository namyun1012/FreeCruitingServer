/* chat.js — FreeCruiting 채팅 클라이언트 */

let stompClient = null;

// Mustache 템플릿에서 주입되는 값 (chat.mustache 인라인 script 블록에서 설정)
// window.CHAT_CONFIG = { partyId, roomId, userName }
const { partyId: currentPartyId, roomId: currentRoomId, userName: username } = window.CHAT_CONFIG;

/* ── 초기화 ── */
document.addEventListener('DOMContentLoaded', () => {
    connect();

    document.getElementById('message-input').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') sendMessage();
    });
});

/* ── WebSocket 연결 ── */
function connect() {
    if (stompClient?.connected) return;

    loadPreviousMessages(currentRoomId).then(() => {
        const socket = new SockJS('/ws-stomp');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // 콘솔 노이즈 제거

        stompClient.connect({}, onConnected, onError);
    });
}

function onConnected(frame) {
    console.log('Connected:', frame);
    document.getElementById('disconnectButton').style.display = 'inline-flex';

    stompClient.subscribe(`/sub/chat/room/${currentRoomId}`, (message) => {
        showMessage(JSON.parse(message.body));
    });
}

function onError(error) {
    console.error('STOMP Error:', error);
    alert('채팅 연결에 실패했습니다. 콘솔을 확인해주세요.');
    document.getElementById('disconnectButton').style.display = 'none';
}

/* ── 연결 해제 ── */
function disconnect() {
    if (stompClient?.connected) {
        stompClient.disconnect();
        document.getElementById('chat-window').innerHTML = '';
        document.getElementById('disconnectButton').style.display = 'none';
        stompClient = null;
        console.log('Disconnected');
    }
}

/* ── 이전 메시지 불러오기 ── */
async function loadPreviousMessages(roomId) {
    try {
        const response = await fetch(`/api/v1/chat/rooms/${roomId}/messages`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const messages = await response.json();
        messages.forEach(showMessage);
    } catch (err) {
        console.error('Failed to load previous messages:', err);
    }
}

/* ── 메시지 전송 ── */
function sendMessage() {
    const input = document.getElementById('message-input');
    const content = input.value.trim();

    if (!content) return;

    if (!stompClient?.connected) {
        alert('채팅방에 먼저 입장해주세요.');
        return;
    }

    const chatMessage = {
        type: 'CHAT',
        partyId: currentPartyId,
        roomId: currentRoomId,
        sender: username,
        message: content,
    };

    stompClient.send('/pub/chat/message', {}, JSON.stringify(chatMessage));
    input.value = '';
}

/* ── 메시지 렌더링 ── */
function showMessage(message) {
    const chatWindow = document.getElementById('chat-window');
    const isMine = message.sender === username;

    const row = document.createElement('div');
    row.classList.add('message-row', isMine ? 'mine' : 'theirs');

    const bubble = document.createElement('div');
    bubble.classList.add('bubble', isMine ? 'bubble-mine' : 'bubble-theirs');

    if (!isMine) {
        const senderEl = document.createElement('div');
        senderEl.classList.add('sender-name');
        senderEl.textContent = message.sender;
        bubble.appendChild(senderEl);
    }

    const textEl = document.createElement('span');
    textEl.textContent = message.message; // XSS 방지: innerHTML 대신 textContent 사용
    bubble.appendChild(textEl);

    row.appendChild(bubble);
    chatWindow.appendChild(row);
    chatWindow.scrollTop = chatWindow.scrollHeight;
}