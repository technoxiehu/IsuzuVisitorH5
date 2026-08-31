/**
 * 微信客户端环境判定
 *
 * - 微信内置浏览器：UA 含 MicroMessenger
 * - 微信开发者工具：UA 含 wechatdevtools，一并视为微信环境，便于工具调试
 * - UA 可被伪造，本判定仅做防呆引导，不承担安全职责
 */
export function isWeChat() {
  const ua = navigator.userAgent
  return /MicroMessenger/i.test(ua) || /wechatdevtools/i.test(ua)
}
