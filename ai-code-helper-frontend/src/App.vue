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
      <aside class="session-sidebar">
        <div class="brand-block">
          <div class="brand-mark">H</div>
          <div>
            <strong>AI Code Helper</strong>
            <span>AI Coding Coach</span>
          </div>
        </div>

        <button type="button" class="new-chat-btn" @click="newConversation">
          <span>+</span>
          新建对话
        </button>

        <div class="sidebar-section">
          <div class="sidebar-label">历史会话</div>
          <button
            v-for="item in sessionHistory"
            :key="item.id"
            type="button"
            class="session-item"
            :class="{ active: item.active }"
          >
            <span>{{ item.title }}</span>
            <small>{{ item.meta }}</small>
          </button>
        </div>

        <div class="sidebar-footer">
          <button type="button">设置</button>
          <a href="https://github.com" target="_blank" rel="noopener noreferrer">GitHub</a>
          <button type="button">关于项目</button>
        </div>
      </aside>

      <section v-show="isDesktop || activeMobileView === 'chat'" class="chat-panel">
        <header class="panel-header">
          <div class="chat-header-main">
            <div class="title-wrap">
              <div class="eyebrow">AI Coding Coach</div>
              <h1>AI Code Helper</h1>
              <p>Practice Hot100 problems with an AI coach.</p>
            </div>
            <div class="user-actions">
              <template v-if="currentUser">
                <div class="auth-user">
                  <strong>{{ currentUser.username }}</strong>
                  <span>{{ currentUser.email }}</span>
                </div>
                <button type="button" class="auth-btn ghost" @click="logout">Sign out</button>
              </template>
              <template v-else>
                <button type="button" class="auth-btn" @click="openAuthModal('login')">Sign in</button>
                <button type="button" class="auth-btn ghost" @click="openAuthModal('register')">Create account</button>
              </template>
            </div>
          </div>
          <div class="session-context-bar">
            <div v-if="activeProblem" class="active-problem">
              <strong>{{ activeProblem.title }}</strong>
              <button @click="clearProblemCoaching" type="button">Clear</button>
            </div>
            <div class="role-picker">
              <div class="select-field">
                <label for="roleSelect">Role</label>
                <select id="roleSelect" v-model="selectedRoleId">
                  <option v-for="role in roles" :key="role.id" :value="role.id">
                    {{ formatRoleOption(role) }}
                  </option>
                </select>
              </div>
              <div class="select-field">
                <label for="solvingModeSelect">Coaching Mode</label>
                <select id="solvingModeSelect" v-model="solvingMode">
                  <option value="guided">Guided Mode</option>
                  <option value="direct">Direct Mode</option>
                  <option value="code_review">Code Review</option>
                </select>
              </div>
            </div>
          </div>
        </header>

        <main class="messages-area" ref="messagesContainer">
          <div v-if="messages.length === 0" class="welcome-card">
            <h2>Ready to practice?</h2>
            <p>Pick a Hot100 problem or ask a coding question to get started.</p>
            <div class="capability-list">
              <span>SSE Streaming</span>
              <span>Persona</span>
              <span>RAG</span>
              <span>Tools</span>
              <span>Hot100 Coach</span>
            </div>
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
                <div v-if="isStreaming" class="streaming-line">
                  <LoadingDots />
                  <span class="typing-cursor"></span>
                </div>
              </div>
            </div>
          </div>
        </main>

        <ChatInput
          :disabled="isAiTyping"
          :loading="isAiTyping"
          @send-message="sendMessage"
          @stop-message="stopCurrentResponse"
          placeholder="Ask about algorithms, backend, or your selected Hot100 problem..."
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
            <button v-if="hot100View !== 'list'" @click="backToProblemList" class="back-btn" type="button">返回</button>
            <h2>{{ hot100Title }}</h2>
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
          <button
            v-if="currentUser"
            @click="openWrongBook"
            class="wrong-book-btn"
            type="button"
          >
            错题本（{{ wrongBook.length }}）
          </button>
          <div v-if="currentUser" class="study-plan-actions">
            <button @click="openStudyPlan(7)" class="study-plan-btn" type="button">7 天计划</button>
            <button @click="openStudyPlan(14)" class="study-plan-btn" type="button">14 天计划</button>
          </div>
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

        <div class="hot100-body" v-else-if="hot100View === 'wrongBook'">
          <div class="wrong-book-view">
            <div class="wrong-book-summary">
              <strong>错题本</strong>
              <span>共 {{ wrongBook.length }} 题，按最近标记为做错的记录排序。</span>
            </div>
            <div class="problem-list list-only">
              <div
                v-for="problem in wrongBook"
                :key="problem.slug"
                class="problem-item"
                :class="{ active: selectedProblem?.slug === problem.slug }"
                @click="selectProblem(problem.slug)"
              >
                <div class="problem-top">
                  <span class="problem-title">{{ problem.title }}</span>
                  <div class="problem-meta">
                    <span class="difficulty">{{ problem.difficulty }}</span>
                    <span class="status-badge is-wrong">做错</span>
                  </div>
                </div>
                <div class="problem-tags">
                  <span v-for="tag in problem.tags" :key="`wrong-${problem.slug}-${tag}`">{{ tag }}</span>
                </div>
              </div>
              <div v-if="!isHot100Loading && wrongBook.length === 0" class="empty-list">
                暂无错题，先在题目详情里把状态标记为“做错”。
              </div>
            </div>
          </div>
        </div>

        <div class="hot100-body" v-else-if="hot100View === 'studyPlan'">
          <div class="study-plan-view">
            <div class="study-plan-summary">
              <strong>{{ studyPlanDays }} 天学习计划</strong>
              <span>基于薄弱标签和推荐题单生成，每天安排一个主要训练目标。</span>
            </div>
            <div class="study-plan-list">
              <div class="study-plan-item" v-for="item in studyPlan" :key="`${item.day}-${item.problemSlug}`">
                <div class="study-plan-day">Day {{ item.day }}</div>
                <div class="study-plan-content">
                  <button
                    v-if="item.problemSlug && item.problemSlug !== '-'"
                    type="button"
                    class="study-plan-title"
                    @click="selectProblem(item.problemSlug)"
                  >
                    {{ item.title }}
                  </button>
                  <strong v-else class="study-plan-title-text">{{ item.title }}</strong>
                  <div class="study-plan-meta">
                    <span>{{ item.difficulty }}</span>
                    <span>{{ item.focus }}</span>
                  </div>
                </div>
              </div>
              <div v-if="!isHot100Loading && studyPlan.length === 0" class="empty-list">
                暂无学习计划，先保存几道题的学习进度后再生成。
              </div>
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
  fetchHot100StudyPlan,
  fetchHot100TagMastery,
  fetchHot100Tags,
  fetchHot100WeakTags,
  fetchHot100WrongBook,
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
      name: 'AI Code Helper',
      category: 'professional',
      tagline: '结构化学习路径、方案设计、知识查漏补缺',
      description: 'Focused coaching for algorithms, backend engineering, and project practice.',
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
      wrongBook: [],
      studyPlan: [],
      studyPlanDays: 7,
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
    },
    hot100Title() {
      if (this.hot100View === 'detail') return '题目详情'
      if (this.hot100View === 'wrongBook') return '错题本'
      if (this.hot100View === 'studyPlan') return '学习计划'
      return 'Hot100 题库'
    },
    sessionHistory() {
      const firstUserMessage = this.messages.find((message) => message.isUser)
      return [
        {
          id: 'current',
          title: firstUserMessage?.content?.slice(0, 22) || '当前对话',
          meta: this.messages.length > 0 ? `${this.messages.length} 条消息` : '等待开始',
          active: true
        },
        { id: 'placeholder-1', title: 'Hot100 双指针复盘', meta: '历史占位', active: false },
        { id: 'placeholder-2', title: '动态规划面试准备', meta: '历史占位', active: false }
      ]
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
        return `Extended · ${role.name}`
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
      this.wrongBook = []
      this.studyPlan = []
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
    newConversation() {
      if (this.currentEventSource) {
        this.currentEventSource.close()
        this.currentEventSource = null
      }
      this.messages = []
      this.currentAiResponse = ''
      this.isAiTyping = false
      this.isStreaming = false
      this.currentProblemSlug = null
      this.connectionError = false
      this.initializeChat()
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
    stopCurrentResponse() {
      if (!this.isAiTyping) return
      this.finishAiResponse()
    },
    startProblemCoaching() {
      if (!this.selectedProblem) return
      this.currentProblemSlug = this.selectedProblem.slug
      this.addMessage(
        `You are now practicing: ${this.selectedProblem.title} (${this.selectedProblem.slug}). Ask your next question.`,
        false,
        this.selectedRoleId
      )
    },
    clearProblemCoaching() {
      this.currentProblemSlug = null
      this.addMessage('Active problem cleared. You are back to general coaching.', false, this.selectedRoleId)
    },
    clearFilters() {
      this.hot100Keyword = ''
      this.hot100Tag = ''
      this.hot100Difficulty = ''
      this.searchHot100Problems()
    },
    async openWrongBook() {
      if (!this.currentUser) {
        this.openAuthModal('login')
        return
      }
      this.isHot100Loading = true
      try {
        this.wrongBook = await fetchHot100WrongBook()
        this.selectedProblem = null
        this.hot100View = 'wrongBook'
      } catch (error) {
        console.error('Failed to load wrong book:', error)
      } finally {
        this.isHot100Loading = false
      }
    },
    async openStudyPlan(days = 7) {
      if (!this.currentUser) {
        this.openAuthModal('login')
        return
      }
      this.isHot100Loading = true
      try {
        this.studyPlanDays = days <= 7 ? 7 : 14
        this.studyPlan = await fetchHot100StudyPlan(this.studyPlanDays)
        this.selectedProblem = null
        this.hot100View = 'studyPlan'
      } catch (error) {
        console.error('Failed to load study plan:', error)
      } finally {
        this.isHot100Loading = false
      }
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
        this.wrongBook = []
        this.studyPlan = []
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

        const [wrongBookResult, weakTagsResult, recommendationsResult, tagMasteryResult, datasetStatsResult] = await Promise.allSettled([
          fetchHot100WrongBook(),
          fetchHot100WeakTags(),
          fetchHot100Recommendations(5),
          fetchHot100TagMastery(),
          fetchHot100DatasetStats()
        ])

        this.wrongBook = wrongBookResult.status === 'fulfilled' ? wrongBookResult.value : []
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

.wrong-book-btn {
  border: 1px solid #fca5a5;
  background: #fef2f2;
  color: #b91c1c;
  border-radius: 10px;
  height: 34px;
  cursor: pointer;
}

.study-plan-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.study-plan-btn {
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  color: #166534;
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

.wrong-book-view {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.wrong-book-summary {
  margin: 10px 12px;
  border: 1px solid #fecaca;
  background: #fff7f7;
  border-radius: 10px;
  padding: 9px 10px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.wrong-book-summary strong {
  color: #991b1b;
  font-size: 13px;
}

.wrong-book-summary span {
  color: #64748b;
  font-size: 12px;
}

.study-plan-view {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.study-plan-summary {
  margin: 10px 12px;
  border: 1px solid #bbf7d0;
  background: #f7fef9;
  border-radius: 10px;
  padding: 9px 10px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.study-plan-summary strong {
  color: #166534;
  font-size: 13px;
}

.study-plan-summary span {
  color: #64748b;
  font-size: 12px;
}

.study-plan-list {
  overflow-y: auto;
}

.study-plan-item {
  display: grid;
  grid-template-columns: 66px 1fr;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px solid #f1f5f9;
}

.study-plan-day {
  align-self: start;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  height: 26px;
  line-height: 24px;
  text-align: center;
  font-size: 12px;
  color: #334155;
  background: #ffffff;
}

.study-plan-content {
  min-width: 0;
}

.study-plan-title,
.study-plan-title-text {
  display: inline-block;
  max-width: 100%;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  text-align: left;
}

.study-plan-title {
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
}

.study-plan-title:hover {
  color: #2563eb;
}

.study-plan-meta {
  margin-top: 5px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.study-plan-meta span {
  border-radius: 999px;
  background: #f1f5f9;
  color: #334155;
  font-size: 11px;
  padding: 2px 8px;
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

/* Modern assistant UI overrides */
.app-shell {
  --bg: #f6f7f8;
  --panel: #ffffff;
  --panel-soft: #f3f5f4;
  --panel-strong: #e8ecea;
  --text: #111816;
  --muted: #66736f;
  --line: rgba(17, 24, 22, 0.1);
  --accent: #10a37f;
  --accent-strong: #0f8f72;
  --accent-soft: rgba(16, 163, 127, 0.12);
  --shadow: 0 20px 60px rgba(15, 23, 42, 0.08);
  min-height: 100vh;
  padding: 16px;
  color: var(--text);
  background:
    radial-gradient(circle at 16% 10%, rgba(16, 163, 127, 0.08), transparent 26%),
    linear-gradient(180deg, #ffffff 0%, var(--bg) 44%, #eef1f0 100%);
  animation: pageFadeIn 360ms ease both;
}

.workspace {
  width: min(100%, 1480px);
  max-width: 1480px;
  height: calc(100vh - 32px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 390px;
  gap: 12px;
}

.session-sidebar,
.chat-panel,
.hot100-panel {
  background: color-mix(in srgb, var(--panel) 94%, transparent);
  border: 1px solid var(--line);
  border-radius: 20px;
  box-shadow: var(--shadow);
}

.session-sidebar {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 14px;
  background: #101513;
  color: #f5fbf8;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 14px;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #061210;
  background: linear-gradient(135deg, #7de2c7, #10a37f);
  font-weight: 800;
}

.brand-block strong,
.brand-block span,
.session-item span,
.session-item small {
  display: block;
}

.brand-block strong {
  font-size: 15px;
}

.brand-block span {
  margin-top: 2px;
  color: rgba(245, 251, 248, 0.62);
  font-size: 12px;
}

.new-chat-btn,
.session-item,
.sidebar-footer button,
.sidebar-footer a {
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: inherit;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 12px;
  cursor: pointer;
  transition: transform 160ms ease, background 160ms ease, box-shadow 160ms ease;
}

.new-chat-btn {
  width: 100%;
  height: 42px;
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
  font-weight: 700;
}

.new-chat-btn span {
  font-size: 20px;
  line-height: 1;
}

.new-chat-btn:hover,
.session-item:hover,
.sidebar-footer button:hover,
.sidebar-footer a:hover {
  background: rgba(255, 255, 255, 0.09);
  transform: translateY(-1px) scale(1.01);
}

.sidebar-section {
  flex: 1;
  min-height: 0;
  margin-top: 18px;
  overflow-y: auto;
}

.sidebar-label {
  margin: 0 4px 8px;
  color: rgba(245, 251, 248, 0.48);
  font-size: 12px;
}

.session-item {
  width: 100%;
  padding: 10px 11px;
  text-align: left;
  margin-bottom: 8px;
}

.session-item.active {
  background: rgba(16, 163, 127, 0.18);
  border-color: rgba(125, 226, 199, 0.28);
}

.session-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.session-item small {
  margin-top: 4px;
  color: rgba(245, 251, 248, 0.48);
  font-size: 11px;
}

.sidebar-footer {
  display: grid;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar-footer button,
.sidebar-footer a {
  height: 34px;
  padding: 0 10px;
  display: flex;
  align-items: center;
  text-decoration: none;
  font-size: 13px;
}

.chat-panel,
.hot100-panel {
  min-width: 0;
  overflow: hidden;
}

.panel-header {
  padding: 18px 22px;
  border-bottom: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(255, 255, 255, 0.48));
}

.eyebrow {
  margin-bottom: 5px;
  color: var(--accent-strong);
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.title-wrap h1 {
  color: var(--text);
  font-size: 24px;
  letter-spacing: 0;
}

.title-wrap p,
.role-picker label,
.role-hint {
  color: var(--muted);
}

.role-picker {
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-content: start;
  min-width: 360px;
  gap: 10px;
}

.select-field {
  display: grid;
  gap: 6px;
}

.role-picker select,
.filter-input,
.filter-select,
.progress-row textarea,
.auth-form input {
  border: 1px solid var(--line);
  background: var(--panel-soft);
  color: var(--text);
  outline: none;
  transition: border-color 160ms ease, box-shadow 160ms ease, background 160ms ease;
}

.role-picker select:focus,
.filter-input:focus,
.filter-select:focus,
.progress-row textarea:focus,
.auth-form input:focus {
  border-color: rgba(16, 163, 127, 0.65);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.messages-area {
  padding: 22px 0 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.42), rgba(246, 247, 248, 0.22));
}

.welcome-card,
.progress-editor,
.saved-record,
.wrong-book-summary,
.study-plan-summary,
.insight-box,
.auth-tip {
  background: var(--panel-soft);
  border-color: var(--line);
}

.welcome-card {
  max-width: 720px;
  margin: 36px auto;
  padding: 22px;
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
  animation: messageIn 260ms ease both;
}

.capability-list span,
.problem-tags span,
.study-plan-meta span,
.active-problem,
.auth-user {
  background: var(--accent-soft);
  border-color: rgba(16, 163, 127, 0.22);
  color: var(--accent-strong);
}

.auth-btn,
.search-btn,
.save-btn,
.auth-submit,
.coach-btn,
.hot100-header button {
  border: 0;
  background: var(--accent);
  color: #fff;
  box-shadow: 0 10px 22px rgba(16, 163, 127, 0.2);
  transition: transform 160ms ease, box-shadow 160ms ease, filter 160ms ease;
}

.auth-btn:hover,
.search-btn:hover,
.save-btn:hover,
.auth-submit:hover,
.coach-btn:hover,
.hot100-header button:hover {
  transform: translateY(-1px);
  filter: brightness(1.03);
  box-shadow: 0 14px 28px rgba(16, 163, 127, 0.24);
}

.auth-btn.ghost,
.back-btn,
.clear-btn,
.mastery-entry-btn,
.mastery-dialog-header button,
.auth-close,
.auth-switch button,
.active-problem button {
  background: var(--panel-soft);
  border: 1px solid var(--line);
  color: var(--text);
}

.hot100-header,
.filter-section {
  background: transparent;
  border-color: var(--line);
}

.problem-item {
  border-color: var(--line);
  transition: background 160ms ease, transform 160ms ease;
}

.problem-item:hover {
  background: var(--panel-soft);
  transform: translateX(2px);
}

.problem-item.active {
  background: var(--accent-soft);
}

.message-bubble {
  background: var(--panel-soft);
  color: var(--text);
  border: 1px solid var(--line);
}

.streaming-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.typing-cursor {
  width: 7px;
  height: 18px;
  border-radius: 999px;
  background: var(--accent);
  animation: cursorBlink 900ms steps(2, start) infinite;
}

.auth-modal,
.mastery-dialog {
  background: var(--panel);
  color: var(--text);
  border-color: var(--line);
}

button:disabled {
  opacity: 0.62;
  cursor: not-allowed;
  transform: none;
}

@keyframes pageFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
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

@keyframes cursorBlink {
  0%, 45% {
    opacity: 1;
  }
  46%, 100% {
    opacity: 0.15;
  }
}

@media (prefers-color-scheme: dark) {
  .app-shell {
    --bg: #0f1110;
    --panel: #171a18;
    --panel-soft: #202421;
    --panel-strong: #2b312d;
    --text: #edf3ef;
    --muted: #9aa7a2;
    --line: rgba(237, 243, 239, 0.11);
    --accent: #10a37f;
    --accent-strong: #7de2c7;
    --accent-soft: rgba(16, 163, 127, 0.16);
    --shadow: 0 20px 60px rgba(0, 0, 0, 0.32);
    background:
      radial-gradient(circle at 20% 8%, rgba(16, 163, 127, 0.12), transparent 30%),
      linear-gradient(180deg, #0b0d0c 0%, #111412 100%);
  }

  .session-sidebar {
    background: #0b0d0c;
  }

  .panel-header,
  .messages-area {
    background: transparent;
  }

  .title-wrap h1,
  .hot100-header h2,
  .problem-title,
  .problem-detail h3,
  .study-plan-title,
  .study-plan-title-text,
  .insight-box strong {
    color: var(--text);
  }

  .message-bubble,
  .ai-message .message-bubble {
    background: var(--panel-soft);
    color: var(--text);
    border-color: var(--line);
  }

  .difficulty,
  .meta,
  .problem-detail p,
  .empty-list,
  .message-time,
  .dataset-stats {
    color: var(--muted);
  }
}

@media (max-width: 1180px) {
  .workspace {
    display: block;
    height: calc(100vh - 66px);
  }

  .session-sidebar {
    display: none;
  }
}

@media (max-width: 820px) {
  .app-shell {
    padding: 8px;
  }

  .panel-header,
  .hot100-header {
    padding: 14px;
  }

  .role-picker {
    grid-template-columns: 1fr;
    min-width: 100%;
  }

  .welcome-card {
    margin: 16px 12px;
  }

  .hot100-header {
    flex-wrap: wrap;
    gap: 8px;
  }
}

/* Refined AI product visual system */
.app-shell {
  --bg: #f7f7f5;
  --surface: #ffffff;
  --surface-muted: #f3f4f2;
  --surface-hover: #ecefed;
  --text: #1f2421;
  --text-soft: #5f6863;
  --text-faint: #8a948f;
  --border: rgba(0, 0, 0, 0.08);
  --border-strong: rgba(0, 0, 0, 0.12);
  --accent: #0d8f72;
  --accent-soft: rgba(13, 143, 114, 0.09);
  --radius-md: 16px;
  --radius-lg: 20px;
  --shadow-soft: 0 1px 2px rgba(0, 0, 0, 0.04), 0 18px 40px rgba(0, 0, 0, 0.035);
  min-height: 100vh;
  padding: 14px;
  color: var(--text);
  background: var(--bg);
}

.workspace {
  width: min(100%, 1500px);
  max-width: 1500px;
  height: calc(100vh - 28px);
  display: grid;
  grid-template-columns: 252px minmax(620px, 1fr) 392px;
  gap: 10px;
}

.session-sidebar,
.chat-panel,
.hot100-panel {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: none;
}

.session-sidebar {
  padding: 12px;
  background: #111815;
  color: rgba(255, 255, 255, 0.92);
}

.brand-block {
  gap: 11px;
  padding: 6px 6px 14px;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.92);
}

.brand-block strong {
  font-size: 14px;
  font-weight: 700;
}

.brand-block span,
.sidebar-label,
.session-item small {
  color: rgba(255, 255, 255, 0.48);
}

.new-chat-btn,
.session-item,
.sidebar-footer button,
.sidebar-footer a {
  border: 0;
  background: transparent;
  border-radius: 12px;
  box-shadow: none;
}

.new-chat-btn {
  height: 40px;
  justify-content: flex-start;
  padding: 0 11px;
  background: rgba(255, 255, 255, 0.08);
}

.new-chat-btn:hover,
.session-item:hover,
.sidebar-footer button:hover,
.sidebar-footer a:hover {
  background: rgba(255, 255, 255, 0.09);
  transform: none;
}

.session-item {
  padding: 10px;
  margin-bottom: 4px;
}

.session-item.active {
  background: rgba(255, 255, 255, 0.1);
  border: 0;
}

.sidebar-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.chat-panel {
  background: var(--surface);
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: start;
  gap: 18px;
  padding: 18px 22px 14px;
  border-bottom: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.82);
}

.eyebrow {
  margin-bottom: 4px;
  color: var(--text-faint);
  font-size: 12px;
  font-weight: 650;
  letter-spacing: 0;
  text-transform: none;
}

.title-wrap h1 {
  color: var(--text);
  font-size: 26px;
  line-height: 1.2;
  font-weight: 720;
}

.title-wrap p {
  max-width: 620px;
  color: var(--text-soft);
  font-size: 13px;
}

.role-picker {
  min-width: 324px;
  grid-template-columns: 154px 154px;
  gap: 10px;
}

.select-field {
  gap: 5px;
}

.role-picker label {
  color: var(--text-faint);
  font-size: 12px;
}

.role-picker select,
.filter-input,
.filter-select,
.progress-row textarea,
.auth-form input {
  height: 38px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface-muted);
  color: var(--text);
  font-size: 13px;
  box-shadow: none;
}

.role-picker select:focus,
.filter-input:focus,
.filter-select:focus,
.progress-row textarea:focus,
.auth-form input:focus {
  border-color: var(--border-strong);
  box-shadow: 0 0 0 3px rgba(31, 36, 33, 0.06);
}

.auth-box {
  margin: 0;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.auth-user {
  max-width: 210px;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-muted);
  color: var(--text-soft);
}

.auth-user strong {
  color: var(--text);
}

.auth-user span {
  color: var(--text-faint);
}

.auth-btn,
.auth-btn.ghost,
.back-btn,
.clear-btn,
.mastery-entry-btn,
.mastery-dialog-header button,
.auth-close,
.auth-switch button,
.active-problem button,
.hot100-header button,
.search-btn,
.wrong-book-btn,
.study-plan-btn,
.save-btn,
.auth-submit,
.coach-btn {
  height: 36px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface-muted);
  color: var(--text);
  box-shadow: none;
  font-size: 13px;
  transition: background 140ms ease, border-color 140ms ease, transform 140ms ease;
}

.auth-btn,
.search-btn,
.save-btn,
.auth-submit,
.coach-btn {
  border-color: transparent;
  background: var(--text);
  color: #fff;
}

.coach-btn {
  background: var(--accent);
}

.auth-btn:hover,
.auth-btn.ghost:hover,
.back-btn:hover,
.clear-btn:hover,
.mastery-entry-btn:hover,
.mastery-dialog-header button:hover,
.auth-close:hover,
.auth-switch button:hover,
.active-problem button:hover,
.hot100-header button:hover,
.search-btn:hover,
.wrong-book-btn:hover,
.study-plan-btn:hover,
.save-btn:hover,
.auth-submit:hover,
.coach-btn:hover {
  background: var(--surface-hover);
  border-color: var(--border-strong);
  transform: none;
  filter: none;
  box-shadow: none;
}

.auth-btn:hover,
.search-btn:hover,
.save-btn:hover,
.auth-submit:hover {
  background: #303632;
}

.coach-btn:hover {
  background: #0b7f65;
}

.messages-area {
  flex: 1;
  min-height: 0;
  padding: 30px 0 18px;
  background:
    linear-gradient(90deg, transparent 0, transparent calc(50% - 390px), rgba(0, 0, 0, 0.025) calc(50% - 390px), rgba(0, 0, 0, 0.025) calc(50% - 389px), transparent calc(50% - 389px)),
    var(--surface);
}

.welcome-card {
  max-width: 680px;
  margin: 44px auto 24px;
  padding: 0 26px;
  border: 0;
  background: transparent;
  box-shadow: none;
  text-align: left;
}

.welcome-card h2 {
  font-size: 28px;
  line-height: 1.18;
  color: var(--text);
}

.welcome-card p {
  max-width: 600px;
  color: var(--text-soft);
  font-size: 14px;
}

.capability-list {
  margin-top: 18px;
  gap: 8px;
}

.capability-list span,
.problem-tags span,
.study-plan-meta span {
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-muted);
  color: var(--text-soft);
  font-size: 12px;
  padding: 3px 8px;
}

.tip {
  color: var(--text-faint);
}

.active-problem {
  width: fit-content;
  max-width: 100%;
  margin-top: 10px;
  padding: 5px 6px 5px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-muted);
  color: var(--text-soft);
}

.typing-avatar {
  width: 32px;
  height: 32px;
  border: 1px solid var(--border);
  box-shadow: none;
}

.typing-cursor {
  width: 6px;
  height: 16px;
  background: var(--accent);
}

.hot100-panel {
  background: var(--surface);
}

.hot100-header {
  min-height: 64px;
  padding: 15px 16px;
  border-bottom: 1px solid var(--border);
}

.hot100-header h2 {
  color: var(--text);
  font-size: 17px;
  font-weight: 700;
}

.dataset-stats {
  color: var(--text-faint);
  font-size: 12px;
}

.filter-section {
  display: grid;
  grid-template-columns: 1fr;
  gap: 9px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--border);
  background: #fbfbfa;
}

.search-line {
  grid-template-columns: 1fr 66px;
  gap: 8px;
}

.filter-row,
.study-plan-actions {
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.clear-btn,
.wrong-book-btn,
.study-plan-btn,
.mastery-entry-btn {
  height: 32px;
  color: var(--text-soft);
}

.wrong-book-btn {
  background: #f8f6f5;
  color: var(--text-soft);
}

.study-plan-btn {
  background: var(--surface-muted);
}

.auth-tip,
.insight-box {
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--surface);
  color: var(--text-soft);
  font-size: 12px;
}

.insight-box strong {
  color: var(--text);
}

.problem-list,
.study-plan-list {
  padding: 8px;
}

.problem-item {
  margin-bottom: 6px;
  padding: 11px 12px;
  border: 1px solid transparent;
  border-radius: 14px;
  background: transparent;
}

.problem-item:hover {
  background: var(--surface-muted);
  transform: none;
}

.problem-item.active {
  border-color: rgba(13, 143, 114, 0.18);
  background: var(--accent-soft);
}

.problem-top {
  align-items: flex-start;
}

.problem-title {
  color: var(--text);
  font-size: 14px;
  font-weight: 650;
  line-height: 1.35;
}

.problem-meta {
  gap: 5px;
}

.difficulty {
  color: var(--text-faint);
  font-size: 10px;
  font-weight: 650;
}

.status-badge {
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface);
  color: var(--text-faint);
  font-size: 10px;
  line-height: 17px;
  padding: 0 7px;
}

.status-badge.is-completed,
.status-badge.is-mastered {
  border-color: rgba(13, 143, 114, 0.2);
  background: rgba(13, 143, 114, 0.07);
  color: #237764;
}

.status-badge.is-wrong {
  border-color: rgba(170, 58, 58, 0.16);
  background: rgba(170, 58, 58, 0.06);
  color: #8f4a4a;
}

.problem-tags {
  margin-top: 8px;
  gap: 5px;
}

.problem-detail {
  padding: 16px;
}

.progress-editor,
.saved-record,
.wrong-book-summary,
.study-plan-summary,
.mastery-list {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--surface-muted);
}

.study-plan-item {
  margin-bottom: 6px;
  border: 0;
  border-radius: 14px;
  background: transparent;
}

.study-plan-item:hover {
  background: var(--surface-muted);
}

.study-plan-day {
  border-color: var(--border);
  background: var(--surface);
  color: var(--text-soft);
}

.auth-modal,
.mastery-dialog {
  border: 1px solid var(--border);
  border-radius: 20px;
  background: var(--surface);
  box-shadow: var(--shadow-soft);
}

.mobile-switch {
  gap: 6px;
}

.mobile-switch button {
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface);
  color: var(--text-soft);
}

.mobile-switch button:first-child,
.mobile-switch button:last-child {
  border-radius: 999px;
}

.mobile-switch button.active {
  border-color: rgba(13, 143, 114, 0.2);
  background: var(--accent-soft);
  color: var(--accent);
}

@media (prefers-color-scheme: dark) {
  .app-shell {
    --bg: #0d100f;
    --surface: #151917;
    --surface-muted: #1d231f;
    --surface-hover: #252c28;
    --text: #edf1ee;
    --text-soft: #b1bab5;
    --text-faint: #7f8a85;
    --border: rgba(255, 255, 255, 0.08);
    --border-strong: rgba(255, 255, 255, 0.14);
    --accent: #2eb895;
    --accent-soft: rgba(46, 184, 149, 0.1);
    background: var(--bg);
  }

  .session-sidebar {
    background: #0f1411;
  }

  .panel-header,
  .filter-section {
    background: rgba(21, 25, 23, 0.9);
  }

  .messages-area,
  .chat-panel,
  .hot100-panel {
    background: var(--surface);
  }

  .messages-area {
    background:
      linear-gradient(90deg, transparent 0, transparent calc(50% - 390px), rgba(255, 255, 255, 0.025) calc(50% - 390px), rgba(255, 255, 255, 0.025) calc(50% - 389px), transparent calc(50% - 389px)),
      var(--surface);
  }

  .auth-btn,
  .search-btn,
  .save-btn,
  .auth-submit {
    background: #edf1ee;
    color: #151917;
  }

  .auth-btn:hover,
  .search-btn:hover,
  .save-btn:hover,
  .auth-submit:hover {
    background: #dce4df;
  }

  .status-badge.is-completed,
  .status-badge.is-mastered {
    color: #8bdcc7;
  }

  .status-badge.is-wrong {
    color: #d8a2a2;
  }
}

@media (max-width: 1260px) {
  .workspace {
    grid-template-columns: 236px minmax(0, 1fr) 360px;
  }

  .panel-header {
    grid-template-columns: 1fr;
  }

  .role-picker,
  .auth-box {
    justify-content: flex-start;
  }
}

@media (max-width: 1180px) {
  .workspace {
    display: block;
    height: calc(100vh - 64px);
  }

  .session-sidebar {
    display: none;
  }
}

@media (max-width: 820px) {
  .app-shell {
    padding: 8px;
  }

  .panel-header {
    padding: 15px;
  }

  .title-wrap h1 {
    font-size: 23px;
  }

  .role-picker {
    grid-template-columns: 1fr;
    min-width: 0;
  }

  .messages-area {
    padding-top: 18px;
  }

  .welcome-card {
    margin: 22px auto;
    padding: 0 16px;
  }

  .filter-section {
    padding: 10px;
  }
}

/* Layout bugfix: keep three columns stable and prevent header title squeeze */
.app-shell {
  height: 100vh;
  min-height: 100vh;
  padding: 0;
  overflow: hidden;
}

.workspace {
  width: 100%;
  max-width: none;
  height: 100vh;
  margin: 0;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 400px;
  gap: 0;
  overflow: hidden;
}

.session-sidebar,
.chat-panel,
.hot100-panel {
  height: 100vh;
  min-height: 0;
  border-radius: 0;
}

.session-sidebar {
  min-width: 0;
  overflow: hidden;
}

.chat-panel {
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.hot100-panel {
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.hot100-body,
.problem-list,
.study-plan-list,
.problem-detail {
  min-height: 0;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px 24px;
  flex-shrink: 0;
}

.title-wrap {
  flex: 1 1 320px;
  min-width: 280px;
  max-width: 100%;
}

.title-wrap h1,
.title-wrap p,
.eyebrow {
  writing-mode: horizontal-tb;
  word-break: normal;
  overflow-wrap: break-word;
  white-space: normal;
}

.role-picker {
  flex: 0 1 340px;
  min-width: 300px;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 10px;
}

.select-field {
  flex: 1 1 145px;
  min-width: 0;
}

.role-picker select {
  width: 100%;
  min-width: 0;
}

.auth-box {
  flex: 0 1 auto;
  min-width: 0;
  flex-wrap: wrap;
}

.auth-user {
  min-width: 0;
}

.messages-area {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

.chat-input {
  flex-shrink: 0;
}

@media (max-width: 1180px) {
  .app-shell {
    padding: 8px;
  }

  .workspace {
    display: block;
    height: calc(100vh - 48px);
  }

  .chat-panel,
  .hot100-panel {
    height: 100%;
    border-radius: 20px;
  }
}

@media (max-width: 820px) {
  .title-wrap {
    flex-basis: 100%;
    min-width: 0;
  }

  .role-picker,
  .auth-box {
    flex-basis: 100%;
    min-width: 0;
  }
}

/* Compact OpenAI-like polish pass */
.app-shell {
  --app-bg: #f7f7f5;
  --app-surface: #ffffff;
  --app-surface-soft: #f4f4f2;
  --app-surface-hover: #eeeeeb;
  --app-text: #111827;
  --app-muted: #6b7280;
  --app-faint: #9ca3af;
  --app-border: #e5e7eb;
  --app-border-strong: #d1d5db;
  --app-accent: #111827;
  --app-green: #0f8f72;
  font-family: var(--font-claude-ui);
  color: var(--app-text);
  background: var(--app-bg);
  animation: appEnter 220ms ease-out both;
}

.workspace {
  background: var(--app-bg);
}

.session-sidebar {
  width: 280px;
  padding: 12px;
  background: #0f1713;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-block {
  padding: 4px 6px 12px;
}

.brand-mark {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.1);
}

.brand-block strong {
  font-size: 14px;
}

.brand-block span,
.sidebar-label,
.session-item small {
  font-size: 12px;
}

.new-chat-btn,
.session-item,
.sidebar-footer button,
.sidebar-footer a {
  transition: all 0.2s ease;
}

.new-chat-btn {
  height: 38px;
  border-radius: 12px;
}

.session-item {
  border-radius: 12px;
  padding: 9px 10px;
}

.session-item:hover,
.sidebar-footer button:hover,
.sidebar-footer a:hover {
  background: rgba(255, 255, 255, 0.08);
}

.session-item:active,
.new-chat-btn:active,
.sidebar-footer button:active,
.sidebar-footer a:active {
  transform: scale(0.99);
}

.chat-panel {
  background: var(--app-surface);
  border-left: 1px solid var(--app-border);
  border-right: 1px solid var(--app-border);
  animation: panelEnter 260ms ease-out both;
}

.panel-header {
  padding: 13px 18px 11px;
  gap: 10px 16px;
  background: rgba(250, 250, 249, 0.92);
  border-bottom: 1px solid var(--app-border);
}

.title-wrap {
  flex-basis: 300px;
}

.eyebrow {
  margin-bottom: 2px;
  color: var(--app-faint);
  font-size: 12px;
  line-height: 1.3;
}

.title-wrap h1 {
  margin: 0;
  color: var(--app-text);
  font-size: 24px;
  line-height: 1.25;
  font-weight: 700;
}

.title-wrap p {
  margin-top: 4px;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.45;
}

.role-picker {
  flex-basis: 292px;
  min-width: 260px;
  gap: 8px;
}

.select-field {
  flex-basis: 132px;
  gap: 4px;
}

.role-picker label {
  color: var(--app-muted);
  font-size: 12px;
  line-height: 1.3;
}

.role-picker select,
.filter-input,
.filter-select,
.auth-form input,
.progress-row textarea {
  height: 34px;
  border: 1px solid var(--app-border);
  border-radius: 12px;
  background: #fafafa;
  color: var(--app-text);
  font-size: 13px;
  line-height: 1.4;
  transition: all 0.2s ease;
}

.role-picker select:hover,
.filter-input:hover,
.filter-select:hover,
.auth-form input:hover,
.progress-row textarea:hover {
  border-color: var(--app-border-strong);
  background: #ffffff;
}

.role-picker select:focus,
.filter-input:focus,
.filter-select:focus,
.auth-form input:focus,
.progress-row textarea:focus {
  border-color: #9ca3af;
  box-shadow: 0 0 0 3px rgba(17, 24, 39, 0.06);
}

.auth-box {
  gap: 6px;
}

.auth-user {
  max-width: 180px;
  height: 34px;
  padding: 0 10px;
  border-color: var(--app-border);
  background: var(--app-surface-soft);
}

.auth-btn,
.auth-btn.ghost,
.hot100-header button,
.back-btn,
.search-btn,
.clear-btn,
.wrong-book-btn,
.study-plan-btn,
.mastery-entry-btn,
.save-btn,
.coach-btn,
.auth-submit,
.auth-close,
.auth-switch button {
  height: 34px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1;
  transition: all 0.2s ease;
}

.auth-btn,
.search-btn,
.save-btn,
.auth-submit {
  border-color: transparent;
  background: var(--app-accent);
  color: #fff;
}

.auth-btn:hover,
.search-btn:hover,
.save-btn:hover,
.auth-submit:hover {
  background: #252f3f;
}

.auth-btn.ghost,
.hot100-header button,
.back-btn,
.clear-btn,
.wrong-book-btn,
.study-plan-btn,
.mastery-entry-btn {
  border: 1px solid var(--app-border);
  background: #fafafa;
  color: var(--app-text);
}

.auth-btn.ghost:hover,
.hot100-header button:hover,
.back-btn:hover,
.clear-btn:hover,
.wrong-book-btn:hover,
.study-plan-btn:hover,
.mastery-entry-btn:hover {
  background: var(--app-surface-hover);
  border-color: var(--app-border-strong);
}

.auth-btn:active,
.hot100-header button:active,
.back-btn:active,
.search-btn:active,
.clear-btn:active,
.wrong-book-btn:active,
.study-plan-btn:active,
.mastery-entry-btn:active,
.save-btn:active,
.coach-btn:active,
.auth-submit:active {
  transform: scale(0.98);
}

.messages-area {
  padding: 18px 0 10px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfbfa 100%);
}

.welcome-card {
  max-width: 660px;
  margin: 20px auto 12px;
  padding: 0 22px;
}

.welcome-card h2 {
  margin: 0;
  font-size: 24px;
  line-height: 1.28;
}

.welcome-card p {
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.5;
}

.capability-list {
  margin-top: 12px;
  gap: 6px;
}

.capability-list span,
.problem-tags span,
.study-plan-meta span {
  padding: 2px 8px;
  border: 1px solid var(--app-border);
  border-radius: 999px;
  background: #f3f4f6;
  color: var(--app-muted);
  font-size: 11px;
  line-height: 18px;
}

.active-problem {
  margin-top: 7px;
  padding: 4px 6px 4px 9px;
  font-size: 12px;
}

.hot100-panel {
  width: 400px;
  background: #fbfbfa;
  border-left: 1px solid var(--app-border);
  animation: panelEnter 300ms ease-out both;
}

.hot100-header {
  min-height: 54px;
  padding: 12px 14px;
  background: #fbfbfa;
  border-bottom: 1px solid var(--app-border);
}

.hot100-header h2 {
  font-size: 18px;
  line-height: 1.3;
}

.dataset-stats {
  font-size: 12px;
  color: var(--app-muted);
}

.filter-section {
  padding: 10px 12px;
  gap: 8px;
  background: #f7f7f5;
  border-bottom: 1px solid var(--app-border);
  transition: opacity 0.2s ease;
}

.search-line {
  grid-template-columns: minmax(0, 1fr) 64px;
}

.problem-list,
.study-plan-list {
  padding: 10px;
}

.problem-item {
  margin-bottom: 8px;
  padding: 10px 11px;
  border: 1px solid var(--app-border);
  border-radius: 14px;
  background: var(--app-surface);
  box-shadow: 0 1px 2px rgba(17, 24, 39, 0.03);
  transition: all 0.2s ease;
  animation: itemFade 180ms ease-out both;
}

.problem-item:hover {
  background: #ffffff;
  border-color: var(--app-border-strong);
  box-shadow: 0 8px 20px rgba(17, 24, 39, 0.07);
  transform: translateY(-2px);
}

.problem-item.active {
  border-color: #c9d7d1;
  background: #f7fbf9;
}

.problem-title {
  font-size: 14px;
  line-height: 1.38;
  font-weight: 650;
}

.difficulty {
  font-size: 10px;
  letter-spacing: 0.02em;
  color: var(--app-faint);
}

.status-badge {
  height: 18px;
  padding: 0 7px;
  border: 1px solid var(--app-border);
  border-radius: 999px;
  background: #f9fafb;
  color: var(--app-muted);
  font-size: 10px;
  line-height: 16px;
}

.status-badge.is-completed,
.status-badge.is-mastered {
  border-color: #cde6dc;
  background: #f0f8f5;
  color: var(--app-green);
}

.status-badge.is-wrong {
  border-color: #f0d4d4;
  background: #fff7f7;
  color: #9f4545;
}

.problem-tags {
  margin-top: 7px;
  gap: 5px;
}

.auth-tip,
.insight-box,
.progress-editor,
.saved-record,
.wrong-book-summary,
.study-plan-summary {
  border-color: var(--app-border);
  background: #ffffff;
  border-radius: 14px;
}

.study-plan-item {
  border-radius: 14px;
  transition: all 0.2s ease;
}

.study-plan-item:hover {
  background: #ffffff;
  box-shadow: 0 6px 16px rgba(17, 24, 39, 0.05);
  transform: translateY(-1px);
}

@keyframes appEnter {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes panelEnter {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes itemFade {
  from {
    opacity: 0.75;
    transform: translateY(3px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-color-scheme: dark) {
  .app-shell {
    --app-bg: #0d100f;
    --app-surface: #151917;
    --app-surface-soft: #1d231f;
    --app-surface-hover: #242b27;
    --app-text: #f3f4f6;
    --app-muted: #a3aca7;
    --app-faint: #79837e;
    --app-border: rgba(255, 255, 255, 0.08);
    --app-border-strong: rgba(255, 255, 255, 0.14);
    background: var(--app-bg);
  }

  .chat-panel,
  .messages-area {
    background: var(--app-surface);
  }

  .panel-header,
  .hot100-header,
  .filter-section,
  .hot100-panel {
    background: #111614;
  }

  .role-picker select,
  .filter-input,
  .filter-select,
  .auth-form input,
  .progress-row textarea,
  .auth-btn.ghost,
  .hot100-header button,
  .back-btn,
  .clear-btn,
  .wrong-book-btn,
  .study-plan-btn,
  .mastery-entry-btn,
  .problem-item,
  .auth-tip,
  .insight-box,
  .progress-editor,
  .saved-record,
  .wrong-book-summary,
  .study-plan-summary {
    background: var(--app-surface-soft);
    color: var(--app-text);
    border-color: var(--app-border);
  }

  .problem-item:hover {
    background: #202721;
  }
}

@media (max-width: 1180px) {
  .hot100-panel {
    width: auto;
  }
}

/* Coaching chat header and Claude-like typography refinement */
.app-shell {
  --claude-bg: #faf9f5;
  --claude-panel: rgba(255, 255, 255, 0.82);
  --claude-soft: #f3f1eb;
  --claude-hover: #f3f4f6;
  --claude-text: #111827;
  --claude-muted: #6b7280;
  --claude-faint: #9ca3af;
  --claude-border: #e5e7eb;
  --claude-border-strong: #d1d5db;
  --claude-button: #111827;
  font-family: var(--font-claude-ui);
  line-height: 1.5;
  background: var(--claude-bg);
  color: var(--claude-text);
}

.panel-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 18px 10px;
  background: rgba(250, 249, 245, 0.86);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--claude-border);
  animation: headerIn 260ms ease-out both;
}

.chat-header-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  min-width: 0;
}

.title-wrap {
  flex: 1 1 auto;
  min-width: 0;
}

.eyebrow {
  margin: 0 0 2px;
  color: var(--claude-muted);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.35;
}

.title-wrap h1 {
  margin: 0;
  font-family: var(--font-claude-display);
  color: var(--claude-text);
  font-size: 24px;
  font-weight: 650;
  line-height: 1.22;
}

.title-wrap p {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  margin: 4px 0 0;
  color: var(--claude-muted);
  font-size: 13px;
  line-height: 1.45;
}

.header-label {
  flex-shrink: 0;
  color: var(--claude-faint);
  font-size: 12px;
}

.auth-box {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.auth-user {
  height: 32px;
  max-width: 220px;
  padding: 0 10px;
  border: 1px solid var(--claude-border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
}

.auth-user strong {
  color: var(--claude-text);
  font-size: 12px;
  font-weight: 600;
}

.auth-user span {
  max-width: 120px;
  color: var(--claude-faint);
  font-size: 12px;
}

.session-context-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 8px;
  border: 1px solid var(--claude-border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.68);
}

.active-problem,
.active-problem.is-empty {
  flex: 1 1 auto;
  width: auto;
  min-width: 0;
  max-width: none;
  margin: 0;
  padding: 0 4px 0 8px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--claude-muted);
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.active-problem span {
  flex-shrink: 0;
  color: var(--claude-faint);
}

.active-problem strong {
  min-width: 0;
  overflow: hidden;
  color: var(--claude-text);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.active-problem button {
  flex-shrink: 0;
  height: 28px;
  padding: 0 10px;
  border: 1px solid var(--claude-border);
  border-radius: 999px;
  background: transparent;
  color: var(--claude-muted);
  font-size: 12px;
  transition: all 0.2s ease;
}

.active-problem button:hover {
  background: var(--claude-hover);
  color: var(--claude-text);
}

.role-picker {
  flex: 0 0 auto;
  min-width: 330px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.select-field {
  flex: 1 1 0;
  min-width: 0;
  gap: 3px;
}

.select-field label {
  color: var(--claude-faint);
  font-size: 11px;
  font-weight: 500;
}

.role-picker select {
  height: 32px;
  border: 1px solid var(--claude-border);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--claude-text);
  font-size: 12px;
  transition: all 0.2s ease;
}

.role-picker select:hover,
.role-picker select:focus {
  border-color: var(--claude-border-strong);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(17, 24, 39, 0.05);
}

.auth-btn,
.auth-btn.ghost {
  height: 32px;
  border-radius: 999px;
  padding: 0 12px;
  font-size: 12px;
}

.messages-area {
  padding: 14px 0 8px;
  background:
    linear-gradient(180deg, rgba(250, 249, 245, 0.65), rgba(255, 255, 255, 0.76)),
    var(--claude-bg);
}

.welcome-card {
  margin-top: 12px;
}

.chat-panel {
  background: rgba(255, 255, 255, 0.78);
}

@keyframes headerIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-color-scheme: dark) {
  .app-shell {
    --claude-bg: #111411;
    --claude-panel: rgba(20, 24, 21, 0.86);
    --claude-soft: #1d231f;
    --claude-hover: #252b27;
    --claude-text: #f3f4f6;
    --claude-muted: #a3aca7;
    --claude-faint: #7f8a85;
    --claude-border: rgba(255, 255, 255, 0.08);
    --claude-border-strong: rgba(255, 255, 255, 0.14);
  }

  .panel-header,
  .session-context-bar,
  .auth-user,
  .role-picker select {
    background: rgba(20, 24, 21, 0.82);
  }

  .messages-area,
  .chat-panel {
    background: var(--claude-bg);
  }
}

@media (max-width: 1320px) {
  .chat-header-main,
  .session-context-bar {
    flex-wrap: wrap;
  }

  .auth-box {
    justify-content: flex-start;
  }

  .role-picker {
    flex: 1 1 100%;
    min-width: 0;
  }
}

@media (max-width: 820px) {
  .panel-header {
    padding: 12px;
  }

  .title-wrap h1 {
    font-size: 22px;
  }

  .session-context-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .active-problem {
    padding: 0;
  }

  .role-picker {
    flex-direction: column;
    align-items: stretch;
  }
}

/* Main content reduction pass */
.chat-panel .panel-header {
  gap: 12px;
  padding: 18px 24px 14px;
  background: rgba(250, 249, 245, 0.88);
}

.chat-header-main {
  align-items: center;
}

.chat-panel .title-wrap {
  flex: 1 1 auto;
  min-width: 0;
}

.chat-panel .eyebrow {
  margin-bottom: 4px;
  color: #8b928d;
  font-size: 12px;
  font-weight: 500;
}

.chat-panel .title-wrap h1 {
  font-family: var(--font-claude-display);
  font-size: 34px;
  line-height: 1.08;
  font-weight: 650;
  letter-spacing: -0.01em;
}

.chat-panel .title-wrap p {
  margin-top: 7px;
  color: #6b7280;
  font-size: 15px;
  line-height: 1.45;
}

.user-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.user-actions .auth-user {
  height: 40px;
  max-width: 240px;
  padding: 0 12px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  transition: all 0.2s ease;
}

.user-actions .auth-user:hover {
  background: #fff;
  border-color: #d1d5db;
}

.user-actions .auth-user strong {
  font-size: 13px;
  font-weight: 650;
}

.user-actions .auth-user span {
  max-width: 120px;
  color: #8b928d;
  font-size: 12px;
}

.user-actions .auth-btn {
  height: 40px;
  padding: 0 14px;
  border-radius: 999px;
}

.chat-panel .session-context-bar {
  width: fit-content;
  max-width: 100%;
  min-height: 58px;
  padding: 9px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 1px 2px rgba(17, 24, 39, 0.03);
  animation: headerIn 260ms ease-out both;
}

.chat-panel .session-context-bar:hover {
  border-color: #d1d5db;
  box-shadow: 0 8px 22px rgba(17, 24, 39, 0.05);
}

.chat-panel .active-problem {
  flex: 0 1 260px;
  min-width: 160px;
  padding: 0 8px;
}

.chat-panel .active-problem strong {
  max-width: 210px;
  font-size: 12px;
}

.chat-panel .active-problem button {
  height: 30px;
}

.chat-panel .role-picker {
  flex: 0 0 auto;
  min-width: 0;
  gap: 8px;
}

.chat-panel .select-field {
  position: relative;
  flex: 0 0 auto;
  min-width: 176px;
  height: 40px;
  display: grid;
  grid-template-columns: auto minmax(96px, 1fr);
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  transition: all 0.2s ease;
}

.chat-panel .select-field:hover {
  border-color: #d1d5db;
  background: #fff;
}

.chat-panel .select-field:focus-within {
  border-color: #d1d5db;
  box-shadow: 0 0 0 3px rgba(17, 24, 39, 0.05);
}

.chat-panel .select-field label {
  color: #8b928d;
  font-size: 11px;
  font-weight: 600;
}

.chat-panel .role-picker select {
  height: 36px;
  padding: 0 20px 0 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  color: #111827;
  font-size: 13px;
}

.chat-panel .role-picker select:focus {
  box-shadow: none;
}

.chat-panel .messages-area {
  padding-top: 18px;
}

.chat-panel .welcome-card {
  max-width: 720px;
  margin: 42px auto 0;
  padding: 0 24px;
  animation: panelEnter 260ms ease-out both;
}

.chat-panel .welcome-card h2 {
  font-size: 28px;
  font-weight: 650;
  letter-spacing: -0.01em;
}

.chat-panel .welcome-card p {
  max-width: 520px;
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.chat-panel .capability-list {
  margin-top: 14px;
}

@media (max-width: 1320px) {
  .chat-header-main {
    align-items: flex-start;
  }

  .user-actions {
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .chat-panel .session-context-bar {
    width: 100%;
  }

  .chat-panel .role-picker {
    flex-wrap: wrap;
  }
}

@media (max-width: 960px) {
  .user-actions .auth-user span {
    display: none;
  }
}

@media (max-width: 820px) {
  .chat-panel .panel-header {
    padding: 14px;
  }

  .chat-header-main,
  .user-actions,
  .chat-panel .session-context-bar,
  .chat-panel .role-picker {
    width: 100%;
  }

  .chat-panel .title-wrap h1 {
    font-size: 28px;
  }

  .chat-panel .select-field {
    width: 100%;
  }
}
</style>
