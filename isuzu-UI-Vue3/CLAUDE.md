# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 1. 项目概览

**若依 RuoYi-Vue3 管理后台前端** —— 基于 Vue 3 + Vite + Element Plus 的后台管理系统前端。

- **技术栈**：Vue 3.5 + Vite 6 + Element Plus 2.13 + Pinia 3 + Vue Router 4 + Axios
- **语言**：**纯 JavaScript(无 TypeScript)**,不要假定存在 `.ts` 文件或类型定义
- **仓库边界**：**本仓库仅包含前端代码**；后端为独立的 RuoYi-Vue Java 项目，不在此仓库
- **后端接口**：默认指向 `http://localhost:8080`(通过 vite 代理转发)
- **UI 框架**：Element Plus **全量引入**(非按需)，带 zh-cn 中文 locale

---

## 2. 构建与运行

### 前置依赖

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| Node.js | 16+ | 推荐 18 LTS |
| 包管理器 | npm | 项目使用 npm(仓库存在 package-lock.json),不要用 yarn/pnpm |

### 常用命令

```bash
# 安装依赖(推荐使用国内镜像)
npm install --registry=https://registry.npmmirror.com

# 开发服务器(端口 80，自动打开浏览器；由 vite.config.js 的 server.port / server.open 控制)
npm run dev

# 生产环境构建
npm run build:prod

# 预发布环境构建(--mode staging)
npm run build:stage

# 本地预览构建产物
npm run preview
```

### ⚠️ 重要提醒

- **本项目没有 ESLint / Prettier / lint / test 脚本**，不要假定存在，不要尝试运行 `npm run lint` 或 `npm test`
- 没有单元测试框架，新增代码无需写测试
- 代码风格遵循现有文件的写法(2 空格缩进、单引号、无分号)

### 环境变量与代理

| 文件 | 用途 | 关键变量 |
|------|------|----------|
| `.env.development` | 开发环境 | `VITE_APP_BASE_API=/dev-api` |
| `.env.production` | 生产环境 | `VITE_APP_BASE_API=/prod-api` |
| `.env.staging` | 预发布环境 | `VITE_APP_BASE_API=/stage-api` |

通用变量：`VITE_APP_TITLE`(页面标题)、`VITE_APP_ENV`(环境标识)；`VITE_BUILD_COMPRESS`(gzip 压缩开关)仅 `.env.production` / `.env.staging` 使用，`.env.development` 未配置

**Vite 代理配置**(`vite.config.js`)：
- `/dev-api` → `http://localhost:8080`(rewrite 去掉 `/dev-api` 前缀转发)
- `^/v3/api-docs/(.*)` → SpringDoc 接口文档代理

---

## 3. 架构地图

### 目录结构

```
src/
├── api/              # 接口请求封装，按模块分文件(user.js / role.js / menu.js ...)
├── assets/           # 静态资源(图片、样式)
├── components/       # 全局公共组件(DictTag / Pagination / FileUpload ...)
├── directive/        # 自定义指令(permission / hasPermi / hasRole)
├── layout/           # 布局组件(Sidebar / TagsView / AppMain / Settings)
├── plugins/          # 插件($auth / $tab / $cache / $modal / $download)
├── router/           # 路由配置(静态路由 + dynamicRoutes 注册)
├── store/            # Pinia 状态管理(7 个模块)
├── utils/            # 工具函数(request.js / auth.js / permission.js ...)
├── views/            # 业务页面(按模块分子目录)
├── permission.js     # 路由守卫(副作用导入，注册全局守卫)
└── main.js           # 应用入口
```

### 入口链与权限闭环(核心架构)

```
index.html
  → src/main.js(挂载全局组件/方法/插件/指令)
    → src/permission.js(import './permission' 副作用注册路由守卫)
```

**路由守卫流程**：
1. 白名单：`/login`、`/register` 直接放行
2. 无 token → 跳登录页(带 `redirect` 参数)
3. 有 token 且 `roles.length === 0` → 执行初始化：
   - `user.getInfo()` 获取用户信息(填充 roles / permissions)
   - `permission.generateRoutes()` 生成动态路由
   - `router.addRoute()` 注入动态路由
   - `{ ...to, replace: true }` 重新导航

**动态路由生成**(核心逻辑在 `src/store/modules/permission.js`，不在 router 配置中)：
- 调 `src/api/menu.js` 的 `/getRouters` 接口拿菜单树
- `filterAsyncRouter` 将字符串 `component` 经 `import.meta.glob('./../../views/**/*.vue')` 映射到 views 组件
- 三个特殊字符串映射：`Layout` / `ParentView` / `InnerLink`
- `dynamicRoutes` 再经 `filterDynamicRoutes` 按 `permissions`/`roles` 过滤后 addRoute
- roles 为空数组时兜底为 `['ROLE_DEFAULT']`(该兜底位于 `src/store/modules/user.js` 的 `getInfo()` 中，不在 filterDynamicRoutes 内)

### Pinia Store 模块(7 个)

| 模块 | 路径 | 职责 |
|------|------|------|
| user | `src/store/modules/user.js` | 用户信息、token、权限、登录登出 |
| permission | `src/store/modules/permission.js` | 动态路由生成与过滤 |
| app | `src/store/modules/app.js` | 应用级状态(侧边栏折叠等) |
| settings | `src/store/modules/settings.js` | 布局配置(主题、导航模式等) |
| tagsView | `src/store/modules/tagsView.js` | 标签页管理 |
| dict | `src/store/modules/dict.js` | 字典数据缓存 |
| lock | `src/store/modules/lock.js` | 锁屏功能 |

### 核心文件定位(高频修改)

| 功能 | 文件路径 |
|------|----------|
| 应用入口 | `src/main.js` |
| 路由守卫 | `src/permission.js` |
| 路由配置 | `src/router/index.js`(动态路由生成在 `src/store/modules/permission.js`) |
| axios 封装 | `src/utils/request.js` |
| token 管理 | `src/utils/auth.js`(cookie，key=`Admin-Token`) |
| 权限工具 | `src/utils/permission.js` |
| 按钮权限指令 | `src/directive/permission/` |
| 字典工具 | `src/utils/dict.js` + `useDict` 组合式函数 |
| 布局配置 | `src/store/modules/settings.js` |

---

## 4. 核心编码约定

### 4.1 新增页面约定(最重要)

**有菜单的业务页面**：
- **只在 `src/views/` 对应目录建 `.vue` 文件**
- 菜单由**后端数据库菜单表**控制(`component` 字段填组件路径字符串，如 `system/user/index`)
- **无需改动前端 router 配置**，动态路由会自动加载

**无菜单的隐藏详情页**(如用户编辑、角色分配等弹窗/跳转页)：
- 在 `src/router/index.js` 的 `dynamicRoutes` 数组里注册
- 带 `permissions` 数组控制可见性，如 `permissions: ['system:user:edit']`

### 4.2 组件规范

- 使用 `<script setup>` 语法(组合式 API)
- 组件文件命名：**大驼峰**，如 `UserCard.vue`
- 一个组件一个文件，复杂组件拆分子组件放同目录 `components/` 子文件夹
- 支持 `<script setup name="xxx">` 扩展(unplugin-vue-setup-extend-plus)，用于设置组件名
- **禁止使用类组件**(Vue 3 已不推荐)

### 4.3 自动导入(无需显式 import)

`unplugin-auto-import` 已配置，以下 API 直接使用即可：
- Vue：`ref` / `reactive` / `computed` / `watch` / `onMounted` 等
- Vue Router：`useRoute` / `useRouter`
- Pinia：`defineStore` / `storeToRefs`
- 项目工具：`useDict` / `selectDictLabel`(`selectDictLabels` 未配置自动导入，需显式 import)

**不要重复 import 这些 API**，否则会报错或冗余。

### 4.4 路径别名

| 别名 | 指向 | 用途 |
|------|------|------|
| `@` | `src/` | 源码目录引用 |
| `~` | 项目根 | 根目录引用 |

示例：`import userApi from '@/api/user'`

### 4.5 按钮权限

三种方式，按需选用：

```vue
<!-- 1. 指令方式(最常用，不满足时直接删除 DOM 元素) -->
<el-button v-hasPermi="['system:user:add']">新增</el-button>
<el-button v-hasRole="['admin']">管理员操作</el-button>

<!-- 2. 函数方式(条件判断) -->
<el-button v-if="checkPermi(['system:user:edit'])">编辑</el-button>

<!-- 3. 插件方式(模板内) -->
<el-button v-if="$auth.hasPermi('system:user:remove')">删除</el-button>
```

- 权限字符串格式：`模块:功能:操作`，如 `system:user:list`
- 通配符 `*:*:*` 与 `admin` 角色自动放行
- 指令不满足时执行 `removeChild(el)` 直接删除元素

### 4.6 axios 封装与接口请求

**所有接口请求统一放在 `src/api/` 目录，按模块分文件**(如 `user.js` / `role.js` / `menu.js`)。

`src/utils/request.js` 封装要点：
- `baseURL` 取 `VITE_APP_BASE_API` 环境变量
- 默认带 `Authorization: Bearer token` 请求头；设置 `isToken: false` 可跳过
- GET 请求的 params 用 `tansParams` 序列化进 URL
- **防重复提交**：POST/PUT 请求，sessionStorage 存 url+data+时间指纹，1s 内重复拒绝；`repeatSubmit: false` 可关闭，>5M 数据自动跳过
- 响应统一解包 `res.data`
- 401 弹窗重新登录(`isRelogin` 防重复弹窗)
- 500 / 601 有特殊错误文案
- blob / arraybuffer 二进制响应直接返回(用于文件下载)
- 另导出 `download()` 方法用于文件下载

**接口定义规则**：
- ❗**所有接口字段、路径、错误码以后端仓 OpenSpec 为唯一事实源**，禁止自行发明
- 字段命名必须和后端保持一致(后端用下划线就用下划线，禁止前端自行转驼峰)
- 新增接口先确认后端 spec 已稳定，再写 API 封装

### 4.7 全局方法 / 组件 / 插件

**main.js 挂载到 `globalProperties` 的全局方法**(模板中直接用 `$xxx`，JS 中用 `getCurrentInstance().proxy.$xxx`)：

`parseTime` / `resetForm` / `addDateRange` / `handleTree` / `selectDictLabel` / `selectDictLabels` / `getConfigKey` / `download` / `useDict`(`tansParams` 未挂载全局，仅在 `src/utils/request.js` 内部使用)

**全局插件**(`src/plugins/`)：
- `$auth`：权限判断
- `$tab`：标签页操作
- `$cache`：缓存操作(sessionStorage/localStorage 封装)
- `$modal`：弹窗封装(`$modal.confirm` / `$modal.alert`)
- `$download`：文件下载

**全局组件**(无需 import 直接用)：
`DictTag` / `Pagination` / `FileUpload` / `ImageUpload` / `ImagePreview` / `RightToolbar` / `Editor` / `svg-icon`

### 4.8 字典使用

```vue
<script setup>
// useDict 是组合式函数，自动从 dict store 缓存或请求后端
const { dict } = useDict('sys_user_sex', 'sys_normal_disable')
</script>

<template>
  <!-- DictTag 组件渲染字典标签 -->
  <dict-tag :options="dict.sys_user_sex" :value="row.sex" />
  
  <!-- 下拉选择 -->
  <el-select v-model="queryParams.sex">
    <el-option
      v-for="item in dict.sys_user_sex"
      :key="item.value"
      :label="item.label"
      :value="item.value"
    />
  </el-select>
</template>
```

---

## 5. 禁止行为(红线)

- ❌ 禁止混用 `/opsx:apply` 与 `/implement`
- ❌ 禁止将 `design.md` 提升为正式架构决策
- ❌ 禁止手动修改 `openspec` 基准 spec
- ❌ 禁止硬编码接口地址、密钥等敏感信息
- ❌ 禁止自行发明接口字段、路径、错误码(以后端 OpenSpec 为准)
- ❌ 禁止前端自行将后端字段转驼峰/下划线(保持一致)
- ❌ 禁止给有菜单的页面手动加路由(由后端动态生成)
- ❌ 禁止把临时数据塞进 Pinia 全局状态
- ❌ 禁止全局样式污染(用 scoped)
- ❌ 禁止假定存在 ESLint / Prettier / 测试脚本(本项目没有)

---

## 6. 文档索引(按需阅读)

| 场景 | 文档 | 说明 |
|------|------|------|
| 开发新特性 / OpenSpec 工作流 | [docs/agent-workflow.md](docs/agent-workflow.md) | **必须先读**：/opsx:propose → /implement → /opsx:archive 全流程、命名规则、跨仓协作、会话恢复模板、skills 索引 |
| 接续已有开发任务 | [docs/agent-workflow.md](docs/agent-workflow.md) | 会话恢复模板:按需求 ID 读取 proposal/specs |
| 样式 / 表单校验 / 状态管理 / 路由 / 性能 / 文件权限 | [docs/coding-conventions.md](docs/coding-conventions.md) | 编码规范详细版(主文件第 4 节只保留高频约定) |

> 以上文档在相关任务时按需 Read，不要在本会话中主动加载全部内容。

---

## 7. Agent skills

### Issue tracker

本仓库的 issue 以本地 markdown 文件跟踪（`.scratch/<feature-slug>/` 目录）。见 `docs/agents/issue-tracker.md`。

### Domain docs

单上下文布局：`CONTEXT.md` 与 `docs/adr/` 位于仓库根目录。见 `docs/agents/domain.md`。
