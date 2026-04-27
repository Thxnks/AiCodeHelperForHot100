<template>
  <div class="chat-message" :class="{ 'user-message': isUser, 'ai-message': !isUser }">
    <div class="message-avatar">
      <div v-if="isUser" class="avatar user-avatar">我</div>
      <img
        v-else
        class="avatar role-avatar"
        :src="roleAvatar"
        :alt="aiRole.name"
        @error="handleRoleAvatarError"
      />
    </div>

    <div class="message-content">
      <div class="message-bubble">
        <pre v-if="isUser" class="message-text">{{ message }}</pre>
        <div v-else class="message-markdown" v-html="renderedMessage"></div>
      </div>
      <div class="message-time">{{ formatTime(timestamp) }}</div>
    </div>
  </div>
</template>

<script>
import { formatTime } from '../utils/index.js'
import { marked } from 'marked'

export default {
  name: 'ChatMessage',
  props: {
    message: { type: String, required: true },
    isUser: { type: Boolean, default: false },
    timestamp: { type: Date, default: () => new Date() },
    aiRole: {
      type: Object,
      default: () => ({ name: '助手', avatar: '/characters/sakurajima-mai.png' })
    }
  },
  computed: {
    roleAvatar() {
      return this.aiRole.avatar || this.aiRole.image || '/characters/sakurajima-mai.png'
    },
    renderedMessage() {
      if (this.isUser) return this.message
      marked.setOptions({ breaks: true, gfm: true })
      return marked(this.message)
    }
  },
  methods: {
    formatTime,
    handleRoleAvatarError(event) {
      event.target.src = this.aiRole.avatarFallback || '/characters/sakurajima-mai.png'
    }
  }
}
</script>

<style scoped>
.chat-message {
  display: flex;
  margin-bottom: 18px;
  padding: 0 20px;
}

.message-avatar {
  margin: 0 10px;
  display: flex;
  align-items: flex-start;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.user-avatar {
  background: linear-gradient(135deg, #2563eb, #0ea5e9);
  color: #fff;
}

.role-avatar {
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.45);
}

.message-content {
  max-width: 72%;
  min-width: 100px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  word-break: break-word;
}

.user-message {
  justify-content: flex-end;
}

.user-message .message-avatar {
  order: 2;
}

.user-message .message-content {
  order: 1;
}

.user-message .message-bubble {
  background: linear-gradient(130deg, #2563eb 0%, #0ea5e9 100%);
  color: #fff;
  border-bottom-right-radius: 6px;
}

.ai-message .message-bubble {
  background: #f1f5f9;
  color: #0f172a;
  border: 1px solid #e2e8f0;
  border-bottom-left-radius: 6px;
}

.message-text {
  margin: 0;
  white-space: pre-wrap;
  font: inherit;
}

.message-markdown :deep(pre) {
  overflow-x: auto;
}

.message-markdown :deep(p) {
  margin: 0 0 10px;
  line-height: 1.7;
}

.message-markdown :deep(ul),
.message-markdown :deep(ol) {
  margin: 0 0 10px 0;
  padding-left: 22px;
}

.message-markdown :deep(li) {
  margin: 6px 0;
  line-height: 1.7;
}

.message-markdown :deep(code) {
  background: rgba(15, 23, 42, 0.08);
  padding: 2px 6px;
  border-radius: 6px;
}

.message-time {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
  padding: 0 4px;
}

.user-message .message-time {
  text-align: right;
}

@media (max-width: 768px) {
  .message-content {
    max-width: 85%;
  }
  .chat-message {
    padding: 0 10px;
  }
}
</style>
