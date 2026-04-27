<template>
  <div class="app-shell">
    <div v-if="!isDesktop" class="mobile-switch">
      <button
        type="button"
        :class="{ active: activeMobileView === 'chat' }"
        @click="activeMobileView = 'chat'"
      >
        聊天
      </button>
      <button
        type="button"
        :class="{ active: activeMobileView === 'hot100' }"
        @click="activeMobileView = 'hot100'"
      >
        Hot100
      </button>
    </div>

    <div class="workspace">
      <section v-show="isDesktop || activeMobileView === 'chat'" class="chat-panel">
        <header class="panel-header">
          <div class="title-wrap">
            <h1>AI 学习与求职辅导助手</h1>
            <p>多 Persona 对话：学习规划、面试模拟、简历优化</p>
            <div class="auth-box">
              <template v-if="currentUser">
                <div class="auth-user">
                  <strong>{{ currentUser.username }}</strong>
                  <span>{{ currentUser.email }}</span>
                </div>
                <button type="button" class="auth-btn ghost" @click="logout">退出登录</button>
              </template>
              <template v-else>
                <button type="button" class="auth-btn" @click="openAuthModal('login')">登录</button>
                <button type="button" class="auth-btn ghost" @click="openAuthModal('register')">注册</button>
              </template>
            </div>
            <div v-if="activeProblem" class="active-problem">
              <span>当前辅导题目：{{ activeProblem.title }}（{{ activeProblem.slug }}）</span>
              <button @click="clearProblemCoaching" type="button">清除</button>
            </div>
          </div>
          <div class="role-picker">
            <label for="roleSelect">辅导模式</label>
            <select id="roleSelect" v-model="selectedRoleId">
              <option v-for="role in roles" :key="role.id" :value="role.id">
                {{ formatRoleOption(role) }}
              </option>
            </select>
            <label for="solvingModeSelect">解题模式</label>
            <select id="solvingModeSelect" v-model="solvingMode">
              <option value="guided">引导模式</option>
              <option value="direct">直解模式</option>
              <option value="code_review">代码审查模式</option>
            </select>
            <p class="role-hint">二次元人设已保留为扩展原型，不作为首页主展示。</p>
          </div>
        </header>

        <main class="messages-area" ref="messagesContainer">
          <div v-if="messages.length === 0" class="welcome-card">
            <h2>{{ currentRole.name }}</h2>
            <p>{{ currentRole.description }}</p>
            <div class="capability-list">
              <span>SSE 流式对话</span>
              <span>Persona 角色系统</span>
              <span>RAG 检索增强</span>
              <span>工具调用</span>
              <span>Hot100 题目辅导</span>
            </div>
            <p class="tip">先在右侧选择题目并点击“开始辅导”，再在这里提问。</p>
          </div>

          <ChatMessage
            v-for="message in messages"
            :key="message.id"
            :message="message.content"
            :is-user="message.isUser"
            :timestamp="message.timestamp"
            :ai-role="getRoleById(message.roleId)"
          />

          <div v-if="isAiTyping" class="chat-message ai-message">
            <div class="message-avatar">
              <img class="typing-avatar" :src="currentRole.avatar" :alt="currentRole.name" />
            </div>
            <div class="message-content">
              <div class="message-bubble">
                <div class="ai-response-text" v-html="currentAiResponseRendered"></div>
                <LoadingDots v-if="isStreaming" />
              </div>
            </div>
          </div>
        </main>

        <ChatInput
          :disabled="isAiTyping"
          @send-message="sendMessage"
          placeholder="输入你的问题，按 Enter 发送，Shift + Enter 换行"
        />
      </section>

      <aside v-show="isDesktop || activeMobileView === 'hot100'" class="hot100-panel">
        <div v-if="showMasteryPanel" class="mastery-overlay" @click.self="showMasteryPanel = false">
          <div class="mastery-dialog">
            <div class="mastery-dialog-header">
              <h3>标签掌握度</h3>
              <button type="button" @click="showMasteryPanel = false">关闭</button>
            </div>
            <div class="mastery-list">
              <div class="mastery-item" v-for="item in tagMastery" :key="item.tag">
                <div class="mastery-top">
                  <span>{{ item.tag }}</span>
                  <span>{{ Math.round(item.masteryRate * 100) }}%</span>
                </div>
                <div class="mastery-track">
                  <div class="mastery-bar" :style="{ width: `${Math.round(item.masteryRate * 100)}%` }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <header class="hot100-header">
          <div class="hot100-title-wrap">
            <button v-if="hot100View === 'detail'" @click="backToProblemList" class="back-btn" type="button">返回</button>
            <h2>{{ hot100View === 'detail' ? '题目详情' : 'Hot100 题库' }}</h2>
          </div>
          <div class="dataset-stats" v-if="datasetStats">
            已录入 {{ datasetStats.loadedCount }}/{{ datasetStats.targetCount }}
          </div>
          <button @click="refreshHot100Panel" :disabled="isHot100Loading" type="button">
            {{ isHot100Loading ? '加载中...' : '刷新' }}
          </button>
        </header>

        <div class="filter-section" v-if="hot100View === 'list'">
          <div class="search-line">
            <input
              v-model="hot100Keyword"
              @keyup.enter="searchHot100Problems"
              placeholder="按题名或关键词搜索"
              class="filter-input"
            />
            <button @click="searchHot100Problems" class="search-btn" type="button">搜索</button>
          </div>
          <div class="filter-row">
            <select v-model="hot100Tag" class="filter-select">
              <option value="">全部标签</option>
              <option v-for="tag in hot100Tags" :key="tag" :value="tag">{{ tag }}</option>
            </select>
            <select v-model="hot100Difficulty" class="filter-select">
              <option value="">全部难度</option>
              <option value="easy">easy</option>
              <option value="medium">medium</option>
              <option value="hard">hard</option>
            </select>
          </div>
          <button @click="clearFilters" class="clear-btn" type="button">清空筛选</button>
          <div v-if="!currentUser" class="auth-tip">
            登录后可保存做题进度、查看标签掌握度和推荐题单
            <button type="button" @click="openAuthModal('login')">立即登录</button>
          </div>
          <div class="insight-box" v-if="weakTags.length > 0">
            <strong>薄弱标签：</strong>
            <span v-for="item in weakTags.slice(0, 4)" :key="item.tag">{{ item.tag }}({{ item.wrongCount }})</span>
          </div>
          <div class="insight-box" v-if="recommendations.length > 0">
            <strong>推荐下一题：</strong>
            <span v-for="item in recommendations.slice(0, 3)" :key="item.slug">{{ item.title }}</span>
          </div>
          <button
            v-if="tagMastery.length > 0"
            type="button"
            class="mastery-entry-btn"
            @click="showMasteryPanel = true"
          >
            查看标签掌握度
          </button>
        </div>

        <div class="hot100-body" v-if="hot100View === 'list'">
          <div class="problem-list list-only">
            <div
              v-for="problem in hot100Problems"
              :key="problem.slug"
              class="problem-item"
              :class="{ active: selectedProblem?.slug === problem.slug }"
              @click="selectProblem(problem.slug)"
            >
              <div class="problem-top">
                <span class="problem-title">{{ problem.title }}</span>
                <div class="problem-meta">
                  <span class="difficulty">{{ problem.difficulty }}</span>
                  <span
                    class="status-badge"
                    :class="statusClass(progressMap[problem.slug]?.status || 'NOT_STARTED')"
                  >
                    {{ statusText(progressMap[problem.slug]?.status || 'NOT_STARTED') }}
                  </span>
                </div>
              </div>
              <div class="problem-tags">
                <span v-for="tag in problem.tags" :key="`${problem.slug}-${tag}`">{{ tag }}</span>
              </div>
            </div>
            <div v-if="!isHot100Loading && hot100Problems.length === 0" class="empty-list">
              没有匹配题目
            </div>
          </div>
        </div>

        <div class="hot100-body" v-else>
          <div class="problem-detail detail-only" v-if="selectedProblem">
            <h3>{{ selectedProblem.title }}</h3>
            <p class="meta">slug: {{ selectedProblem.slug }} | 难度: {{ selectedProblem.difficulty }}</p>
            <p v-if="selectedProblem.leetCodeUrl">
              <strong>LeetCode：</strong>
              <a :href="selectedProblem.leetCodeUrl" target="_blank" rel="noopener noreferrer">{{ selectedProblem.leetCodeUrl }}</a>
            </p>
            <p><strong>模式：</strong>{{ selectedProblem.pattern }}</p>
            <p><strong>核心思路：</strong>{{ selectedProblem.coreIdea }}</p>
            <p><strong>常见错误：</strong>{{ selectedProblem.pitfalls }}</p>
            <p><strong>复杂度：</strong>{{ selectedProblem.complexity }}</p>
            <div class="progress-editor">
              <div class="saved-record">
                <div>当前状态：{{ statusText(currentProgressRecord?.status || 'NOT_STARTED') }}</div>
                <div>最近更新：{{ formatDateTime(currentProgressRecord?.updatedAt || currentProgressRecord?.lastReviewedAt) }}</div>
              </div>
              <div class="progress-row">
                <label>学习状态</label>
                <select v-model="progressForm.status" class="filter-select">
                  <option value="NOT_STARTED">未做</option>
                  <option value="COMPLETED">已做</option>
                  <option value="WRONG">做错</option>
                  <option value="MASTERED">已掌握</option>
                </select>
              </div>
              <div class="progress-row">
                <label>学习备注</label>
                <textarea v-model="progressForm.notes" placeholder="记录关键错误点或复盘结论"></textarea>
              </div>
              <button @click="saveProgress" class="save-btn" type="button" :disabled="isSavingProgress">
                {{ isSavingProgress ? '保存中...' : '保存进度' }}
              </button>
              <p v-if="progressSaveMessage" class="save-feedback" :class="progressSaveState">
                {{ progressSaveMessage }}
              </p>
            </div>
            <button @click="startProblemCoaching" class="coach-btn" type="button">开始辅导</button>
          </div>
          <div v-else class="problem-detail empty-detail">
            选择题目查看详情
          </div>
        </div>
      </aside>
    </div>

    <div v-if="showAuthModal" class="auth-modal-mask" @click.self="closeAuthModal">
      <div class="auth-modal">
        <div class="auth-modal-header">
          <h3>{{ authMode === 'login' ? '欢迎登录' : '创建账号' }}</h3>
          <button type="button" class="auth-close" @click="closeAuthModal">关闭</button>
        </div>
        <div class="auth-switch">
          <button
            type="button"
            :class="{ active: authMode === 'login' }"
            @click="switchAuthMode('login')"
          >
            登录
          </button>
          <button
            type="button"
            :class="{ active: authMode === 'register' }"
            @click="switchAuthMode('register')"
          >
            注册
          </button>
        </div>
        <div class="auth-form">
          <input v-model.trim="authForm.username" placeholder="用户名" />
          <input v-if="authMode === 'register'" v-model.trim="authForm.email" placeholder="邮箱" />
          <input v-model="authForm.password" type="password" placeholder="密码" />
          <button type="button" class="auth-submit" :disabled="authLoading" @click="submitAuth">
            {{ authLoading ? '提交中...' : authMode === 'login' ? '立即登录' : '立即注册' }}
          </button>
          <p v-if="authMessage" class="auth-message" :class="authMessageType">{{ authMessage }}</p>
        </div>
      </div>
    </div>

    <div v-if="connectionError" class="error-toast">连接后端失败，请检查后端是否启动</div>
  </div>
</template>

<script>
import { marked } from 'marked'
import ChatMessage from './components/ChatMessage.vue'
import ChatInput from './components/ChatInput.vue'
import LoadingDots from './components/LoadingDots.vue'
import { chatWithSSE, fetchRoles } from './api/chatApi.js'
import { fetchMe, loginAuth, logoutAuth, registerAuth } from './api/authApi.js'
import {
  clearStoredAuthTokens,
  getStoredAccessToken,
  setAuthHeaderToken,
  setStoredAccessToken
} from './api/httpClient.js'
import {
  fetchHot100DatasetStats,
  fetchHot100ProblemDetail,
  fetchHot100Problems,
  fetchHot100ProgressList,
  fetchHot100Recommendations,
  fetchHot100TagMastery,
  fetchHot100Tags,
  fetchHot100WeakTags,
  upsertHot100Progress
} from './api/hot100Api.js'
import { fetchErrorCodeDictionary } from './api/metaApi.js'
import { generateMemoryId } from './utils/index.js'

export default {
  name: 'App',
  components: { ChatMessage, ChatInput, LoadingDots },
  data() {
    const fallbackRole = {
      id: 'assistant',
      name: '编程学习助手',
      category: 'professional',
      tagline: '结构化学习路径、方案设计、知识查漏补缺',
      description: '默认模式：聚焦编程学习规划与项目实践建议。',
      avatar: '/characters/sakurajima-mai.png',
      avatarFallback: '/characters/sakurajima-mai.png',
      image: '/characters/sakurajima-mai.png',
      imageFallback: '/characters/sakurajima-mai.png',
      sortOrder: 1
    }
    return {
      roles: [fallbackRole],
      fallbackRole,
      selectedRoleId: 'assistant',
      solvingMode: 'guided',
      hot100View: 'list',
      messages: [],
      memoryId: null,
      isAiTyping: false,
      isStreaming: false,
      currentProblemSlug: null,
      activeMobileView: 'chat',
      windowWidth: typeof window !== 'undefined' ? window.innerWidth : 1366,
      currentAiResponse: '',
      currentEventSource: null,
      connectionError: false,
      isHot100Loading: false,
      hot100Keyword: '',
      hot100Tag: '',
      hot100Difficulty: '',
      hot100Tags: [],
      hot100Problems: [],
      selectedProblem: null,
      progressMap: {},
      weakTags: [],
      recommendations: [],
      tagMastery: [],
      datasetStats: null,
      showMasteryPanel: false,
      progressForm: {
        status: 'NOT_STARTED',
        notes: ''
      },
      isSavingProgress: false,
      progressSaveState: '',
      progressSaveMessage: '',
      showAuthModal: false,
      authMode: 'login',
      authLoading: false,
      authMessage: '',
      authMessageType: '',
      authForm: {
        username: '',
        email: '',
        password: ''
      },
      errorCodeDictionaryByCode: {},
      currentUser: null
    }
  },
  computed: {
    currentRole() {
      return this.getRoleById(this.selectedRoleId)
    },
    currentAiResponseRendered() {
      if (!this.currentAiResponse) return ''
      marked.setOptions({ breaks: true, gfm: true })
      return marked(this.currentAiResponse)
    },
    activeProblem() {
      if (!this.currentProblemSlug) return null
      return this.hot100Problems.find((item) => item.slug === this.currentProblemSlug) || this.selectedProblem
    },
    isDesktop() {
      return this.windowWidth > 1180
    },
    currentProgressRecord() {
      if (!this.selectedProblem?.slug) return null
      return this.progressMap[this.selectedProblem.slug] || null
    }
  },
  methods: {
    handleResize() {
      this.windowWidth = window.innerWidth
      if (this.isDesktop) {
        this.activeMobileView = 'chat'
      }
    },
    formatRoleOption(role) {
      if (role.category === 'extended') {
        return `扩展 · ${role.name}`
      }
      return role.name
    },
    getRoleById(roleId) {
      return this.roles.find((role) => role.id === roleId) || this.roles[0] || this.fallbackRole
    },
    openAuthModal(mode = 'login') {
      this.authMode = mode
      this.authMessage = ''
      this.authMessageType = ''
      this.showAuthModal = true
    },
    closeAuthModal() {
      this.showAuthModal = false
      this.authLoading = false
      this.authMessage = ''
      this.authMessageType = ''
    },
    switchAuthMode(mode) {
      this.authMode = mode
      this.authMessage = ''
      this.authMessageType = ''
    },
    resolveApiErrorMessage(error, fallbackMessage) {
      const responseMessage = error?.response?.data?.message
      if (responseMessage) return responseMessage
      const businessCode = error?.response?.data?.code ?? error?.code
      if (businessCode !== undefined && businessCode !== null) {
        const dict = this.errorCodeDictionaryByCode[businessCode]
        if (dict?.message) {
          return dict.message
        }
      }
      return error?.message || fallbackMessage
    },
    async loadErrorCodeDictionary() {
      try {
        const dictionary = await fetchErrorCodeDictionary()
        this.errorCodeDictionaryByCode = {}
        for (const item of dictionary) {
          if (item && item.code !== undefined && item.code !== null) {
            this.errorCodeDictionaryByCode[item.code] = item
          }
        }
      } catch (error) {
        console.error('Failed to load error code dictionary:', error)
      }
    },
    async submitAuth() {
      this.authLoading = true
      this.authMessage = ''
      this.authMessageType = ''
      try {
        const payload = {
          username: this.authForm.username,
          password: this.authForm.password
        }
        let data
        if (this.authMode === 'register') {
          data = await registerAuth({
            ...payload,
            email: this.authForm.email
          })
        } else {
          data = await loginAuth(payload)
        }
        const token = data?.accessToken || ''
        const user = data?.user || null
        if (!token || !user) {
          throw new Error('登录返回数据不完整')
        }
        setStoredAccessToken(token)
        setAuthHeaderToken(token)
        this.currentUser = user
        this.authMessage = this.authMode === 'register' ? '注册成功，已自动登录' : '登录成功'
        this.authMessageType = 'success'
        this.authForm.password = ''
        await this.loadLearningInsights()
        setTimeout(() => this.closeAuthModal(), 400)
      } catch (error) {
        this.authMessage = error?.response?.data?.message || error?.message || '认证失败'
        this.authMessageType = 'error'
      } finally {
        this.authLoading = false
      }
    },
    async restoreAuth() {
      const token = getStoredAccessToken()
      if (!token) {
        setAuthHeaderToken('')
        this.currentUser = null
        return
      }
      try {
        setAuthHeaderToken(token)
        this.currentUser = await fetchMe()
      } catch (error) {
        clearStoredAuthTokens()
        setAuthHeaderToken('')
        this.currentUser = null
      }
    },
    async logout() {
      try {
        await logoutAuth()
      } catch (error) {
        console.error('Logout request failed:', error)
      }
      clearStoredAuthTokens()
      setAuthHeaderToken('')
      this.currentUser = null
      this.progressMap = {}
      this.weakTags = []
      this.recommendations = []
      this.tagMastery = []
      this.datasetStats = null
      if (this.selectedProblem?.slug) {
        this.syncProgressFormBySlug(this.selectedProblem.slug)
      }
    },
    sendMessage(message) {
      this.addMessage(message, true, null)
      this.startAiResponse(message)
    },
    addMessage(content, isUser = false, roleId = null) {
      this.messages.push({
        id: Date.now() + Math.random(),
        content,
        isUser,
        roleId,
        timestamp: new Date()
      })
      this.scrollToBottom()
    },
    startAiResponse(userMessage) {
      this.isAiTyping = true
      this.isStreaming = true
      this.currentAiResponse = ''
      this.connectionError = false

      if (this.currentEventSource) {
        this.currentEventSource.close()
      }

      this.currentEventSource = chatWithSSE(
        this.memoryId,
        userMessage,
        this.selectedRoleId,
        this.handleAiMessage,
        this.handleAiError,
        this.handleAiClose,
        this.currentProblemSlug,
        this.solvingMode
      )
    },
    handleAiMessage(data) {
      this.currentAiResponse += data
      this.scrollToBottom()
    },
    handleAiError(error) {
      console.error('AI response error:', error)
      this.connectionError = true
      this.finishAiResponse()
      setTimeout(() => {
        this.connectionError = false
      }, 4000)
    },
    handleAiClose() {
      this.finishAiResponse()
    },
    finishAiResponse() {
      this.isStreaming = false
      if (this.currentAiResponse.trim()) {
        this.addMessage(this.currentAiResponse.trim(), false, this.selectedRoleId)
      }
      this.isAiTyping = false
      this.currentAiResponse = ''
      if (this.currentEventSource) {
        this.currentEventSource.close()
        this.currentEventSource = null
      }
    },
    startProblemCoaching() {
      if (!this.selectedProblem) return
      this.currentProblemSlug = this.selectedProblem.slug
      this.addMessage(
        `已进入 Hot100 题目辅导：${this.selectedProblem.title}（${this.selectedProblem.slug}）。请继续提问。`,
        false,
        this.selectedRoleId
      )
    },
    clearProblemCoaching() {
      this.currentProblemSlug = null
      this.addMessage('已退出题目定向辅导模式。', false, this.selectedRoleId)
    },
    clearFilters() {
      this.hot100Keyword = ''
      this.hot100Tag = ''
      this.hot100Difficulty = ''
      this.searchHot100Problems()
    },
    async selectProblem(slug) {
      try {
        this.selectedProblem = await fetchHot100ProblemDetail(slug)
        this.syncProgressFormBySlug(slug)
        this.progressSaveState = ''
        this.progressSaveMessage = ''
        this.hot100View = 'detail'
      } catch (error) {
        console.error('Failed to load problem detail:', error)
      }
    },
    backToProblemList() {
      this.hot100View = 'list'
    },
    syncProgressFormBySlug(slug) {
      const progress = this.progressMap[slug]
      this.progressForm.status = progress?.status || 'NOT_STARTED'
      this.progressForm.notes = progress?.notes || ''
    },
    statusText(status) {
      switch ((status || '').toUpperCase()) {
        case 'COMPLETED':
          return '已做'
        case 'WRONG':
          return '做错'
        case 'MASTERED':
          return '已掌握'
        default:
          return '未做'
      }
    },
    statusClass(status) {
      switch ((status || '').toUpperCase()) {
        case 'COMPLETED':
          return 'is-completed'
        case 'WRONG':
          return 'is-wrong'
        case 'MASTERED':
          return 'is-mastered'
        default:
          return 'is-not-started'
      }
    },
    formatDateTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value)
      return date.toLocaleString()
    },
    async saveProgress() {
      if (!this.selectedProblem) return
      if (!this.currentUser) {
        this.progressSaveState = 'error'
        this.progressSaveMessage = '请先登录后再保存进度'
        this.openAuthModal('login')
        return
      }
      this.isSavingProgress = true
      this.progressSaveState = ''
      this.progressSaveMessage = ''
      try {
        const saved = await upsertHot100Progress({
          problemSlug: this.selectedProblem.slug,
          status: this.progressForm.status,
          notes: this.progressForm.notes
        })
        if (saved?.problemSlug) {
          this.progressMap[saved.problemSlug] = saved
          this.syncProgressFormBySlug(saved.problemSlug)
        }
        this.progressSaveState = 'success'
        this.progressSaveMessage = `进度已保存：${this.statusText(this.progressForm.status)}`
        this.loadLearningInsights().catch((error) => {
          console.error('Failed to refresh learning insights after save:', error)
        })
      } catch (error) {
        console.error('Failed to save progress:', error)
        this.progressSaveState = 'error'
        this.progressSaveMessage = error?.response?.data?.message || error?.message || '保存失败，请检查后端日志'
      } finally {
        this.isSavingProgress = false
      }
    },
    async loadLearningInsights() {
      if (!this.currentUser) {
        this.progressMap = {}
        this.weakTags = []
        this.recommendations = []
        this.tagMastery = []
        this.datasetStats = null
        if (this.selectedProblem?.slug) {
          this.syncProgressFormBySlug(this.selectedProblem.slug)
        }
        return
      }
      try {
        const progressList = await fetchHot100ProgressList()
        this.progressMap = {}
        for (const item of progressList) {
          this.progressMap[item.problemSlug] = item
        }

        const [weakTagsResult, recommendationsResult, tagMasteryResult, datasetStatsResult] = await Promise.allSettled([
          fetchHot100WeakTags(),
          fetchHot100Recommendations(5),
          fetchHot100TagMastery(),
          fetchHot100DatasetStats()
        ])

        this.weakTags = weakTagsResult.status === 'fulfilled' ? weakTagsResult.value : []
        this.recommendations = recommendationsResult.status === 'fulfilled' ? recommendationsResult.value : []
        this.tagMastery = tagMasteryResult.status === 'fulfilled' ? tagMasteryResult.value : []
        this.datasetStats = datasetStatsResult.status === 'fulfilled' ? datasetStatsResult.value : null

        if (this.selectedProblem?.slug) {
          this.syncProgressFormBySlug(this.selectedProblem.slug)
        }
      } catch (error) {
        console.error('Failed to load learning insights:', error)
      }
    },
    async searchHot100Problems() {
      this.isHot100Loading = true
      try {
        this.hot100Problems = await fetchHot100Problems({
          keyword: this.hot100Keyword || undefined,
          tag: this.hot100Tag || undefined,
          difficulty: this.hot100Difficulty || undefined
        })
        this.hot100View = 'list'
        const exists = this.selectedProblem && this.hot100Problems.some((item) => item.slug === this.selectedProblem.slug)
        if (!exists) {
          this.selectedProblem = null
        }
      } catch (error) {
        console.error('Failed to search Hot100 problems:', error)
      } finally {
        this.isHot100Loading = false
      }
    },
    async refreshHot100Panel() {
      this.hot100Keyword = ''
      this.hot100Tag = ''
      this.hot100Difficulty = ''
      this.showMasteryPanel = false
      this.hot100View = 'list'
      this.selectedProblem = null
      this.hot100Problems = []
      await this.loadHot100Data({ autoSelectFirst: false })
    },
    async loadHot100Data(options = {}) {
      const { autoSelectFirst = false } = options
      this.isHot100Loading = true
      try {
        const [tags, problems] = await Promise.all([
          fetchHot100Tags(),
          fetchHot100Problems()
        ])
        this.hot100Tags = tags
        this.hot100Problems = problems
        if (autoSelectFirst && this.hot100Problems.length > 0) {
          await this.selectProblem(this.hot100Problems[0].slug)
        } else {
          this.selectedProblem = null
          this.hot100View = 'list'
        }
        await this.loadLearningInsights()
      } catch (error) {
        console.error('Failed to load Hot100 data:', error)
      } finally {
        this.isHot100Loading = false
      }
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },
    initializeChat() {
      this.memoryId = generateMemoryId()
    },
    async loadRoles() {
      try {
        const roleList = await fetchRoles()
        if (Array.isArray(roleList) && roleList.length > 0) {
          this.roles = roleList
          if (!this.roles.some((role) => role.id === this.selectedRoleId)) {
            this.selectedRoleId = this.roles[0].id
          }
        }
      } catch (error) {
        console.error('Failed to load roles from backend, fallback to local roles.', error)
      }
    }
  },
  async mounted() {
    this.initializeChat()
    window.addEventListener('resize', this.handleResize)
    await this.restoreAuth()
    await Promise.all([this.loadRoles(), this.loadHot100Data(), this.loadErrorCodeDictionary()])
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    if (this.currentEventSource) this.currentEventSource.close()
  }
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f8fafc;
  color: #0f172a;
  padding: 18px;
  box-sizing: border-box;
}

.mobile-switch {
  display: none;
}

.mobile-switch button {
  flex: 1;
  height: 36px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  cursor: pointer;
}

.mobile-switch button:first-child {
  border-radius: 10px 0 0 10px;
}

.mobile-switch button:last-child {
  border-radius: 0 10px 10px 0;
}

.mobile-switch button.active {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: #93c5fd;
}

.workspace {
  max-width: 1320px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.45fr 1fr;
  gap: 14px;
  height: calc(100vh - 36px);
}

.chat-panel {
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.panel-header {
  padding: 18px 18px 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.title-wrap h1 {
  margin: 0;
  font-size: 26px;
  color: #0f172a;
}

.title-wrap p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
}

.active-problem {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #0c4a6e;
}

.active-problem button {
  border: 1px solid #7dd3fc;
  background: #e0f2fe;
  color: #0c4a6e;
  border-radius: 8px;
  padding: 2px 8px;
  cursor: pointer;
}

.role-picker {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 210px;
}

.role-picker label {
  font-size: 12px;
  color: #64748b;
}

.role-hint {
  margin: 0;
  font-size: 11px;
  color: #94a3b8;
}

.auth-box {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.auth-user {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
  padding: 5px 10px;
  border: 1px solid #dbeafe;
  background: #eff6ff;
  border-radius: 999px;
}

.auth-user strong {
  font-size: 12px;
  color: #1e3a8a;
}

.auth-user span {
  font-size: 11px;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 240px;
}

.auth-btn {
  border: 1px solid #2563eb;
  background: #2563eb;
  color: #fff;
  border-radius: 8px;
  height: 30px;
  padding: 0 10px;
  cursor: pointer;
  font-size: 12px;
}

.auth-btn.ghost {
  border-color: #cbd5e1;
  background: #fff;
  color: #334155;
}

.role-picker select {
  height: 36px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #0f172a;
  padding: 0 10px;
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 14px 0;
}

.welcome-card {
  margin: 12px 20px;
  padding: 16px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.welcome-card h2 {
  margin: 0;
  font-size: 20px;
}

.welcome-card p {
  margin: 8px 0 0;
  color: #475569;
}

.capability-list {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.capability-list span {
  font-size: 12px;
  color: #1e3a8a;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  padding: 4px 10px;
}

.tip {
  font-size: 13px;
  color: #2563eb;
}

.chat-message {
  display: flex;
  margin-bottom: 20px;
  padding: 0 20px;
}

.message-avatar {
  margin-right: 10px;
}

.typing-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.45);
}

.message-content {
  max-width: 72%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  color: #334155;
  background: #f1f5f9;
}

.hot100-panel {
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 20px;
  position: relative;
  min-height: 0;
  display: flex;
  flex-direction: column;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.hot100-header {
  padding: 16px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hot100-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hot100-header h2 {
  margin: 0;
  font-size: 18px;
}

.back-btn {
  height: 30px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;
  padding: 0 10px;
  cursor: pointer;
}

.dataset-stats {
  margin-left: auto;
  margin-right: 8px;
  font-size: 12px;
  color: #475569;
}

.hot100-header button {
  border: 1px solid #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 10px;
  height: 32px;
  padding: 0 12px;
  cursor: pointer;
}

.filter-section {
  padding: 12px 14px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #fcfdff;
}

.insight-box {
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  padding: 8px;
  font-size: 12px;
  color: #334155;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.insight-box strong {
  color: #0f172a;
}

.auth-tip {
  border: 1px dashed #bfdbfe;
  border-radius: 10px;
  background: #f0f9ff;
  color: #1e3a8a;
  font-size: 12px;
  padding: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.auth-tip button {
  border: 1px solid #93c5fd;
  background: #fff;
  color: #1d4ed8;
  border-radius: 8px;
  height: 28px;
  padding: 0 10px;
  cursor: pointer;
}

.mastery-entry-btn {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  border-radius: 10px;
  height: 34px;
  cursor: pointer;
}

.mastery-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.28);
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 14px;
}

.mastery-dialog {
  width: 100%;
  max-width: 520px;
  max-height: 80%;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.mastery-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #e2e8f0;
}

.mastery-dialog-header h3 {
  margin: 0;
  font-size: 16px;
}

.mastery-dialog-header button {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  border-radius: 8px;
  height: 30px;
  padding: 0 10px;
  cursor: pointer;
}

.mastery-list {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px;
  background: #fff;
  overflow-y: auto;
}

.mastery-item + .mastery-item {
  margin-top: 8px;
}

.mastery-top {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #334155;
}

.mastery-track {
  margin-top: 4px;
  height: 8px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}

.mastery-bar {
  height: 100%;
  background: linear-gradient(90deg, #38bdf8, #22c55e);
}

.filter-input,
.filter-select {
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  height: 36px;
  padding: 0 10px;
  font-size: 13px;
}

.search-line {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.filter-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.search-btn {
  border: 1px solid #3b82f6;
  background: #3b82f6;
  color: #ffffff;
  border-radius: 10px;
  height: 36px;
  min-width: 72px;
  cursor: pointer;
}

.clear-btn {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #475569;
  border-radius: 10px;
  height: 34px;
  cursor: pointer;
}

.hot100-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.problem-list {
  overflow-y: auto;
}

.list-only {
  flex: 1;
}

.problem-item {
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f1f5f9;
}

.problem-item:hover {
  background: #f8fafc;
}

.problem-item.active {
  background: #eff6ff;
}

.problem-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.problem-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.problem-title {
  font-size: 13px;
  font-weight: 600;
}

.difficulty {
  text-transform: uppercase;
  font-size: 11px;
  color: #334155;
}

.status-badge {
  border-radius: 999px;
  padding: 1px 8px;
  font-size: 11px;
  line-height: 18px;
}

.status-badge.is-not-started {
  border: 1px solid #cbd5e1;
  color: #475569;
  background: #f8fafc;
}

.status-badge.is-completed {
  border: 1px solid #93c5fd;
  color: #1d4ed8;
  background: #eff6ff;
}

.status-badge.is-wrong {
  border: 1px solid #fca5a5;
  color: #b91c1c;
  background: #fef2f2;
}

.status-badge.is-mastered {
  border: 1px solid #86efac;
  color: #166534;
  background: #f0fdf4;
}

.problem-tags {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.problem-tags span {
  font-size: 11px;
  background: #f1f5f9;
  color: #334155;
  border-radius: 999px;
  padding: 2px 8px;
}

.empty-list {
  padding: 16px;
  color: #64748b;
  font-size: 13px;
}

.problem-detail {
  padding: 12px 14px 16px;
  overflow-y: auto;
}

.detail-only {
  flex: 1;
}

.problem-detail h3 {
  margin: 0;
  font-size: 18px;
}

.meta {
  margin: 6px 0 10px;
  color: #64748b;
  font-size: 12px;
}

.problem-detail p {
  margin: 6px 0;
  font-size: 13px;
  line-height: 1.5;
}

.problem-detail a {
  color: #2563eb;
  text-decoration: underline;
}

.progress-editor {
  margin-top: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fbfdff;
  padding: 10px;
}

.saved-record {
  margin-bottom: 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  padding: 8px;
  font-size: 12px;
  color: #334155;
  background: #ffffff;
}

.progress-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 8px;
}

.progress-row label {
  font-size: 12px;
  color: #475569;
}

.progress-row textarea {
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  min-height: 64px;
  padding: 8px;
  font-size: 13px;
  resize: vertical;
}

.save-btn {
  width: 100%;
  border: 1px solid #2563eb;
  background: #2563eb;
  color: #fff;
  border-radius: 10px;
  height: 34px;
  cursor: pointer;
}

.save-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.save-feedback {
  margin: 8px 0 0;
  font-size: 12px;
}

.save-feedback.success {
  color: #15803d;
}

.save-feedback.error {
  color: #dc2626;
}

.coach-btn {
  margin-top: 10px;
  width: 100%;
  border: none;
  background: linear-gradient(130deg, #0ea5e9, #22c55e);
  color: #ffffff;
  height: 36px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
}

.empty-detail {
  color: #64748b;
}

.error-toast {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 9;
  color: #fff;
  background: #dc2626;
  border-radius: 10px;
  padding: 10px 14px;
  box-shadow: 0 8px 24px rgba(220, 38, 38, 0.35);
}

.auth-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  background: rgba(15, 23, 42, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 14px;
}

.auth-modal {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.25);
  overflow: hidden;
}

.auth-modal-header {
  padding: 14px 14px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e2e8f0;
}

.auth-modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.auth-close {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;
  border-radius: 8px;
  height: 28px;
  padding: 0 10px;
  cursor: pointer;
}

.auth-switch {
  padding: 10px 14px 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.auth-switch button {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #475569;
  border-radius: 10px;
  height: 34px;
  cursor: pointer;
}

.auth-switch button.active {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
}

.auth-form {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.auth-form input {
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  height: 38px;
  padding: 0 12px;
  font-size: 14px;
}

.auth-submit {
  border: none;
  background: linear-gradient(130deg, #2563eb, #0ea5e9);
  color: #fff;
  border-radius: 10px;
  height: 38px;
  cursor: pointer;
  font-weight: 600;
}

.auth-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.auth-message {
  margin: 0;
  font-size: 12px;
}

.auth-message.success {
  color: #15803d;
}

.auth-message.error {
  color: #dc2626;
}

@media (max-width: 1180px) {
  .app-shell {
    padding: 10px;
    overflow: hidden;
  }

  .mobile-switch {
    display: flex;
    max-width: 1320px;
    margin: 0 auto 10px;
  }

  .workspace {
    display: block;
    height: calc(100vh - 66px);
  }

  .chat-panel,
  .hot100-panel {
    height: 100%;
  }
}

@media (max-width: 820px) {
  .panel-header {
    flex-direction: column;
  }

  .role-picker {
    min-width: 100%;
  }
}
</style>
