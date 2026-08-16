<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useVisitorStore } from '@/stores/visitor'
import { searchHost, submitApplication } from '@/api/visitor'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'ApplicationView' })

// 申请单页（PRD §5.3）+ 登记成功弹层（PRD §5.4）
const route = useRoute()
const router = useRouter()
const store = useVisitorStore()

const form = reactive({ hostId: null, hostName: '', startTime: '', endTime: '', reason: '' })

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

// ---- 日期时间选择（PickerGroup：日期 + 时间两个滚轮 tab，参照参考图 5.jpg）----
const now = new Date()
const minDate = new Date(now.getFullYear(), now.getMonth(), now.getDate())
const maxDate = new Date(minDate.getFullYear() + 1, minDate.getMonth(), minDate.getDate())
const nowStr = formatDateTime(new Date()) // 本地时间 yyyy-MM-dd HH:mm（勿用 toISOString，其返回 UTC 时间）

const showTimePicker = ref(false)
const timePickerType = ref('start')
const dateValue = ref(nowStr.split(' ')[0].split('-').map(Number)) // [y, m, d]
const timeValue = ref(nowStr.split(' ')[1].split(':').map(Number)) // [hh, mm]

function openTimePicker(type) {
  timePickerType.value = type
  // 打开时回填当前已选值或默认值
  const current = form[type === 'start' ? 'startTime' : 'endTime'] || nowStr
  const [date, time] = current.split(' ')
  dateValue.value = date.split('-').map(Number)
  timeValue.value = time.split(':').map(Number)
  showTimePicker.value = true
}

// PickerGroup confirm 参数：各子 Picker 的 confirm 结果数组（[日期结果, 时间结果]）
function onTimeConfirm(items) {
  const [y, m, d] = items[0].selectedValues
  const [hh, mm] = items[1].selectedValues
  const date = new Date(y, m - 1, d, hh, mm)
  if (timePickerType.value === 'start') {
    // 开始时间不能早于当前日期（PRD §5.3）
    if (date < minDate) {
      showToast('开始时间不能早于当前日期')
      return
    }
    form.startTime = formatDateTime(date)
    // 结束时间已选且不晚于新开始时间时清空
    if (form.endTime && form.endTime <= form.startTime) {
      form.endTime = ''
    }
  } else {
    // 结束时间必须晚于开始时间
    if (!form.startTime) {
      showToast('请先选择开始时间')
      showTimePicker.value = false
      return
    }
    if (date <= new Date(form.startTime.replace(' ', 'T'))) {
      showToast('结束时间必须晚于开始时间')
      return
    }
    form.endTime = formatDateTime(date)
  }
  showTimePicker.value = false
}

// ---- 提交 ----
const submitting = ref(false)
const showSuccess = ref(false)

const canSubmit = computed(
  () => form.hostId && form.startTime && form.endTime && form.reason.trim() && companionsValid.value,
)

async function onSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    await submitApplication({
      visitorId: store.visitorId,
      hostId: form.hostId,
      startTime: `${form.startTime}:00`,
      endTime: `${form.endTime}:00`,
      reason: form.reason.trim(),
      companions: companions.value.map((c) => ({ name: c.name.trim(), idCard: c.idCard.trim() })),
    })
    showSuccess.value = true
  } catch {
    // 拦截器已提示（如当日拒绝数达 3 次被拦截）
  } finally {
    submitting.value = false
  }
}

function onSuccessClose() {
  showSuccess.value = false
  router.replace('/list')
}

// ---- 进入时弹窗提醒（老用户被拒后再次申请）----
onMounted(() => {
  if (route.query.toast === 'rejectRemain') {
    const remain = Number(route.query.remain) || 1
    showToast(`存在审批人拒绝情况，申请次数剩余：${remain} 次`)
  }
})
</script>

<template>
  <div class="page">
    <h2 class="page-title">访客申请单</h2>

    <div class="page-card">
      <!-- 被访问人：只读，点击弹窗选择 -->
      <van-field :model-value="form.hostName" label="被访问人" placeholder="点击选择被访问人" readonly is-link required
        @click="showHostPopup = true" />

      <!-- 开始时间 -->
      <van-field :model-value="form.startTime" label="开始时间" placeholder="请选择开始时间" readonly is-link required
        @click="openTimePicker('start')" />

      <!-- 结束时间 -->
      <van-field :model-value="form.endTime" label="结束时间" placeholder="请选择结束时间" readonly is-link required
        @click="openTimePicker('end')" />

      <!-- 访问事由 -->
      <van-field v-model="form.reason" label="访问事由" type="textarea" rows="3" maxlength="200" show-word-limit
        placeholder="请填写访问事由（200字以内）" required />
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
      提交申请
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

    <!-- 日期时间选择弹窗（PickerGroup：日期 tab + 时间 tab） -->
    <van-popup v-model:show="showTimePicker" position="bottom" round>
      <van-picker-group title="选择时间" :tabs="['选择日期', '选择时间']" next-step-text="下一步"
        @confirm="onTimeConfirm" @cancel="showTimePicker = false">
        <van-date-picker v-model="dateValue" :min-date="minDate" :max-date="maxDate" />
        <van-time-picker v-model="timeValue" />
      </van-picker-group>
    </van-popup>

    <!-- 登记成功弹层（PRD §5.4） -->
    <van-popup v-model:show="showSuccess" round :style="{ width: '80%' }">
      <div class="success-popup">
        <van-icon name="checked" size="56px" color="#07c160" />
        <h3 class="success-title">登记成功</h3>
        <p class="success-tip">请耐心等待被访人的确认</p>
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
