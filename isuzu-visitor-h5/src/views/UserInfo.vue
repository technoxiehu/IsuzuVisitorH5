<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useVisitorStore } from '@/stores/visitor'
import { registerUser, updateUser, uploadAvatar } from '@/api/visitor'
import { toAvatarUrl } from '@/utils/avatar'

defineOptions({ name: 'UserInfoView' })

// 用户信息页（新用户注册）/ 我的信息页（老用户修改），PRD §5.2、§5.6
// 通过 query.mode=edit 区分：edit 为我的信息页，回显并提交更新
const route = useRoute()
const router = useRouter()
const store = useVisitorStore()

const isEdit = ref(false)
const form = reactive({ name: '', phone: '', idCard: '', company: '', plateNo: '', avatar: '' })
const errors = reactive({ name: '', phone: '', idCard: '', company: '', plateNo: '', avatar: '' })
// 编辑模式原始值快照（保存前未修改检测的基准，Application.vue 同款逻辑）
const original = reactive({ name: '', phone: '', idCard: '', company: '', plateNo: '', avatar: '' })
const fileList = ref([])
const submitting = ref(false)
// 无车进场（勾选后车牌非必填、输入禁用，提交空字符串；未勾选则车牌必填）
const noCar = ref(false)

// 18 位身份证格式（末位可 X，与 Application.vue 随行人员校验一致）
const ID_CARD_RE = /^\d{17}[\dX]$/
// 输入过滤：仅数字与 X（自动转大写），截断 18 位
const idCardFormatter = (v) => v.replace(/[^\dXx]/g, '').slice(0, 18).toUpperCase()
// 车牌号非必填：留空通过；填写仅限汉字/字母/数字，≤10 位（宽松校验，兼容新能源等）
const PLATE_RE = /^[一-龥A-Za-z0-9]{0,10}$/
// 输入过滤：仅汉字/字母/数字（自动转大写），截断 10 位
const plateFormatter = (v) => v.replace(/[^一-龥A-Za-z0-9]/g, '').slice(0, 10).toUpperCase()

onMounted(async () => {
  // 等待路由解析完成后再读取 query 与回显
  await router.isReady()
  isEdit.value = route.query.mode === 'edit'
  if (isEdit.value && store.userInfo) {
    form.name = store.userInfo.name || ''
    form.phone = store.userInfo.phone || ''
    form.idCard = store.userInfo.idCard || '' // 后端返回全量值，本人设备回显
    form.company = store.userInfo.company || ''
    form.plateNo = store.userInfo.plateNo || '' // 非必填，老用户无则留空
    form.avatar = store.userInfo.avatar || ''
    if (form.avatar) {
      fileList.value = [{ url: toAvatarUrl(form.avatar) }]
    }
    // 原车牌为空 → 默认勾选"无车进场"（存量用户 plateNo 为 null 兼容）
    noCar.value = !form.plateNo
    // 快照原始值（保存前未修改检测的基准）
    original.name = form.name
    original.phone = form.phone
    original.idCard = form.idCard
    original.company = form.company
    original.plateNo = form.plateNo
    original.avatar = form.avatar
  }
})

/** 切换"无车进场"：清除车牌遗留错误提示（勾选态不再校验车牌） */
function onToggleNoCar() {
  noCar.value = !noCar.value
  errors.plateNo = ''
}

/** 头像上传（PRD §5.2：拍照/相册，上传提示参照参考图） */
async function onAvatarRead(file) {
  try {
    const res = await uploadAvatar(file.file)
    form.avatar = res.data.url
    errors.avatar = ''
    fileList.value = [{ url: form.avatar }]
  } catch {
    // 失败提示由拦截器 toast 负责；保留本地预览供删除后重拍，不整表清空
    if (form.avatar) {
      // 已有历史头像：恢复显示历史头像，避免预览与提交值不一致
      fileList.value = [{ url: toAvatarUrl(form.avatar) }]
    } else {
      // 无历史头像：保留 Vant 本地预览（blob），标记失败
      fileList.value = [{ ...file, status: 'failed', message: '上传失败' }]
    }
  }
}

function validate() {
  errors.name = form.name.trim() ? (form.name.length > 20 ? '姓名长度不能超过20个字符' : '') : '请输入姓名'
  errors.phone = /^1[3-9]\d{9}$/.test(form.phone) ? '' : '请输入正确的11位手机号'
  // 全量回显，须为合法 18 位身份证号
  errors.idCard = ID_CARD_RE.test(form.idCard) ? '' : '请输入正确的18位身份证号'
  errors.company = form.company.trim() ? '' : '请输入单位'
  // 车牌号：勾选"无车进场"跳过校验；否则必填（要么填车牌，要么显式勾选无车）
  if (noCar.value) {
    errors.plateNo = ''
  } else if (!form.plateNo.trim()) {
    errors.plateNo = '请输入车牌号，无车请勾选"无车进场"'
  } else if (!PLATE_RE.test(form.plateNo)) {
    errors.plateNo = '车牌号格式不正确'
  }
  errors.avatar = form.avatar ? '' : '请上传头像照片'
  return (
    !errors.name &&
    !errors.phone &&
    !errors.idCard &&
    !errors.company &&
    !errors.plateNo &&
    !errors.avatar
  )
}

// 保存前检查：是否真实修改了内容（按提交口径 trim 归一化比较，Application.vue 同款逻辑）
// 车牌按提交口径比较：勾选无车提交空串，取消勾选后原值恢复为已填写
function plateNoForSubmit() {
  return noCar.value ? '' : form.plateNo.trim()
}

function isModified() {
  return (
    form.name.trim() !== original.name.trim() ||
    form.phone !== original.phone ||
    form.idCard !== original.idCard ||
    form.company.trim() !== original.company.trim() ||
    plateNoForSubmit() !== original.plateNo.trim() ||
    form.avatar !== original.avatar
  )
}

async function onSubmit() {
  if (!validate()) return
  // 编辑模式：未实际修改任何内容时不走后台修改流程，提示后直接返回列表
  if (isEdit.value && !isModified()) {
    showToast('报告，保持原装！😎')
    router.replace('/list')
    return
  }
  submitting.value = true
  const data = {
    visitorId: store.visitorId,
    name: form.name.trim(),
    phone: form.phone,
    idCard: form.idCard, // 脱敏值也原样提交，由后端守卫决定是否覆盖
    company: form.company.trim(),
    plateNo: plateNoForSubmit(), // 勾选无车提交空字符串（后端清空原列），否则提交填写值
    avatar: form.avatar,
  }
  try {
    if (isEdit.value) {
      await updateUser(data)
      store.setUserInfo({ ...data })
      router.replace('/list')
    } else {
      await registerUser(data)
      router.replace('/application')
    }
  } catch {
    // 拦截器已提示（如「该用户已注册」），保留已填内容支持重试
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <h2 class="page-title">{{ isEdit ? '我的信息' : '欢迎您来到五十铃发动机厂' }}</h2>
    <p v-if="!isEdit" class="page-subtitle">请填写个人信息</p>

    <div class="page-card">
      <!-- 头像 -->
      <div class="avatar-row">
        <span class="field-label"><span class="required">*</span>头像照片</span>
        <van-uploader
          v-model="fileList"
          :after-read="onAvatarRead"
          :max-count="1"
          accept="image/*"
          capture="user"
        />
      </div>
      <p class="field-tip">照片清晰、头像完整、尽量白底</p>
      <p v-if="errors.avatar" class="field-error">{{ errors.avatar }}</p>

      <!-- 姓名 -->
      <van-field
        v-model="form.name"
        label="姓名"
        placeholder="请输入姓名"
        maxlength="20"
        :error-message="errors.name"
        required
      />

      <!-- 手机号 -->
      <van-field
        v-model="form.phone"
        type="tel"
        label="手机号"
        placeholder="请输入11位手机号"
        maxlength="11"
        :error-message="errors.phone"
        required
      />

      <!-- 身份证号 -->
      <van-field
        v-model="form.idCard"
        label="身份证号"
        placeholder="请输入18位身份证号"
        maxlength="18"
        :formatter="idCardFormatter"
        :error-message="errors.idCard"
        required
      />

      <!-- 单位 -->
      <van-field
        v-model="form.company"
        label="单位"
        placeholder="请输入单位"
        :error-message="errors.company"
        required
      />

      <!-- 车牌号（要么填写车牌，要么勾选"无车进场"；勾选后禁用输入并提交空值） -->
      <van-field
        v-model="form.plateNo"
        label="车牌号"
        placeholder="请输入车牌号"
        maxlength="10"
        :formatter="plateFormatter"
        :error-message="errors.plateNo"
        :disabled="noCar"
        :required="!noCar"
      />
      <div class="no-car-row" @click="onToggleNoCar">
        <van-checkbox v-model="noCar" shape="square" @click.stop />
        <span class="no-car-label">无车进场（步行/打车/公交等）</span>
      </div>
      <p v-if="noCar" class="no-car-tip">未填车牌无法驾车进场</p>
    </div>

    <van-button type="primary" block round :loading="submitting" @click="onSubmit">
      {{ isEdit ? '保存修改' : '预约' }}
    </van-button>
  </div>
</template>

<style scoped>
.page-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
  text-align: center;
  margin: -12px 0 16px;
}

.avatar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.field-tip {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.field-error {
  font-size: 12px;
  color: #ee0a24;
  margin-bottom: 8px;
}

/* 无车进场复选框行（整行可点，checkbox 本体阻止冒泡由 v-model 接管） */
.no-car-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
}

.no-car-label {
  font-size: 13px;
  color: var(--color-text);
}

/* 勾选后的后果提示（警示橙，区别于普通说明文字） */
.no-car-tip {
  font-size: 12px;
  color: #ff976a;
  padding: 0 16px;
  margin-bottom: 8px;
}
</style>
