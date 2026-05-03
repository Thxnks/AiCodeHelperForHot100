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
      <div class="message-time">{{ displayTime(timestamp) }}</div>
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
    displayTime(date) {
      const timeText = formatTime(date)
      return timeText
        .replace('刚刚', 'Just now')
        .replace(' 分钟前', ' min ago')
        .replace(' 小时前', ' hr ago')
    },
    handleRoleAvatarError(event) {
      event.target.src = this.aiRole.avatarFallback || '/characters/sakurajima-mai.png'
    }
  }
}
</script>

<style scoped>
.chat-message {
  display: flex;
  max-width: 820px;
  margin: 0 auto 14px;
  padding: 0 24px;
  animation: messageIn 220ms ease-out both;
}

.message-avatar {
  margin: 0 12px 0 0;
  display: flex;
  align-items: flex-start;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.user-avatar {
  background: var(--text, #1f2421);
  color: #fff;
  font-size: 12px;
}

.role-avatar {
  object-fit: cover;
  border: 1px solid var(--border, rgba(0, 0, 0, 0.08));
  box-shadow: none;
}

.message-content {
  max-width: min(74%, 620px);
  min-width: 100px;
}

.message-bubble {
  padding: 11px 14px;
  border-radius: 16px;
  word-break: break-word;
  box-shadow: none;
  font-size: 14px;
  line-height: 1.55;
}

.user-message {
  justify-content: flex-end;
}

.user-message .message-avatar {
  order: 2;
  margin: 0 0 0 12px;
}

.user-message .message-content {
  order: 1;
}

.user-message .message-bubble {
  background: var(--app-text, #111827);
  color: #fff;
  border-bottom-right-radius: 8px;
}

.ai-message .message-bubble {
  background: #f7f7f5;
  color: var(--app-text, #111827);
  border: 1px solid var(--app-border, #e5e7eb);
  border-bottom-left-radius: 8px;
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
  line-height: 1.55;
}

.message-markdown :deep(ul),
.message-markdown :deep(ol) {
  margin: 0 0 10px 0;
  padding-left: 22px;
}

.message-markdown :deep(li) {
  margin: 4px 0;
  line-height: 1.55;
}

.message-markdown :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 6px;
}

.message-markdown :deep(pre) {
  background: #111815;
  color: #f8fafc;
  padding: 12px;
  border-radius: 12px;
  font-family: var(--font-claude-mono);
}

.message-time {
  margin-top: 3px;
  font-size: 12px;
  color: var(--app-faint, #9ca3af);
  padding: 0 4px;
}

@keyframes messageIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message .message-time {
  text-align: right;
}

@media (max-width: 768px) {
  .message-content {
    max-width: 86%;
  }
  .chat-message {
    padding: 0 10px;
  }
}

/* Coaching chat message refinement */
.chat-message {
  max-width: 860px;
  margin-bottom: 12px;
  animation: messageIn 220ms ease-out both;
}

.avatar {
  width: 36px;
  height: 36px;
  font-weight: 600;
}

.message-content {
  max-width: min(76%, 660px);
}

.message-bubble {
  padding: 16px 18px;
  border-radius: 20px;
  font-family: var(--font-claude-ui);
  font-size: 14px;
  line-height: 1.55;
}

.ai-message .message-bubble {
  background: rgba(255, 255, 255, 0.82);
  border-color: var(--claude-border, #e5e7eb);
}

.user-message .message-bubble {
  background: var(--claude-button, #111827);
}

.message-time {
  color: var(--claude-faint, #9ca3af);
  font-size: 12px;
}

@media (prefers-color-scheme: dark) {
  .ai-message .message-bubble {
    background: rgba(29, 35, 31, 0.88);
    border-color: rgba(255, 255, 255, 0.08);
  }
}
</style>
