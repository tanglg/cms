<script setup lang="ts">
import { computed, reactive, ref } from 'vue'

type Role = 'SALES_MEMBER' | 'MANAGER'
type CustomerStatus = 'PROSPECTIVE_CUSTOMER' | 'FORMAL_CUSTOMER' | 'INACTIVE_CUSTOMER'
type SalesScreen = 'today' | 'customers' | 'detail' | 'me'

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

interface CustomerOwner {
  id: string
  name: string
}

interface PlannedContact {
  id: string
  customerId: string
  customerName: string
  plannedDate: string
}

interface CustomerContact {
  id: string
  customerId: string
  customerName: string
  salesMemberId: string
  salesMemberName: string
  contactTime: string
  communicationSummary: string
}

interface TodayPlannedContact {
  plannedContactId: string
  customerId: string
  customerName: string
  attentionLevel: number
  plannedDate: string
  registered: boolean
}

interface SalesCustomer {
  id: string
  name: string
  status: CustomerStatus
  attentionLevel: number
}

interface SalesCustomerDetail extends SalesCustomer {
  futurePlannedContacts: PlannedContact[]
  customerContacts: CustomerContact[]
}

interface ManagerCustomer {
  id: string
  name: string
  status: CustomerStatus
  attentionLevel: number
  owner: CustomerOwner
  agreementSigningDate: string | null
}

interface ManagerCustomerDetail extends ManagerCustomer {
  futurePlannedContacts: PlannedContact[]
  customerContacts: CustomerContact[]
}

const apiBase = import.meta.env.VITE_API_BASE_URL ?? ''
const token = ref(localStorage.getItem('cms-token') ?? '')
const currentAccount = ref<Account | null>(null)
const members = ref<Account[]>([])
const managerCustomers = ref<ManagerCustomer[]>([])
const selectedManagerCustomer = ref<ManagerCustomerDetail | null>(null)
const salesCustomers = ref<SalesCustomer[]>([])
const todayPlannedContacts = ref<TodayPlannedContact[]>([])
const selectedSalesCustomer = ref<SalesCustomerDetail | null>(null)
const errorMessage = ref('')
const successMessage = ref('')
const loading = ref(false)
const selectedMemberId = ref('')
const salesScreen = ref<SalesScreen>('today')
const contactModalOpen = ref(false)
const createModalOpen = ref(false)
const toastVisible = ref(false)
const createToastVisible = ref(false)
const nextPlanDate = ref('')
const detailPlanDate = ref('')
const filterName = ref('')
const filterLevel = ref('全部')
const filterStatus = ref('全部')
const managerCustomerSearch = ref('')
const managerCustomerOwnerFilter = ref('')
const managerCustomerStatusFilter = ref('')
const managerCustomerAttentionFilter = ref('')

const loginForm = reactive({
  phoneNumber: '13800000000',
  password: 'admin123456',
})

const createSalesCustomerForm = reactive({
  name: '',
})

const contactForm = reactive({
  customerId: '',
  communicationSummary: '',
  attentionLevel: 1,
})

const createManagerCustomerForm = reactive({
  name: '',
  status: 'PROSPECTIVE_CUSTOMER' as CustomerStatus,
  ownerId: '',
  agreementSigningDate: '',
})

const editManagerCustomerForm = reactive({
  name: '',
  ownerId: '',
  attentionLevel: 1,
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
const isSalesMember = computed(() => currentAccount.value?.roles.includes('SALES_MEMBER') ?? false)
const selectedMember = computed(() => members.value.find((member) => member.id === selectedMemberId.value))
const salesMemberOptions = computed(() =>
  members.value.filter((member) => member.active && member.roles.includes('SALES_MEMBER')),
)
const registeredTodayCount = computed(() => todayPlannedContacts.value.filter((contact) => contact.registered).length)
const visibleSalesCustomers = computed(() =>
  salesCustomers.value
    .filter((customer) => customer.name.includes(filterName.value.trim()))
    .filter((customer) => filterLevel.value === '全部' || String(customer.attentionLevel) === filterLevel.value)
    .filter((customer) => filterStatus.value === '全部' || customer.status === filterStatus.value),
)
const salesScreenTitle = computed(() => {
  if (salesScreen.value === 'customers') return '客户'
  if (salesScreen.value === 'detail') return '客户详情'
  if (salesScreen.value === 'me') return '我的'
  return '今天'
})
const contactCustomerName = computed(() => {
  if (selectedSalesCustomer.value?.id === contactForm.customerId) return selectedSalesCustomer.value.name
  const planned = todayPlannedContacts.value.find((contact) => contact.customerId === contactForm.customerId)
  if (planned) return planned.customerName
  return salesCustomers.value.find((customer) => customer.id === contactForm.customerId)?.name ?? ''
})
const businessDate = computed(() =>
  new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date()),
)

function roleText(member: Account) {
  const labels = []
  if (member.roles.includes('SALES_MEMBER')) labels.push('销售成员')
  if (member.roles.includes('MANAGER')) labels.push('管理者')
  return labels.join('、')
}

function customerStatusText(status: CustomerStatus) {
  if (status === 'PROSPECTIVE_CUSTOMER') return '潜在客户'
  if (status === 'FORMAL_CUSTOMER') return '正式客户'
  return '停用客户'
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
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
      ...options.headers,
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
      await loadManagerCustomers()
    }
    if (result.account.roles.includes('SALES_MEMBER')) {
      salesScreen.value = 'today'
      await loadSalesWorkspace()
    }
  }, '登录成功')
}

async function loadMe() {
  if (!token.value) return
  await run(async () => {
    currentAccount.value = await request<Account>('/api/auth/me')
    if (isManager.value) {
      await loadMembers()
      await loadManagerCustomers()
    }
    if (isSalesMember.value) {
      await loadSalesWorkspace()
    }
  })
}

async function loadSalesWorkspace() {
  await Promise.all([loadTodayPlannedContacts(), loadSalesCustomers()])
}

async function loadTodayPlannedContacts() {
  todayPlannedContacts.value = await request<TodayPlannedContact[]>('/api/sales/activity-calendar/today')
}

async function loadSalesCustomers() {
  salesCustomers.value = await request<SalesCustomer[]>('/api/sales/customers')
}

async function showSalesScreen(screen: SalesScreen) {
  salesScreen.value = screen
  if (screen === 'today') await run(loadTodayPlannedContacts)
  if (screen === 'customers') await run(loadSalesCustomers)
}

async function openSalesCustomerDetail(id: string) {
  await run(async () => {
    selectedSalesCustomer.value = await request<SalesCustomerDetail>(`/api/sales/customers/${id}`)
    contactForm.attentionLevel = selectedSalesCustomer.value.attentionLevel
    detailPlanDate.value = selectedSalesCustomer.value.futurePlannedContacts[0]?.plannedDate ?? ''
    salesScreen.value = 'detail'
  })
}

async function openContactModal(customerId: string) {
  await run(async () => {
    if (selectedSalesCustomer.value?.id !== customerId) {
      selectedSalesCustomer.value = await request<SalesCustomerDetail>(`/api/sales/customers/${customerId}`)
    }
    contactForm.customerId = customerId
    contactForm.communicationSummary = ''
    contactForm.attentionLevel = selectedSalesCustomer.value?.attentionLevel ?? 1
    nextPlanDate.value = ''
    contactModalOpen.value = true
  })
}

async function submitCustomerContact() {
  if (!contactForm.customerId) return
  await run(async () => {
    await request<CustomerContact>('/api/sales/customer-contacts', {
      method: 'POST',
      body: JSON.stringify({
        customerId: contactForm.customerId,
        communicationSummary: contactForm.communicationSummary,
      }),
    })
    if (selectedSalesCustomer.value && contactForm.attentionLevel !== selectedSalesCustomer.value.attentionLevel) {
      await request<SalesCustomer>(`/api/sales/customers/${contactForm.customerId}/attention-level`, {
        method: 'PATCH',
        body: JSON.stringify({ attentionLevel: contactForm.attentionLevel }),
      })
    }
    if (nextPlanDate.value) {
      await request<PlannedContact>('/api/sales/planned-contacts', {
        method: 'POST',
        body: JSON.stringify({
          customerId: contactForm.customerId,
          plannedDate: nextPlanDate.value,
        }),
      })
    }
    await loadSalesWorkspace()
    selectedSalesCustomer.value = await request<SalesCustomerDetail>(`/api/sales/customers/${contactForm.customerId}`)
    contactModalOpen.value = false
    toastVisible.value = true
    window.setTimeout(() => {
      toastVisible.value = false
    }, 1800)
  }, '接触记录已提交，可继续创建下一次计划接触或调整关注程度。')
}

async function createSalesCustomer() {
  await run(async () => {
    const customer = await request<SalesCustomer>('/api/sales/customers', {
      method: 'POST',
      body: JSON.stringify({ name: createSalesCustomerForm.name }),
    })
    createSalesCustomerForm.name = ''
    createModalOpen.value = false
    createToastVisible.value = true
    await loadSalesCustomers()
    await openSalesCustomerDetail(customer.id)
    window.setTimeout(() => {
      createToastVisible.value = false
    }, 1800)
  }, '潜在客户已创建')
}

async function saveDetailAttentionLevel() {
  if (!selectedSalesCustomer.value) return
  await run(async () => {
    await request<SalesCustomer>(`/api/sales/customers/${selectedSalesCustomer.value?.id}/attention-level`, {
      method: 'PATCH',
      body: JSON.stringify({ attentionLevel: contactForm.attentionLevel }),
    })
    await loadSalesCustomers()
    selectedSalesCustomer.value = await request<SalesCustomerDetail>(`/api/sales/customers/${selectedSalesCustomer.value?.id}`)
  }, '关注程度已保存')
}

async function createDetailPlan() {
  if (!selectedSalesCustomer.value || !detailPlanDate.value) return
  await run(async () => {
    await request<PlannedContact>('/api/sales/planned-contacts', {
      method: 'POST',
      body: JSON.stringify({
        customerId: selectedSalesCustomer.value?.id,
        plannedDate: detailPlanDate.value,
      }),
    })
    selectedSalesCustomer.value = await request<SalesCustomerDetail>(`/api/sales/customers/${selectedSalesCustomer.value?.id}`)
  }, '下一次计划接触已创建')
}

async function loadMembers() {
  members.value = await request<Account[]>('/api/manager/members')
  if (!selectedMemberId.value && members.value.length > 0) {
    selectMember(members.value[0])
  }
  if (!createManagerCustomerForm.ownerId && salesMemberOptions.value.length > 0) {
    createManagerCustomerForm.ownerId = salesMemberOptions.value[0].id
  }
}

async function loadManagerCustomers() {
  const params = new URLSearchParams()
  if (managerCustomerSearch.value.trim()) params.set('name', managerCustomerSearch.value.trim())
  if (managerCustomerOwnerFilter.value) params.set('ownerId', managerCustomerOwnerFilter.value)
  if (managerCustomerStatusFilter.value) params.set('status', managerCustomerStatusFilter.value)
  if (managerCustomerAttentionFilter.value) params.set('attentionLevel', managerCustomerAttentionFilter.value)
  const suffix = params.toString() ? `?${params.toString()}` : ''
  managerCustomers.value = await request<ManagerCustomer[]>(`/api/manager/customers${suffix}`)
  if (
    selectedManagerCustomer.value &&
    !managerCustomers.value.some((customer) => customer.id === selectedManagerCustomer.value?.id)
  ) {
    selectedManagerCustomer.value = null
  }
}

async function logout() {
  await run(async () => {
    await request<void>('/api/auth/session', { method: 'DELETE' })
    token.value = ''
    currentAccount.value = null
    localStorage.removeItem('cms-token')
    members.value = []
    managerCustomers.value = []
    salesCustomers.value = []
    todayPlannedContacts.value = []
    selectedManagerCustomer.value = null
    selectedSalesCustomer.value = null
    selectedMemberId.value = ''
  }, '已退出登录')
}

async function createManagerCustomer() {
  await run(async () => {
    const payload = {
      name: createManagerCustomerForm.name,
      status: createManagerCustomerForm.status,
      ownerId: createManagerCustomerForm.ownerId,
      agreementSigningDate:
        createManagerCustomerForm.status === 'FORMAL_CUSTOMER' ? createManagerCustomerForm.agreementSigningDate : null,
    }
    const customer = await request<ManagerCustomer>('/api/manager/customers', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    createManagerCustomerForm.name = ''
    createManagerCustomerForm.status = 'PROSPECTIVE_CUSTOMER'
    createManagerCustomerForm.agreementSigningDate = ''
    managerCustomerSearch.value = ''
    await loadManagerCustomers()
    await openManagerCustomer(customer.id)
  }, '客户已创建')
}

async function openManagerCustomer(id: string) {
  await run(async () => {
    selectedManagerCustomer.value = await request<ManagerCustomerDetail>(`/api/manager/customers/${id}`)
    editManagerCustomerForm.name = selectedManagerCustomer.value.name
    editManagerCustomerForm.ownerId = selectedManagerCustomer.value.owner.id
    editManagerCustomerForm.attentionLevel = selectedManagerCustomer.value.attentionLevel
  })
}

async function updateManagerCustomer() {
  if (!selectedManagerCustomer.value) return
  await run(async () => {
    const updated = await request<ManagerCustomer>(`/api/manager/customers/${selectedManagerCustomer.value?.id}`, {
      method: 'PATCH',
      body: JSON.stringify(editManagerCustomerForm),
    })
    selectedManagerCustomer.value = {
      ...selectedManagerCustomer.value!,
      ...updated,
    }
    await loadManagerCustomers()
  }, '客户治理信息已保存')
}

async function deactivateManagerCustomer() {
  if (!selectedManagerCustomer.value) return
  await run(async () => {
    const updated = await request<ManagerCustomer>(
      `/api/manager/customers/${selectedManagerCustomer.value?.id}/deactivate`,
      { method: 'POST' },
    )
    selectedManagerCustomer.value = {
      ...selectedManagerCustomer.value!,
      ...updated,
    }
    await loadManagerCustomers()
  }, '客户已停用')
}

async function restoreManagerCustomer() {
  if (!selectedManagerCustomer.value) return
  await run(async () => {
    const updated = await request<ManagerCustomer>(`/api/manager/customers/${selectedManagerCustomer.value?.id}/restore`, {
      method: 'POST',
    })
    selectedManagerCustomer.value = {
      ...selectedManagerCustomer.value!,
      ...updated,
    }
    await loadManagerCustomers()
  }, '客户已恢复')
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
      <div class="phone-status login-status">
        <span>09:41</span>
        <span>5G 88%</span>
      </div>

      <header class="phone-header login-header">
        <div>
          <h1 id="login-title">登录</h1>
        </div>
      </header>

      <form class="login-screen" @submit.prevent="login">
        <div class="login-form-top">
          <div class="brand-mark">客情登记</div>
          <label>
            手机号
            <input v-model="loginForm.phoneNumber" autocomplete="username" inputmode="tel" required />
          </label>
          <label>
            密码
            <input v-model="loginForm.password" autocomplete="current-password" type="password" required />
          </label>
        </div>
        <button class="login-action" :disabled="loading" type="submit">登录</button>
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
        <p v-else-if="toastVisible" class="success">提交成功</p>
        <p v-else-if="createToastVisible" class="success">创建成功</p>
      </div>

      <section v-if="isSalesMember" class="sales-mobile-stage" aria-label="销售端移动端">
        <section class="phone-frame" aria-labelledby="sales-phone-title">
          <div class="phone-status">
            <span>09:41</span>
            <span>5G 88%</span>
          </div>

          <header class="phone-header">
            <div>
              <p class="eyebrow">移动端</p>
              <h1 id="sales-phone-title">{{ salesScreenTitle }}</h1>
            </div>
            <button
              v-if="salesScreen === 'customers'"
              class="phone-icon-action"
              type="button"
              aria-label="新建潜在客户"
              @click="createModalOpen = true"
            >
              +
            </button>
          </header>

          <main class="phone-content">
            <section v-if="salesScreen === 'today'" class="phone-stack" aria-labelledby="mobile-today-title">
              <div class="today-summary-card">
                <p id="mobile-today-title">今天计划客户</p>
                <strong>{{ registeredTodayCount }} / {{ todayPlannedContacts.length }}</strong>
              </div>

              <div class="mobile-today-list">
                <article
                  v-for="plannedContact in todayPlannedContacts"
                  :key="plannedContact.plannedContactId"
                  class="mobile-customer-card"
                  :class="{ registered: plannedContact.registered }"
                >
                  <div class="mobile-card-head">
                    <button class="mobile-card-title" type="button" @click="openSalesCustomerDetail(plannedContact.customerId)">
                      <strong>{{ plannedContact.customerName }}</strong>
                      <small>{{ plannedContact.plannedDate }} · 关注 {{ plannedContact.attentionLevel }} 级</small>
                    </button>
                    <span class="mobile-status-pill" :class="{ registered: plannedContact.registered }">
                      {{ plannedContact.registered ? '已登记' : '未登记' }}
                    </span>
                  </div>

                  <div class="mobile-card-actions">
                    <span class="attention-stars" :aria-label="`关注程度 ${plannedContact.attentionLevel} 级`">
                      <span v-for="level in [1, 2, 3, 4, 5]" :key="level" :class="{ active: level <= plannedContact.attentionLevel }">
                        ★
                      </span>
                    </span>
                    <button class="mobile-link-action" :disabled="loading" type="button" @click="openContactModal(plannedContact.customerId)">
                      登记
                    </button>
                  </div>
                </article>
                <p v-if="todayPlannedContacts.length === 0" class="mobile-empty-state">今天暂无计划客户。</p>
              </div>
            </section>

            <section v-else-if="salesScreen === 'customers'" class="phone-stack" aria-labelledby="mobile-customers-title">
              <div class="mobile-filter-card">
                <input id="mobile-customers-title" v-model="filterName" placeholder="搜索客户名称" />
                <div class="mobile-filter-grid">
                  <select v-model="filterLevel">
                    <option>全部</option>
                    <option value="5">5 级关注</option>
                    <option value="4">4 级关注</option>
                    <option value="3">3 级关注</option>
                    <option value="2">2 级关注</option>
                    <option value="1">1 级关注</option>
                  </select>
                  <select v-model="filterStatus">
                    <option>全部</option>
                    <option value="PROSPECTIVE_CUSTOMER">潜在客户</option>
                    <option value="FORMAL_CUSTOMER">正式客户</option>
                    <option value="INACTIVE_CUSTOMER">停用客户</option>
                  </select>
                </div>
              </div>

              <div class="mobile-today-list">
                <button
                  v-for="customer in visibleSalesCustomers"
                  :key="customer.id"
                  class="mobile-customer-card mobile-customer-button"
                  type="button"
                  @click="openSalesCustomerDetail(customer.id)"
                >
                  <div class="mobile-card-head">
                    <span class="mobile-card-title">
                      <strong>{{ customer.name }}</strong>
                      <small>{{ customerStatusText(customer.status) }}</small>
                    </span>
                    <span class="mobile-subtle-pill">关注 {{ customer.attentionLevel }}</span>
                  </div>
                </button>
                <p v-if="visibleSalesCustomers.length === 0" class="mobile-empty-state">暂无客户。</p>
              </div>
            </section>

            <section v-else-if="salesScreen === 'detail' && selectedSalesCustomer" class="phone-stack">
              <section class="mobile-detail-card">
                <p class="eyebrow">{{ customerStatusText(selectedSalesCustomer.status) }}</p>
                <h3>{{ selectedSalesCustomer.name }}</h3>
                <div class="detail-form-grid">
                  <label>
                    关注程度
                    <select v-model.number="contactForm.attentionLevel">
                      <option v-for="level in [5, 4, 3, 2, 1]" :key="level" :value="level">{{ level }} 级</option>
                    </select>
                  </label>
                  <button class="secondary-action" :disabled="loading" type="button" @click="saveDetailAttentionLevel">保存关注程度</button>
                </div>
              </section>

              <section class="mobile-detail-card">
                <h3>计划下次接触日期</h3>
                <form class="inline-input-action" @submit.prevent="createDetailPlan">
                  <input v-model="detailPlanDate" type="date" required />
                  <button :disabled="loading" type="submit">创建</button>
                </form>
                <div class="history-list">
                  <article v-for="plannedContact in selectedSalesCustomer.futurePlannedContacts" :key="plannedContact.id">
                    <strong>{{ plannedContact.plannedDate }}</strong>
                    <p>{{ plannedContact.customerName }}</p>
                  </article>
                  <p v-if="selectedSalesCustomer.futurePlannedContacts.length === 0" class="mobile-empty-state">暂无未来计划。</p>
                </div>
              </section>

              <section class="mobile-detail-card">
                <div class="mobile-card-head">
                  <h3>历史接触记录</h3>
                  <button class="mobile-link-action" :disabled="loading" type="button" @click="openContactModal(selectedSalesCustomer.id)">
                    登记非计划接触记录
                  </button>
                </div>
                <div class="history-list">
                  <article v-for="item in selectedSalesCustomer.customerContacts" :key="item.id">
                    <strong>{{ formatDateTime(item.contactTime) }}</strong>
                    <p>{{ item.communicationSummary }}</p>
                  </article>
                  <p v-if="selectedSalesCustomer.customerContacts.length === 0" class="mobile-empty-state">暂无历史接触记录。</p>
                </div>
              </section>
            </section>

            <section v-else class="phone-stack">
              <section class="mobile-detail-card">
                <p class="eyebrow">当前用户</p>
                <h2>{{ currentAccount.name }}</h2>
                <p>{{ currentAccount.phoneNumber }} · {{ roleText(currentAccount) }}</p>
              </section>

              <section class="mobile-detail-card mobile-password-form">
                <h3>修改密码</h3>
                <label>
                  当前密码
                  <input v-model="passwordForm.currentPassword" autocomplete="current-password" type="password" />
                </label>
                <label>
                  新密码
                  <input v-model="passwordForm.newPassword" autocomplete="new-password" type="password" />
                </label>
                <button class="primary-action" :disabled="loading" type="button" @click="changeOwnPassword">保存密码</button>
              </section>

              <button class="mobile-logout-action" type="button" @click="logout">退出登录</button>
            </section>
          </main>

          <nav class="phone-nav" aria-label="销售端导航">
            <button :class="{ active: salesScreen === 'today' }" type="button" @click="showSalesScreen('today')">今天</button>
            <button :class="{ active: salesScreen === 'customers' }" type="button" @click="showSalesScreen('customers')">客户</button>
            <button :class="{ active: salesScreen === 'me' }" type="button" @click="showSalesScreen('me')">我的</button>
          </nav>
        </section>
      </section>

      <div v-if="isManager" class="content-grid manager-content-grid">
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

        <section class="panel form-panel" aria-labelledby="manager-create-customer-title">
          <h2 id="manager-create-customer-title">新建客户</h2>
          <form @submit.prevent="createManagerCustomer">
            <label>
              客户名称
              <input v-model="createManagerCustomerForm.name" required />
            </label>
            <label>
              客户状态
              <select v-model="createManagerCustomerForm.status" required>
                <option value="PROSPECTIVE_CUSTOMER">潜在客户</option>
                <option value="FORMAL_CUSTOMER">正式客户</option>
              </select>
            </label>
            <label>
              负责人
              <select v-model="createManagerCustomerForm.ownerId" required>
                <option v-for="member in salesMemberOptions" :key="member.id" :value="member.id">{{ member.name }}</option>
              </select>
            </label>
            <label v-if="createManagerCustomerForm.status === 'FORMAL_CUSTOMER'">
              协议签署日期
              <input v-model="createManagerCustomerForm.agreementSigningDate" type="date" required />
            </label>
            <button class="primary-action" :disabled="loading || salesMemberOptions.length === 0" type="submit">创建客户</button>
          </form>
        </section>

        <section class="panel customer-list-panel" aria-labelledby="manager-customers-title">
          <div class="section-heading">
            <div>
              <h2 id="manager-customers-title">客户治理</h2>
              <p>全团队客户按关注程度从高到低展示。</p>
            </div>
            <button class="ghost-action" type="button" @click="loadManagerCustomers">刷新</button>
          </div>

          <form class="manager-filter-grid" @submit.prevent="loadManagerCustomers">
            <label>
              客户名称
              <input v-model="managerCustomerSearch" placeholder="输入客户名称" />
            </label>
            <label>
              负责人
              <select v-model="managerCustomerOwnerFilter">
                <option value="">全部负责人</option>
                <option v-for="member in salesMemberOptions" :key="member.id" :value="member.id">{{ member.name }}</option>
              </select>
            </label>
            <label>
              客户状态
              <select v-model="managerCustomerStatusFilter">
                <option value="">全部状态</option>
                <option value="PROSPECTIVE_CUSTOMER">潜在客户</option>
                <option value="FORMAL_CUSTOMER">正式客户</option>
                <option value="INACTIVE_CUSTOMER">停用客户</option>
              </select>
            </label>
            <label>
              关注程度
              <select v-model="managerCustomerAttentionFilter">
                <option value="">全部关注</option>
                <option v-for="level in [1, 2, 3, 4, 5]" :key="level" :value="String(level)">关注 {{ level }}</option>
              </select>
            </label>
            <button class="secondary-action" :disabled="loading" type="submit">筛选</button>
          </form>

          <div class="customer-list">
            <button
              v-for="customer in managerCustomers"
              :key="customer.id"
              class="customer-row"
              :class="{ selected: selectedManagerCustomer?.id === customer.id, inactive: customer.status === 'INACTIVE_CUSTOMER' }"
              type="button"
              @click="openManagerCustomer(customer.id)"
            >
              <span>
                <strong>{{ customer.name }}</strong>
                <small>{{ customerStatusText(customer.status) }} · {{ customer.owner.name }}</small>
              </span>
              <em>关注 {{ customer.attentionLevel }}</em>
            </button>
            <p v-if="managerCustomers.length === 0" class="empty-state">暂无客户。</p>
          </div>
        </section>

        <section v-if="selectedManagerCustomer" class="panel customer-detail-panel" aria-labelledby="manager-customer-detail-title">
          <div class="section-heading">
            <div>
              <p class="eyebrow">管理端客户详情</p>
              <h2 id="manager-customer-detail-title">{{ selectedManagerCustomer.name }}</h2>
            </div>
            <span class="status-pill">{{ customerStatusText(selectedManagerCustomer.status) }}</span>
          </div>

          <div class="detail-grid">
            <span>
              <small>负责人</small>
              <strong>{{ selectedManagerCustomer.owner.name }}</strong>
            </span>
            <span>
              <small>关注程度</small>
              <strong>{{ selectedManagerCustomer.attentionLevel }}</strong>
            </span>
            <span>
              <small>协议签署日期</small>
              <strong>{{ selectedManagerCustomer.agreementSigningDate ?? '未签署' }}</strong>
            </span>
          </div>

          <form class="manager-edit-grid" @submit.prevent="updateManagerCustomer">
            <label>
              客户名称
              <input v-model="editManagerCustomerForm.name" required />
            </label>
            <label>
              负责人
              <select v-model="editManagerCustomerForm.ownerId" required>
                <option v-for="member in salesMemberOptions" :key="member.id" :value="member.id">{{ member.name }}</option>
              </select>
            </label>
            <label>
              关注程度
              <select v-model.number="editManagerCustomerForm.attentionLevel" required>
                <option v-for="level in [1, 2, 3, 4, 5]" :key="level" :value="level">关注 {{ level }}</option>
              </select>
            </label>
            <button class="primary-action" :disabled="loading" type="submit">保存治理信息</button>
          </form>

          <div class="action-row">
            <button
              class="danger-action"
              :disabled="loading || selectedManagerCustomer.status === 'INACTIVE_CUSTOMER'"
              type="button"
              @click="deactivateManagerCustomer"
            >
              停用客户
            </button>
            <button
              class="secondary-action"
              :disabled="loading || selectedManagerCustomer.status !== 'INACTIVE_CUSTOMER'"
              type="button"
              @click="restoreManagerCustomer"
            >
              恢复客户
            </button>
          </div>

          <section class="detail-section">
            <h3>未来计划</h3>
            <div class="planned-contact-list">
              <div
                v-for="plannedContact in selectedManagerCustomer.futurePlannedContacts"
                :key="plannedContact.id"
                class="planned-contact-row readonly"
              >
                <span>
                  <strong>{{ plannedContact.plannedDate }}</strong>
                  <small>{{ plannedContact.customerName }}</small>
                </span>
              </div>
              <p v-if="selectedManagerCustomer.futurePlannedContacts.length === 0" class="empty-state">暂无未来计划。</p>
            </div>
          </section>

          <section class="detail-section">
            <h3>历史接触记录</h3>
            <div class="planned-contact-list">
              <div
                v-for="contact in selectedManagerCustomer.customerContacts"
                :key="contact.id"
                class="planned-contact-row readonly"
              >
                <span>
                  <strong>{{ formatDateTime(contact.contactTime) }} · {{ contact.salesMemberName }}</strong>
                  <small>{{ contact.communicationSummary }}</small>
                </span>
              </div>
              <p v-if="selectedManagerCustomer.customerContacts.length === 0" class="empty-state">暂无历史接触记录。</p>
            </div>
          </section>
        </section>
      </div>

      <div v-if="createModalOpen" class="modal-scrim" role="dialog" aria-modal="true" aria-labelledby="create-modal-title">
        <form class="sheet-modal" @submit.prevent="createSalesCustomer">
          <header class="sheet-header">
            <h2 id="create-modal-title">新建潜在客户</h2>
          </header>
          <div class="sheet-body">
            <section class="mobile-detail-card">
              <label>
                客户名称
                <input v-model="createSalesCustomerForm.name" placeholder="填写客户名称" required />
              </label>
            </section>
          </div>
          <footer class="sheet-actions">
            <div class="modal-actions">
              <button class="orange-action" type="button" @click="createModalOpen = false">取消</button>
              <button class="primary-action" :disabled="loading" type="submit">创建潜在客户</button>
            </div>
          </footer>
        </form>
      </div>

      <div v-if="contactModalOpen" class="modal-scrim" role="dialog" aria-modal="true" aria-labelledby="contact-modal-title">
        <form class="sheet-modal" @submit.prevent="submitCustomerContact">
          <header class="sheet-header">
            <h2 id="contact-modal-title">登记接触记录</h2>
          </header>
          <div class="sheet-body">
            <div class="contact-customer-card">
              <span>
                <small>客户</small>
                <strong>{{ contactCustomerName }}</strong>
              </span>
              <span>
                <small>接触日期</small>
                <strong>{{ businessDate }}</strong>
              </span>
            </div>
            <label>
              沟通内容
              <textarea
                v-model="contactForm.communicationSummary"
                placeholder="请填写：接触方式、接触对象、沟通内容、沟通结论。"
                required
              ></textarea>
            </label>
            <label class="mobile-detail-card">
              下一次计划日期
              <span class="inline-input-action">
                <input v-model="nextPlanDate" type="date" />
                <button type="button" @click="nextPlanDate = ''">清除</button>
              </span>
            </label>
            <label class="mobile-detail-card">
              关注程度
              <select v-model.number="contactForm.attentionLevel">
                <option v-for="level in [5, 4, 3, 2, 1]" :key="level" :value="level">{{ level }} 级关注</option>
              </select>
            </label>
          </div>
          <footer class="sheet-actions">
            <div class="modal-actions">
              <button class="orange-action" type="button" @click="contactModalOpen = false">取消</button>
              <button class="primary-action" :disabled="loading" type="submit">提交记录</button>
            </div>
          </footer>
        </form>
      </div>
    </section>
  </main>
</template>
