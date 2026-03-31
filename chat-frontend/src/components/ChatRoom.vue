<template>
  <div class="chat-app">
    <!-- 側邊欄 -->
    <aside class="sidebar">
      <div class="online-header">
        <span class="dot"></span> 線上 {{ onlineCount }} 人
      </div>
      <ul class="user-list">
        <li
          v-for="user in onlineUsers"
          :key="user"
          :class="{
            active: privateTarget === user,
            self: user === username
          }"
          @click="selectUser(user)"
        >
          {{ user }}{{ user === username ? '（我）' : '' }}
          <span v-if="unreadMap[user]" class="badge">{{ unreadMap[user] }}</span>
        </li>
      </ul>
      <button v-if="privateTarget" @click="privateTarget = null" class="btn-back">
        ← 回公開聊天室
      </button>
    </aside>

    <!-- 聊天主區域 -->
    <main class="chat-main">
      <div class="chat-header">
        {{ privateTarget ? `私訊：${privateTarget}` : '公開聊天室' }}
      </div>

      <div class="messages" ref="msgBox">
        <div
          v-for="(msg, i) in currentMessages"
          :key="i"
          :class="['msg', msg.sender === username ? 'mine' : 'theirs']"
        >
          <span class="sender">{{ msg.sender }}</span>
          <span class="bubble">{{ msg.content }}</span>
          <span class="time">{{ msg.time }}</span>
        </div>
      </div>

      <div class="input-bar">
        <input
          v-model="text"
          @keyup.enter="send"
          :placeholder="privateTarget ? `私訊 ${privateTarget}...` : '輸入訊息...'"
        />
        <button @click="send">送出</button>
      </div>
    </main>

    <!-- 右上角通知 -->
    <transition name="slide">
      <div v-if="notice" class="notice">{{ notice }}</div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useWebSocket } from '@/composables/useWebSocket'

const username = ref<string>('User_' + Math.floor(Math.random() * 1000))
const privateTarget = ref<string | null>(null)
const text = ref<string>('')
const msgBox = ref<HTMLElement | null>(null)
const notice = ref<string>('')

const {
  publicMessages,
  privateMessages,
  onlineUsers,
  onlineCount,
  unreadMap,
  connect,
  sendPublic,
  sendPrivate,
  disconnect
} = useWebSocket(username.value)

// 目前顯示的訊息
const currentMessages = computed(() =>
  privateTarget.value
    ? (privateMessages.value[privateTarget.value] || [])
    : publicMessages.value
)

// 點選用戶切換私訊
function selectUser(user: string): void {
  if (user === username.value) return
  privateTarget.value = user
  unreadMap.value[user] = 0
}

// 發送訊息
function send(): void {
  const content = text.value.trim()
  if (!content) return
  if (privateTarget.value) {
    sendPrivate(privateTarget.value, content)
  } else {
    sendPublic(content)
  }
  text.value = ''
}

// 顯示通知
function showNotice(msg: string): void {
  notice.value = msg
  setTimeout(() => { notice.value = '' }, 3000)
}

// 監聽私訊未讀，顯示通知
watch(
  privateMessages,
  (val) => {
    for (const [user, msgs] of Object.entries(val)) {
      if (user !== privateTarget.value && (unreadMap.value[user] || 0) > 0) {
        const last = msgs[msgs.length - 1]
        showNotice(`${user}：${last.content}`)
      }
    }
  },
  { deep: true }
)

// 自動捲到底部
watch(
  currentMessages,
  async () => {
    await nextTick()
    if (msgBox.value) {
      msgBox.value.scrollTop = msgBox.value.scrollHeight
    }
  },
  { deep: true }
)

onMounted(connect)
onUnmounted(disconnect)
</script>

<style scoped>
.chat-app {
  display: flex;
  height: 100vh;
  font-size: 14px;
  position: relative;
}

.sidebar {
  width: 200px;
  border-right: 1px solid #e5e7eb;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.online-header {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  display: inline-block;
}

.user-list {
  list-style: none;
  padding: 0;
  margin: 0;
  flex: 1;
  overflow-y: auto;
}

.user-list li {
  padding: 7px 10px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-list li:hover { background: #f3f4f6; }
.user-list li.active { background: #eff6ff; font-weight: 500; }
.user-list li.self { color: #9ca3af; cursor: default; }

.badge {
  background: #ef4444;
  color: white;
  border-radius: 10px;
  padding: 1px 6px;
  font-size: 11px;
}

.btn-back {
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #d1d5db;
  cursor: pointer;
  background: white;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  padding: 14px 20px;
  font-weight: 500;
  border-bottom: 1px solid #e5e7eb;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.msg {
  display: flex;
  flex-direction: column;
  max-width: 65%;
}

.msg.mine { align-self: flex-end; align-items: flex-end; }
.msg.theirs { align-self: flex-start; align-items: flex-start; }

.sender { font-size: 11px; color: #9ca3af; margin-bottom: 2px; }

.bubble {
  padding: 8px 14px;
  border-radius: 14px;
  line-height: 1.5;
}

.mine .bubble { background: #3b82f6; color: white; }
.theirs .bubble { background: #f3f4f6; color: #111; }

.time { font-size: 11px; color: #9ca3af; margin-top: 3px; }

.input-bar {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid #e5e7eb;
}

.input-bar input {
  flex: 1;
  padding: 9px 14px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  outline: none;
  font-size: 14px;
}

.input-bar input:focus { border-color: #3b82f6; }

.input-bar button {
  padding: 9px 18px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
}

.notice {
  position: absolute;
  top: 16px;
  right: 16px;
  background: #1f2937;
  color: white;
  padding: 10px 16px;
  border-radius: 10px;
  font-size: 13px;
  z-index: 100;
}

.slide-enter-active, .slide-leave-active { transition: all 0.3s; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-8px); }
</style>