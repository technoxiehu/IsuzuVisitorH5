import request from '@/utils/request'

// 门卫有效单据卡片（卡片墙数据源，接口契约 §3.13，需登录）
export function listGuardCard(query) {
  return request({
    url: '/visitor/guard/list',
    method: 'get',
    params: query
  })
}

// 门卫进出登记（新增进场/离厂事件，接口契约 §3.14，需登录；data.entryType：'0' 进场 / '1' 离厂）
export function createGuardEntry(data) {
  return request({
    url: '/visitor/guard/entry',
    method: 'post',
    data: data
  })
}

// 入场记录查询（接口契约 §3.15，需登录）
export function listGuardEntry(query) {
  return request({
    url: '/visitor/guard/entry/list',
    method: 'get',
    params: query
  })
}
