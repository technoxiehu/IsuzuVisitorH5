/**
 * 身份证脱敏：保留前 3 位与后 4 位，中间 11 位替换为 *，如 110***********1234（末位 X 保留）。
 * 规则与后端 IdCardUtils.mask 保持一致（docs/03_接口契约.md §3.6）。
 */
export function maskIdCard(id) {
  if (!id) return ''
  const s = String(id)
  // 幂等：对已脱敏值重复调用结果不变（含 * 不匹配完整格式）
  if (/^\d{17}[\dX]$/i.test(s)) return s.replace(/^(\d{3})\d{11}(\d{4})$/i, '$1***********$2')
  return s
}
