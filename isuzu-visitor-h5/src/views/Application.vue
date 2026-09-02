<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useVisitorStore } from '@/stores/visitor'
import { getApplicationDetail, getSubmitToken, searchHost, submitApplication } from '@/api/visitor'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'ApplicationView' })

// 申请单页（PRD §5.3）+ 登记成功弹层（PRD §5.4）+ 编辑模式（PRD v1.9：列表页左滑「修改」进入）
const route = useRoute()
const router = useRouter()
const store = useVisitorStore()

// 编辑模式：带 applicationId 进入时回显原单，提交时后端撤销原单并新建（PRD v1.9）
const editApplicationId = computed(() => route.query.applicationId || '')

const form = reactive({ hostId: null, hostName: '', startTime: '', endTime: '', reason: '', remark: '' })

// ---- 随行人员（可选，最多 5 人，PRD v1.4 §5.3）----
const MAX_COMPANIONS = 5
const ID_CARD_RE = /^\d{17}[\dX]$/
const companions = ref([]) // [{ name, idCard }]

function addCompanion() {
  if (companions.value.length >= MAX_COMPANIONS) {
    showToast('最多添加5名随行人员')
    return
  }
  companions.value.push({ name: '', idCard: '' })
}

function removeCompanion(i) {
  companions.value.splice(i, 1)
}

// 身份证输入：仅允许数字与 X（自动转大写），截断 18 位
const idCardFormatter = (v) => v.replace(/[^\dXx]/g, '').slice(0, 18).toUpperCase()

// 行内校验：格式 / 申请单内重复
function idCardError(i) {
  const id = companions.value[i].idCard.trim()
  if (!id) return ''
  if (!ID_CARD_RE.test(id)) return '身份证号格式不正确'
  if (companions.value.some((c, j) => j !== i && c.idCard.trim() === id)) return '身份证号重复'
  return ''
}

// 已添加的行须姓名非空 + 身份证号有效且不重复；未添加任何行时视为有效（可空）
const companionsValid = computed(() => {
  const list = companions.value
  if (!list.length) return true
  const ids = new Set()
  for (const c of list) {
    const name = c.name.trim()
    const id = c.idCard.trim()
    if (!name || !ID_CARD_RE.test(id)) return false
    if (ids.has(id)) return false
    ids.add(id)
  }
  return true
})

// ---- 被访人查询弹窗（禁止手输，只能弹窗搜索选择）----
const showHostPopup = ref(false)
const hostKeyword = ref('')
const hostList = ref([])
const hostLoading = ref(false)
const selectedHostId = ref(null)
const hostSearched = ref(false)

async function onHostSearch() {
  if (!hostKeyword.value.trim()) {
    showToast('请输入被访人姓名')
    return
  }
  hostLoading.value = true
  try {
    const res = await searchHost(hostKeyword.value.trim())
    hostList.value = res.data.list || []
    selectedHostId.value = null
    hostSearched.value = true
  } catch {
    // 拦截器已提示
  } finally {
    hostLoading.value = false
  }
}

function onHostConfirm() {
  const host = hostList.value.find((h) => h.userId === selectedHostId.value)
  if (!host) {
    showToast('请选择被访人')
    return
  }
  form.hostId = host.userId
  form.hostName = host.name
  showHostPopup.value = false
}

// ---- 日期选择（PRD v1.10：以日期为最小单位，仅年月日，不选具体时间）----
// 访问跨度上限 2 个自然月（结束日期最晚 = 开始日期 + 2 个月）
const MAX_VISIT_MONTHS = 2
const now = new Date()
const minDate = new Date(now.getFullYear(), now.getMonth(), now.getDate())
const maxDate = new Date(minDate.getFullYear() + 1, minDate.getMonth(), minDate.getDate())
const nowStr = formatDateTime(new Date()) // 本地时间 yyyy-MM-dd HH:mm（勿用 toISOString，其返回 UTC 时间）

/** 加 N 个自然月（日溢出截断到目标月末，如 12-31 + 2 月 = 次年 2 月 28/29 日，与后端 Calendar.add 口径一致） */
function addMonths(date, months) {
  const target = new Date(date.getFullYear(), date.getMonth() + months, 1)
  const day = Math.min(date.getDate(), new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate())
  target.setDate(day)
  return target
}

const showDatePicker = ref(false)
const datePickerType = ref('start')
const dateValue = ref(nowStr.split(' ')[0].split('-').map(Number)) // [y, m, d]

function openDatePicker(type) {
  datePickerType.value = type
  // 打开时回填当前已选值或默认值
  const current = form[type === 'start' ? 'startTime' : 'endTime'] || nowStr
  dateValue.value = current.split(' ')[0].split('-').map(Number)
  showDatePicker.value = true
}

// van-date-picker confirm 时 v-model（dateValue）已同步为选中值，直接读取
function onDateConfirm() {
  const [y, m, d] = dateValue.value
  const date = new Date(y, m - 1, d)
  if (datePickerType.value === 'start') {
    // 开始日期不能早于当前日期（PRD §5.3）
    if (date < minDate) {
      showToast('开始日期不能早于当前日期')
      return
    }
    form.startTime = formatDateTime(date).slice(0, 10)
    // 结束日期早于新开始日期、或跨度超过 2 个自然月时清空
    const maxEnd = addMonths(date, MAX_VISIT_MONTHS)
    if (form.endTime && (form.endTime < form.startTime || new Date(`${form.endTime}T00:00:00`) > maxEnd)) {
      form.endTime = ''
    }
  } else {
    // 结束日期不能早于开始日期（允许同日，v1.10）
    if (!form.startTime) {
      showToast('请先选择开始日期')
      showDatePicker.value = false
      return
    }
    const startDay = new Date(`${form.startTime}T00:00:00`)
    if (date < startDay) {
      showToast('结束日期不能早于开始日期')
      return
    }
    // 访问跨度不能超过 2 个自然月（含首尾，与后端校验口径一致）
    if (date > addMonths(startDay, MAX_VISIT_MONTHS)) {
      showToast('访问时间不能超过2个月')
      return
    }
    form.endTime = formatDateTime(date).slice(0, 10)
  }
  showDatePicker.value = false
}

// ---- 提交 ----
const submitting = ref(false)
const showSuccess = ref(false)

// 一次性提交令牌（防重复提交）：sessionStorage 复用，提交成功弹层期间刷新页面后重放旧 token 会被后端 601 拒绝
const SUBMIT_TOKEN_KEY = 'visitor_submit_token'

async function fetchSubmitToken() {
  const cached = sessionStorage.getItem(SUBMIT_TOKEN_KEY)
  if (cached) {
    submitToken.value = cached
    return
  }
  try {
    const res = await getSubmitToken()
    submitToken.value = res.data.submitToken
    sessionStorage.setItem(SUBMIT_TOKEN_KEY, res.data.submitToken)
  } catch {
    // 拦截器已提示；submitToken 为空时 onSubmit 直接 return
  }
}

const submitToken = ref('')
fetchSubmitToken()

const canSubmit = computed(
  () => form.hostId && form.startTime && form.endTime && form.reason.trim() && companionsValid.value,
)

async function onSubmit() {
  // 同步守卫：loading 态重渲染前的双击窗口内拦截第二次点击
  if (submitting.value || !canSubmit.value || !submitToken.value) return
  // 编辑模式：未实际修改任何内容时不走提交修改流程，提示后直接返回列表
  if (editApplicationId.value && !isModified()) {
    showToast('原汁原味，零改动✨')
    router.replace('/list')
    return
  }
  submitting.value = true
  try {
    await submitApplication({
      visitorId: store.visitorId,
      hostId: form.hostId,
      // 日期粒度：开始日 00:00:00、结束日 23:59:59（PRD v1.10，当天整天有效）
      startTime: `${form.startTime} 00:00:00`,
      endTime: `${form.endTime} 23:59:59`,
      reason: form.reason.trim(),
      remark: form.remark.trim(),
      companions: companions.value.map((c) => ({ name: c.name.trim(), idCard: c.idCard.trim() })),
      submitToken: submitToken.value,
      replaceApplicationId: editApplicationId.value || undefined,
    })
    showSuccess.value = true
  } catch {
    // 拦截器已提示（如当日拒绝数达 3 次、重复提交被拦截）；令牌已失效则静默重领，表单数据保留可直接再提交
    sessionStorage.removeItem(SUBMIT_TOKEN_KEY)
    fetchSubmitToken()
  } finally {
    submitting.value = false
  }
}

function onSuccessClose() {
  showSuccess.value = false
  sessionStorage.removeItem(SUBMIT_TOKEN_KEY)
  router.replace('/list')
}

// ---- 编辑模式：拉取原单详情回显（详情接口仅待审批可查；已审批/已撤销时提示并返回列表）----
// 原始值快照（提交前未修改检测的基准）
const original = reactive({ hostId: null, startTime: '', endTime: '', reason: '', remark: '', companions: [] })

async function loadEditData() {
  try {
    const res = await getApplicationDetail(store.visitorId, editApplicationId.value)
    const d = res.data
    form.hostId = d.hostId
    form.hostName = d.hostName
    // 接口返回 yyyy-MM-dd HH:mm:ss，表单为 yyyy-MM-dd（日期粒度）
    form.startTime = (d.startTime || '').slice(0, 10)
    form.endTime = (d.endTime || '').slice(0, 10)
    form.reason = d.reason || ''
    form.remark = d.remark || ''
    companions.value = (d.companions || []).map((c) => ({ name: c.name, idCard: c.idCard }))
    // 快照原始值（副本，避免后续编辑污染基准）
    original.hostId = d.hostId
    original.startTime = form.startTime
    original.endTime = form.endTime
    original.reason = form.reason
    original.remark = form.remark
    original.companions = companions.value.map((c) => ({ ...c }))
  } catch {
    router.replace('/list')
  }
}

// 编辑模式提交前检查：是否真实修改了内容（时间/事由/随行人员按提交口径归一化比较）
function isModified() {
  if (form.hostId !== original.hostId) return true
  if (form.startTime !== original.startTime) return true
  if (form.endTime !== original.endTime) return true
  if (form.reason.trim() !== original.reason.trim()) return true
  if (form.remark.trim() !== original.remark.trim()) return true
  const norm = (list) => list.map((c) => ({ name: c.name.trim(), idCard: c.idCard.trim() }))
  const cur = norm(companions.value)
  const src = norm(original.companions)
  if (cur.length !== src.length) return true
  return cur.some((c, i) => c.name !== src[i].name || c.idCard !== src[i].idCard)
}

// ---- 进入时弹窗提醒（老用户被拒后再次申请）----
onMounted(async () => {
  if (route.query.toast === 'rejectRemain') {
    const remain = Number(route.query.remain) || 1
    showToast(`存在审批人拒绝情况，申请次数剩余：${remain} 次`)
  }
  if (editApplicationId.value) {
    await loadEditData()
  }
})
</script>

<template>
  <div class="page">
    <h2 class="page-title">{{ editApplicationId ? '修改来访预约填报' : '来访预约填报' }}</h2>

    <div class="page-card">
      <!-- 被访问人：只读，点击弹窗选择 -->
      <van-field :model-value="form.hostName" label="被访问人" placeholder="点击选择被访问人" readonly is-link required
        @click="showHostPopup = true" />

      <!-- 开始日期 -->
      <van-field :model-value="form.startTime" label="开始日期" placeholder="请选择开始日期" readonly is-link required
        @click="openDatePicker('start')" />

      <!-- 结束日期 -->
      <van-field :model-value="form.endTime" label="结束日期" placeholder="请选择结束日期" readonly is-link required
        @click="openDatePicker('end')" />

      <!-- 访问事由 -->
      <van-field v-model="form.reason" label="访问事由" type="textarea" rows="3" maxlength="200" show-word-limit
        placeholder="请填写访问事由（200字以内）" required />

      <!-- 备注（可选，≤200 字） -->
      <van-field v-model="form.remark" label="备注" type="textarea" rows="2" maxlength="200" show-word-limit
        placeholder="请填写备注（选填，200字以内）" />
    </div>

    <!-- 随行人员（可选，最多 5 人；已添加的行须完整且不重复才可提交） -->
    <div class="page-card">
      <div class="field-label">随行人员（可选，最多 5 人）<span class="companion-count">{{ companions.length }}/5</span></div>
      <div v-for="(c, i) in companions" :key="i" class="companion-row">
        <div class="companion-fields">
          <van-field v-model="c.name" label="姓名" maxlength="20" placeholder="随行人员姓名"
            :error-message="c.name.trim() ? '' : '请输入姓名'" />
          <van-field v-model="c.idCard" label="身份证号" maxlength="18" placeholder="18位身份证号"
            :formatter="idCardFormatter" :error-message="idCardError(i)" />
        </div>
        <van-icon name="delete-o" class="companion-delete" @click="removeCompanion(i)" />
      </div>
      <van-button v-if="companions.length < 5" block plain type="primary" size="small" icon="plus"
        class="companion-add" @click="addCompanion">
        添加随行人员
      </van-button>
    </div>

    <van-button type="primary" block round :disabled="!canSubmit" :loading="submitting" @click="onSubmit">
      {{ editApplicationId ? '提交修改' : '提交预约' }}
    </van-button>

    <!-- 被访人查询弹窗 -->
    <van-popup v-model:show="showHostPopup" position="bottom" round :style="{ height: '80%' }">
      <div class="host-popup">
        <h3 class="popup-title">选择被访人</h3>

        <van-row gutter="12" align="center" class="host-search-row">
          <van-col span="18">
            <van-search v-model="hostKeyword" placeholder="请输入被访人姓名" @search="onHostSearch" shape="round" />
          </van-col>
          <van-col span="6" class="search-btn-col">
            <van-button block type="primary" size="small" :loading="hostLoading" @click="onHostSearch">
              搜索
            </van-button>
          </van-col>
        </van-row>


        <!-- <div class="host-search">
          <van-search v-model="hostKeyword" placeholder="请输入被访人姓名" @search="onHostSearch" />
          <van-button size="normal" type="primary" :loading="hostLoading" @click="onHostSearch">
            搜索
          </van-button>
        </div> -->

        <div class="host-list">
          <van-empty v-if="hostSearched && !hostList.length" description=" " />
          <van-radio-group v-model="selectedHostId">
            <van-cell-group inset>
              <van-cell v-for="host in hostList" :key="host.userId" clickable @click="selectedHostId = host.userId">
                <template #title>
                  <van-radio :name="host.userId">{{ host.name }}</van-radio>
                </template>
                <template #label>{{ host.deptName || '—' }}</template>
              </van-cell>
            </van-cell-group>
          </van-radio-group>
        </div>

        <div class="popup-footer">
          <van-button block round type="primary" @click="onHostConfirm">确认</van-button>
        </div>
      </div>
    </van-popup>

    <!-- 日期选择弹窗（仅年月日，PRD v1.10：以日期为最小单位） -->
    <van-popup v-model:show="showDatePicker" position="bottom" round>
      <van-date-picker v-model="dateValue" :min-date="minDate" :max-date="maxDate" title="选择日期"
        @confirm="onDateConfirm" @cancel="showDatePicker = false" />
    </van-popup>

    <!-- 登记成功弹层（PRD §5.4） -->
    <van-popup v-model:show="showSuccess" round :style="{ width: '80%' }">
      <div class="success-popup">
        <van-icon name="checked" size="56px" color="#07c160" />
        <h3 class="success-title">{{ editApplicationId ? '预约修改成功' : '预约成功' }}</h3>
        <p class="success-tip">预约已提交，请等待被访问人审核</p>
        <van-button block round type="primary" @click="onSuccessClose">关闭</van-button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.host-popup {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;
}

.popup-title {
  text-align: center;
  margin-bottom: 12px;
  color: var(--color-title);
}

.host-search {
  display: flex;
  align-items: center;
  gap: 8px;
}

.host-list {
  flex: 1;
  overflow-y: auto;
}

.popup-footer {
  padding-top: 12px;
}

.companion-count {
  margin-left: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.companion-row {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  margin-bottom: 12px;
}

.companion-fields {
  flex: 1;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 8px;
  background: #fafafa;
}

.companion-fields :deep(.van-field) {
  padding: 8px 0;
  background: transparent;
}

.companion-delete {
  padding: 20px 8px 0;
  color: var(--color-text-secondary);
}

.companion-add {
  margin-top: 4px;
}

.success-popup {
  padding: 32px 24px;
  text-align: center;
}

.success-title {
  margin: 12px 0 8px;
  color: var(--color-text);
}

.success-tip {
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}
</style>
