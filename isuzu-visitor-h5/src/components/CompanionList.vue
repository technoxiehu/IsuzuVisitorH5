<script setup>
import { maskIdCard } from '@/utils/mask'

defineOptions({ name: 'CompanionList' })

// 随行人员名单展示（姓名 + 脱敏身份证号，列表页/审批页共用，PRD v1.4）
defineProps({
  companions: { type: Array, default: () => [] }, // [{ name, idCard }]
})
</script>

<template>
  <div class="companion-list">
    <div v-for="(c, i) in companions" :key="i" class="companion-item">
      <span class="companion-name">{{ c.name }}</span>
      <span class="companion-id">{{ maskIdCard(c.idCard) }}</span>
    </div>
  </div>
</template>

<style scoped>
.companion-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.companion-item {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
}

.companion-name {
  color: var(--color-text);
}

.companion-id {
  color: var(--color-text-secondary);
  font-variant-numeric: tabular-nums;
}
</style>
