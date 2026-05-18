<script setup lang="ts">
import { computed, reactive, ref } from 'vue'

type Role = 'SALES_MEMBER' | 'MANAGER'

interface Account {
  id: string
  name: string
  phoneNumber: string
  roles: Role[]
  active: boolean
}

interface LoginResponse {
  token: string
  account: Account
}

const apiBase = import.meta.env.VITE_API_BASE_URL ?? ''
const token = ref(localStorage.getItem('cms-token') ?? '')
const currentAccount = ref<Account | null>(null)
const members = ref<Account[]>([])
const errorMessage = ref('')
const successMessage = ref('')
const loading = ref(false)
const selectedMemberId = ref('')

const loginForm = reactive({
  phoneNumber: '13800000000',
  password: 'admin123456',
})

const createForm = reactive({
  name: '',
  phoneNumber: '',
  initialPassword: '',
  salesMember: true,
  manager: false,
})

const editForm = reactive({
  name: '',
  phoneNumber: '',
  salesMember: true,
  manager: false,
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
})

const resetForm = reactive({
  newPassword: '',
})

const isManager = computed(() => currentAccount.value?.roles.includes('MANAGER') ?? false)
const selectedMember = computed(() => members.value.find((member) => member.id === selectedMemberId.value))

function roleText(member: Account) {
  const labels = []
  if (member.roles.includes('SALES_MEMBER')) labels.push('销售成员')
  if (member.roles.includes('MANAGER')) labels.push('管理者')
  return labels.join('、')
}

function authHeaders(): Record<string, string> {
  return token.value ? { Authorization: `Bearer ${token.value}` } : {}
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${apiBase}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
    },
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: '请求失败' }))
    throw new Error(body.message ?? '请求失败')
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

async function run(action: () => Promise<void>, success?: string) {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await action()
    if (success) successMessage.value = success
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '请求失败'
  } finally {
    loading.value = false
  }
}

async function login() {
  await run(async () => {
    const result = await request<LoginResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(loginForm),
    })
    token.value = result.token
    currentAccount.value = result.account
    localStorage.setItem('cms-token', result.token)
    if (result.account.roles.includes('MANAGER')) {
      await loadMembers()
    }
  }, '登录成功')
}

async function loadMe() {
  if (!token.value) return
  await run(async () => {
    currentAccount.value = await request<Account>('/api/auth/me')
    if (isManager.value) await loadMembers()
  })
}

async function loadMembers() {
  members.value = await request<Account[]>('/api/manager/members')
  if (!selectedMemberId.value && members.value.length > 0) {
    selectMember(members.value[0])
  }
}

async function logout() {
  await run(async () => {
    await request<void>('/api/auth/session', { method: 'DELETE' })
    token.value = ''
    currentAccount.value = null
    members.value = []
    selectedMemberId.value = ''
    localStorage.removeItem('cms-token')
  }, '已退出登录')
}

async function createMember() {
  await run(async () => {
    await request<Account>('/api/manager/members', {
      method: 'POST',
      body: JSON.stringify(createForm),
    })
    createForm.name = ''
    createForm.phoneNumber = ''
    createForm.initialPassword = ''
    createForm.salesMember = true
    createForm.manager = false
    await loadMembers()
  }, '成员已创建')
}

function selectMember(member: Account) {
  selectedMemberId.value = member.id
  editForm.name = member.name
  editForm.phoneNumber = member.phoneNumber
  editForm.salesMember = member.roles.includes('SALES_MEMBER')
  editForm.manager = member.roles.includes('MANAGER')
  resetForm.newPassword = ''
}

async function updateMember() {
  if (!selectedMemberId.value) return
  await run(async () => {
    await request<Account>(`/api/manager/members/${selectedMemberId.value}`, {
      method: 'PATCH',
      body: JSON.stringify(editForm),
    })
    await loadMembers()
  }, '成员资料已更新')
}

async function resetPassword() {
  if (!selectedMemberId.value) return
  await run(async () => {
    await request<void>(`/api/manager/members/${selectedMemberId.value}/reset-password`, {
      method: 'POST',
      body: JSON.stringify(resetForm),
    })
    resetForm.newPassword = ''
  }, '密码已重置')
}

async function deactivateMember() {
  if (!selectedMemberId.value) return
  await run(async () => {
    await request<void>(`/api/manager/members/${selectedMemberId.value}/deactivate`, {
      method: 'POST',
    })
    await loadMembers()
  }, '账号已停用')
}

async function changeOwnPassword() {
  await run(async () => {
    await request<void>('/api/auth/password', {
      method: 'POST',
      body: JSON.stringify(passwordForm),
    })
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
  }, '密码已修改')
}

loadMe()
</script>

<template>
  <main class="app-shell">
    <section v-if="!currentAccount" class="login-view" aria-labelledby="login-title">
      <div class="brand">
        <span class="brand-mark">CMS</span>
        <div>
          <h1 id="login-title">销售客户接触登记</h1>
          <p>使用手机号和密码登录。</p>
        </div>
      </div>

      <form class="panel form-panel" @submit.prevent="login">
        <label>
          手机号
          <input v-model="loginForm.phoneNumber" autocomplete="username" inputmode="tel" required />
        </label>
        <label>
          密码
          <input v-model="loginForm.password" autocomplete="current-password" type="password" required />
        </label>
        <button class="primary-action" :disabled="loading" type="submit">登录</button>
      </form>
    </section>

    <section v-else class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">当前账号</p>
          <h1>{{ currentAccount.name }}</h1>
          <p>{{ currentAccount.phoneNumber }} · {{ roleText(currentAccount) }}</p>
        </div>
        <button class="ghost-action" type="button" @click="logout">退出</button>
      </header>

      <div class="status-line" aria-live="polite">
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <p v-else-if="successMessage" class="success">{{ successMessage }}</p>
      </div>

      <div class="content-grid">
        <section class="panel form-panel" aria-labelledby="password-title">
          <h2 id="password-title">修改我的密码</h2>
          <form @submit.prevent="changeOwnPassword">
            <label>
              当前密码
              <input v-model="passwordForm.currentPassword" autocomplete="current-password" type="password" required />
            </label>
            <label>
              新密码
              <input v-model="passwordForm.newPassword" autocomplete="new-password" type="password" required />
            </label>
            <button class="primary-action" :disabled="loading" type="submit">修改密码</button>
          </form>
        </section>

        <template v-if="isManager">
          <section class="panel form-panel" aria-labelledby="create-title">
            <h2 id="create-title">创建成员账号</h2>
            <form @submit.prevent="createMember">
              <label>
                姓名
                <input v-model="createForm.name" required />
              </label>
              <label>
                手机号
                <input v-model="createForm.phoneNumber" inputmode="tel" required />
              </label>
              <label>
                初始密码
                <input v-model="createForm.initialPassword" type="password" required />
              </label>
              <div class="role-row">
                <label><input v-model="createForm.salesMember" type="checkbox" /> 销售成员</label>
                <label><input v-model="createForm.manager" type="checkbox" /> 管理者</label>
              </div>
              <button class="primary-action" :disabled="loading" type="submit">创建账号</button>
            </form>
          </section>

          <section class="panel member-list" aria-labelledby="members-title">
            <div class="section-heading">
              <h2 id="members-title">成员管理</h2>
              <button class="ghost-action" type="button" @click="loadMembers">刷新</button>
            </div>
            <button
              v-for="member in members"
              :key="member.id"
              class="member-row"
              :class="{ selected: member.id === selectedMemberId, inactive: !member.active }"
              type="button"
              @click="selectMember(member)"
            >
              <span>
                <strong>{{ member.name }}</strong>
                <small>{{ member.phoneNumber }} · {{ roleText(member) }}</small>
              </span>
              <em>{{ member.active ? '启用' : '停用' }}</em>
            </button>
          </section>

          <section v-if="selectedMember" class="panel form-panel" aria-labelledby="edit-title">
            <h2 id="edit-title">编辑成员</h2>
            <form @submit.prevent="updateMember">
              <label>
                姓名
                <input v-model="editForm.name" required />
              </label>
              <label>
                手机号
                <input v-model="editForm.phoneNumber" inputmode="tel" required />
              </label>
              <div class="role-row">
                <label><input v-model="editForm.salesMember" type="checkbox" /> 销售成员</label>
                <label><input v-model="editForm.manager" type="checkbox" /> 管理者</label>
              </div>
              <button class="primary-action" :disabled="loading" type="submit">保存修改</button>
            </form>

            <form class="reset-row" @submit.prevent="resetPassword">
              <label>
                重置密码
                <input v-model="resetForm.newPassword" type="password" required />
              </label>
              <button class="secondary-action" :disabled="loading" type="submit">重置</button>
            </form>

            <button class="danger-action" :disabled="loading || !selectedMember.active" type="button" @click="deactivateMember">
              停用账号
            </button>
          </section>
        </template>
      </div>
    </section>
  </main>
</template>
