<template>
  <div class="chat-input">
    <div class="input-container">
      <textarea
        ref="inputRef"
        v-model="inputMessage"
        :placeholder="placeholder"
        :disabled="disabled"
        class="input-textarea"
        rows="1"
        @keydown="handleKeyDown"
        @input="adjustHeight"
      />
      <button
        :disabled="!loading && !inputMessage.trim()"
        @click="loading ? stopMessage() : sendMessage()"
        class="send-button"
        :class="{ loading }"
        :aria-label="loading ? '停止生成' : '发送消息'"
      >
        <span v-if="loading" class="stop-icon"></span>
        <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M2 21l21-9L2 3v7l15 2-15 2v7z" fill="currentColor"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ChatInput',
  props: {
    disabled: {
      type: Boolean,
      default: false
    },
    placeholder: {
      type: String,
      default: 'Ask about algorithms, backend, or your selected Hot100 problem...'
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      inputMessage: ''
    }
  },
  methods: {
    sendMessage() {
      if (this.inputMessage.trim() && !this.disabled) {
        this.$emit('send-message', this.inputMessage.trim())
        this.inputMessage = ''
        this.adjustHeight()
      }
    },
    handleKeyDown(event) {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault()
        this.sendMessage()
      }
    },
    stopMessage() {
      this.$emit('stop-message')
    },
    adjustHeight() {
      this.$nextTick(() => {
        const textarea = this.$refs.inputRef
        textarea.style.height = 'auto'
        textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px'
      })
    },
    focus() {
      this.$refs.inputRef.focus()
    }
  },
  mounted() {
    this.adjustHeight()
  }
}
</script>

<style scoped>
.chat-input {
  padding: 12px 20px 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.88), #ffffff);
  border-top: 1px solid var(--app-border, #e5e7eb);
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  width: min(780px, calc(100% - 32px));
  max-width: 800px;
  min-width: 0;
  margin: 0 auto;
  padding: 8px 8px 8px 16px;
  border: 1px solid var(--app-border, #e5e7eb);
  border-radius: 22px;
  background: #f7f7f5;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.input-container:focus-within {
  border-color: var(--app-border-strong, #d1d5db);
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(17, 24, 39, 0.07);
}

.input-textarea {
  flex: 1;
  min-width: 0;
  padding: 8px 0;
  border: 0;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
  outline: none;
  min-height: 46px;
  max-height: 132px;
  overflow-y: auto;
  background: transparent;
  color: var(--app-text, #111827);
}

.input-textarea:focus {
  box-shadow: none;
}

.input-textarea:disabled {
  color: var(--app-faint, #9ca3af);
  cursor: not-allowed;
}

.send-button {
  width: 40px;
  height: 40px;
  background: var(--app-text, #111827);
  border: none;
  border-radius: 50%;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
  box-shadow: none;
}

.send-button:hover:not(:disabled) {
  transform: scale(1.04);
  background: #1f2937;
}

.send-button:disabled {
  background: #d1d5db;
  cursor: not-allowed;
  color: #6b7280;
  opacity: 1;
}

.send-button.loading {
  background: var(--app-text, #111827);
}

.send-button:active:not(:disabled) {
  transform: scale(0.96);
}

.stop-icon {
  width: 13px;
  height: 13px;
  border-radius: 4px;
  background: currentColor;
  display: block;
}

@media (max-width: 768px) {
  .chat-input {
    padding: 15px;
  }
  
  .input-container {
    gap: 8px;
  }
  
  .input-textarea {
    font-size: 16px; /* 防止在移动设备上自动缩放 */
  }
}

/* Claude-like composer refinement */
.chat-input {
  padding: 12px 18px 16px;
  background: rgba(250, 249, 245, 0.78);
  border-top: 1px solid var(--claude-border, #e5e7eb);
  backdrop-filter: blur(12px);
}

.input-container {
  width: min(860px, calc(100% - 28px));
  max-width: 860px;
  min-height: 60px;
  padding: 8px 8px 8px 16px;
  border: 1px solid var(--claude-border, #e5e7eb);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 1px 2px rgba(17, 24, 39, 0.04);
  transition: all 0.2s ease;
}

.input-container:focus-within {
  border-color: var(--claude-border-strong, #d1d5db);
  box-shadow: 0 10px 28px rgba(17, 24, 39, 0.08);
}

.input-textarea {
  min-height: 42px;
  max-height: 128px;
  color: var(--claude-text, #111827);
  font-family: var(--font-claude-ui);
  font-size: 14px;
  line-height: 1.5;
}

.input-textarea::placeholder {
  color: var(--claude-faint, #9ca3af);
}

.send-button {
  width: 40px;
  height: 40px;
  background: var(--claude-button, #111827);
  transition: all 0.2s ease;
}

.send-button:hover:not(:disabled) {
  transform: scale(1.03);
  background: #1f2937;
}

.send-button:disabled {
  background: #e5e7eb;
  color: #9ca3af;
}

@media (prefers-color-scheme: dark) {
  .chat-input {
    background: rgba(17, 20, 17, 0.78);
  }

  .input-container {
    background: rgba(29, 35, 31, 0.88);
  }
}
</style> 
