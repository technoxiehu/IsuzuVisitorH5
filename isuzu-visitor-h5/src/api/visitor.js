import request from '@/utils/request'

// 访客登记系统接口（docs/03_接口契约.md §3）

/** §3.1 用户信息查询（老用户判断，查无时 data.user=null） */
export function getUser(visitorId) {
  return request.get('/visitor/user', { params: { visitorId } })
}

/** §3.2 新用户注册 */
export function registerUser(data) {
  return request.post('/visitor/user', data)
}

/** §3.3 用户信息更新（我的信息页） */
export function updateUser(data) {
  return request.put('/visitor/user', data)
}

/** §3.4 头像上传（匿名） */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/visitor/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** §3.5 被访人查询 */
export function searchHost(keyword) {
  return request.get('/visitor/host/search', { params: { keyword } })
}

/** §3.6 申请单提交 */
export function submitApplication(data) {
  return request.post('/visitor/application', data)
}

/** §3.7 审批记录查询（有效记录 + 当日拒绝数） */
export function getApplicationList(visitorId) {
  return request.get('/visitor/application/list', { params: { visitorId } })
}

/** §3.8 审批详情查询（token 鉴权） */
export function getApproveDetail(token) {
  return request.get('/visitor/approve/detail', { params: { token } })
}

/** §3.9 审批结果回写 */
export function approve(token, action) {
  return request.post('/visitor/approve', { token, action })
}
