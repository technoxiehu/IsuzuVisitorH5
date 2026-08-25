<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
const form = reactive({ name: '', phone: '', idCard: '', company: '', avatar: '' })
const errors = reactive({ name: '', phone: '', idCard: '', company: '', avatar: '' })
const fileList = ref([])
const submitting = ref(false)

// 18 位身份证格式（末位可 X，与 Application.vue 随行人员校验一致）
const ID_CARD_RE = /^\d{17}[\dX]$/
// 输入过滤：仅数字与 X（自动转大写），截断 18 位
const idCardFormatter = (v) => v.replace(/[^\dXx]/g, '').slice(0, 18).toUpperCase()

onMounted(async () => {
  // 等待路由解析完成后再读取 query 与回显
  await router.isReady()
  isEdit.value = route.query.mode === 'edit'
  if (isEdit.value && store.userInfo) {
    form.name = store.userInfo.name || ''
    form.phone = store.userInfo.phone || ''
    form.idCard = store.userInfo.idCard || '' // 后端返回全量值，本人设备回显
    form.company = store.userInfo.company || ''
    form.avatar = store.userInfo.avatar || ''
    if (form.avatar) {
      fileList.value = [{ url: toAvatarUrl(form.avatar) }]
    }
  }
})

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
  errors.avatar = form.avatar ? '' : '请上传头像照片'
  return !errors.name && !errors.phone && !errors.idCard && !errors.company && !errors.avatar
}

async function onSubmit() {
  if (!validate()) return
  submitting.value = true
  const data = {
    visitorId: store.visitorId,
    name: form.name.trim(),
    phone: form.phone,
    idCard: form.idCard, // 脱敏值也原样提交，由后端守卫决定是否覆盖
    company: form.company.trim(),
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
    <h2 class="page-title">{{ isEdit ? '我的信息' : '用户信息' }}</h2>

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
        type="text"
        inputmode="numeric"
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
    </div>

    <van-button type="primary" block round :loading="submitting" @click="onSubmit">
      {{ isEdit ? '保存修改' : '提交' }}
    </van-button>
  </div>
</template>

<style scoped>
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
</style>
