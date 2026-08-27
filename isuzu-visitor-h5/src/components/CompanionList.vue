<script setup>
import { maskIdCard } from '@/utils/mask'

defineOptions({ name: 'CompanionList' })

// 随行人员名单展示（PRD v1.4/v1.10，列表页/审批页共用）：
// 固定显示「随行人员」标题区块，有名单逐行展示（姓名+脱敏身份证），无名单展示空态
defineProps({
  companions: { type: Array, default: () => [] }, // [{ name, idCard }]
})
</script>

<template>
  <div class="companion-list">
    <div class="companion-title">随行人员</div>
    <template v-if="companions.length">
      <div v-for="(c, i) in companions" :key="i" class="companion-item">
        <span class="companion-name">{{ c.name }}</span>
        <span class="companion-id">{{ maskIdCard(c.idCard) }}</span>
      </div>
    </template>
    <div v-else class="companion-empty">无随行人员</div>
  </div>
</template>

<style scoped>
.companion-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.companion-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
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

.companion-empty {
  font-size: 13px;
  color: var(--color-text-secondary);
}
</style>
