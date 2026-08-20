# 编码规范详细版

> 本文档从 CLAUDE.md 拆分而来,主文件 CLAUDE.md 第 4 节只保留高频约定(新增页面、自动导入、按钮权限、axios、字典等)。
> 涉及**样式、表单校验、状态管理、路由、性能、文件权限**时阅读本文档。

## 1. 样式规范

- 使用 Vue 单文件组件的 `<style scoped>`，**禁止全局样式污染**
- Element Plus 组件样式覆盖需用 `:deep()` 或写在全局样式文件中
- 颜色、间距等尽量用 Element Plus 的 CSS 变量，**禁止硬编码颜色值**(除非设计稿明确要求)
- 禁止大量使用内联样式，复杂样式写在 `<style>` 中

## 2. 表单校验

- 使用 **Element Plus 表单校验**(`el-form` + `rules`)
- 校验规则写在组件内的 `rules` 对象中，禁止散落在各处
- 常用校验：`required` / `max` / `min` / `pattern` / 自定义 validator
- 提交前调用 `formRef.validate()` 校验通过后再发请求

## 3. 其他约定

- **token 存 cookie**(key=`Admin-Token`，基于 `js-cookie`)，不是 localStorage
- **登录密码 RSA 加密**(`src/utils/jsencrypt.js`，内置密钥对)，不要明文传输
- **404 兜底路由**：`/:pathMatch(.*)*`，匹配不到的路由走 404 页面
- 动态路由数据来自后端 `/getRouters`(`src/api/menu.js`)
- 布局有三种模式，由 `settings.js` 的 `navType` 控制

## 4. 状态管理

- 页面级状态用组件本地 `ref` / `reactive`
- 全局状态(用户信息、权限、布局配置等)用 Pinia
- **禁止把临时数据塞进全局状态**，store 只存跨页面共享的数据
- 7 个 store 模块已固定，新增模块需在 ADR 中说明理由

## 5. 路由规范

- 路由路径全小写，多单词用横杠，如 `/system/user-management`
- 业务页面统一放 `src/views/` 目录(不是 `src/pages/`)
- 有菜单的页面**不要手动加路由**，由后端菜单表动态生成
- 隐藏详情页才在 `src/router/index.js` 的 `dynamicRoutes` 中注册

## 6. 性能约束

- 列表必须加分页(后端已分页，前端用 `Pagination` 组件)
- 禁止一次性渲染 100 条以上数据(表格用分页，长列表用虚拟滚动)
- 图片必须懒加载(`el-image` 默认支持)
- 大图标用 SVG 或雪碧图

---

## 7. 文件编辑权限矩阵

### ✅ 可手工修改

- `openspec/changes/**/proposal.md`
- `openspec/changes/**/specs/*.md`(增量 spec)
- `docs/adr/*.md`(架构决策记录)
- `CONTEXT.md`(业务术语)
- `FEATURES.md`(需求总索引)
- `src/views/` 下的业务页面
- `src/api/` 下的接口封装
- `src/components/` 下的公共组件

### ❌ 禁止手工修改

- `openspec/specs/**`(基准 spec 库，只能通过 `/opsx:archive` 合并)
- `openspec/changes/**/design.md`(OpenSpec 内部草稿，matt 不读取)
- `openspec/changes/**/tasks.md`(OpenSpec 内部任务，matt 不读取)
- `src/utils/request.js`(axios 核心封装，修改需 ADR 评审)
- `src/permission.js`(路由守卫核心，修改需 ADR 评审)

---

## 8. 关键依赖版本

| 依赖 | 版本 |
|------|------|
| Vue | 3.5.x |
| Vite | 6.x |
| Element Plus | 2.13.x |
| Pinia | 3.x |
| Vue Router | 4.x |
| Axios | 1.x |
| js-cookie | 3.x |
| unplugin-auto-import | 最新 |
| unplugin-vue-setup-extend-plus | 最新 |
