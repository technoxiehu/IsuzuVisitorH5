<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" class="entry-query">
      <el-form-item label="关键字" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="访客姓名 / 手机号 / 门卫姓名"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="放行时间" prop="dateRange">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="entryList" stripe>
      <el-table-column label="序号" width="64" align="center" prop="index">
        <template #default="scope">{{ (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1 }}</template>
      </el-table-column>
      <el-table-column label="访客姓名" prop="visitorName" min-width="100" />
      <el-table-column label="单位" prop="visitorCompany" min-width="140" show-overflow-tooltip />
      <el-table-column label="手机号" prop="visitorPhone" min-width="120" />
      <el-table-column label="身份证号" prop="visitorIdCard" min-width="160" />
      <el-table-column label="车牌号" prop="plateNo" min-width="110">
        <template #default="scope">{{ scope.row.plateNo || '-' }}</template>
      </el-table-column>
      <el-table-column label="被访人" prop="hostName" min-width="100" />
      <el-table-column label="访问时间" min-width="190">
        <template #default="scope">
          {{ formatRange(scope.row.startTime, scope.row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column label="放行门卫" prop="operatorName" min-width="100" />
      <el-table-column label="放行时间" prop="entryTime" min-width="160" />
    </el-table>

    <pagination
      v-show="total > 0"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="GuardEntry">
import { listGuardEntry } from '@/api/visitor/guard'

const { proxy } = getCurrentInstance()

const entryList = ref([])
const loading = ref(false)
const total = ref(0)
const dateRange = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: undefined,
  beginTime: undefined,
  endTime: undefined
})

/** 访问时间范围展示：同日显示「MM-DD HH:mm ~ HH:mm」，跨日显示完整 */
function formatRange(start, end) {
  if (!start && !end) return '-'
  const s = start ? start.slice(0, 16) : ''
  const e = end ? end.slice(0, 16) : ''
  if (s && e && s.slice(0, 10) === e.slice(0, 10)) {
    return `${s.slice(5)} ~ ${e.slice(11)}`
  }
  return `${s} ~ ${e}`
}

function getList() {
  loading.value = true
  listGuardEntry(queryParams)
    .then((res) => {
      entryList.value = res.rows
      total.value = Number(res.total)
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  if (dateRange.value && dateRange.value.length === 2) {
    queryParams.beginTime = dateRange.value[0]
    queryParams.endTime = dateRange.value[1]
  } else {
    queryParams.beginTime = undefined
    queryParams.endTime = undefined
  }
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  proxy.resetForm('queryRef')
  queryParams.beginTime = undefined
  queryParams.endTime = undefined
  handleQuery()
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.entry-query {
  padding: 0;
}
</style>
