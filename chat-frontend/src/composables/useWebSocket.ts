import { ref } from 'vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

interface ChatMessage {
  sender: string
  content: string
  time: string
}

interface PrivateMessage extends ChatMessage {
  to: string
}

interface OnlineStatus {
  users: string[]
  count: number
}

export function useWebSocket(username: string) {
  const publicMessages = ref<ChatMessage[]>([])
  const privateMessages = ref<Record<string, ChatMessage[]>>({})
  const onlineUsers = ref<string[]>([])
  const onlineCount = ref<number>(0)
  const unreadMap = ref<Record<string, number>>({})

  let client: Client | null = null

  function now(): string {
    return new Date().toLocaleTimeString('zh-TW', {
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  function connect(): void {
    client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),

      onConnect: () => {
        // 訂閱公開聊天室
        client!.subscribe('/topic/chatroom', ({ body }) => {
          const msg: ChatMessage = JSON.parse(body)
          publicMessages.value.push(msg)
        })

        // 訂閱私訊（只收自己的）
        client!.subscribe(`/topic/private.${username}`, ({ body }) => {
          const msg: ChatMessage = JSON.parse(body)
          const sender = (msg as PrivateMessage).sender
          if (!privateMessages.value[sender]) {
            privateMessages.value[sender] = []
          }
          privateMessages.value[sender].push(msg)
          unreadMap.value[sender] = (unreadMap.value[sender] || 0) + 1
        })

        // 訂閱線上人數
        client!.subscribe('/topic/online', ({ body }) => {
          const data: OnlineStatus = JSON.parse(body)
          onlineUsers.value = data.users
          onlineCount.value = data.count
        })

        // 通知伺服器我上線了
        client!.publish({
          destination: '/app/join',
          body: JSON.stringify({ username })
        })
      },

      onStompError: (frame) => {
        console.error('WebSocket 錯誤:', frame)
      }
    })

    client.activate()
  }

  function sendPublic(content: string): void {
    client?.publish({
      destination: '/app/chat',
      body: JSON.stringify({ sender: username, content, time: now() })
    })
  }

  function sendPrivate(to: string, content: string): void {
    const msg: PrivateMessage = { sender: username, to, content, time: now() }
    client?.publish({
      destination: '/app/private',
      body: JSON.stringify(msg)
    })
    // 把自己送出的也存本地
    if (!privateMessages.value[to]) {
      privateMessages.value[to] = []
    }
    privateMessages.value[to].push(msg)
  }

  function disconnect(): void {
    client?.deactivate()
  }

  return {
    publicMessages,
    privateMessages,
    onlineUsers,
    onlineCount,
    unreadMap,
    connect,
    sendPublic,
    sendPrivate,
    disconnect
  }
}