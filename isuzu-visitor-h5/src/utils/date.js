// 日期格式化（接口时间格式 yyyy-MM-dd HH:mm:ss，docs/03_接口契约.md §1.2）
export function formatDateTime(date, withSeconds = false) {
  const p = (n) => String(n).padStart(2, '0')
  const base = `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())} ${p(date.getHours())}:${p(date.getMinutes())}`
  return withSeconds ? `${base}:${p(date.getSeconds())}` : base
}
