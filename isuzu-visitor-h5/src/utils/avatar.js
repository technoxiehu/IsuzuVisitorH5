/** 头像 URL 归一化：绝对 URL 取 pathname，相对路径原样返回（兼容历史绝对 URL 数据） */
export function toAvatarUrl(u) {
  if (!u) return ''
  if (/^https?:\/\//i.test(u)) {
    try {
      return new URL(u).pathname
    } catch {
      return u
    }
  }
  return u
}
