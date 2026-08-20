# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

- 五十铃（Isuzu）访客登记系统：访客扫码预约登记 → 被访问人邮件审批 → 门卫线下核验放行。
- 业务规范以 `docs/01_PRD.md`（v1.2）与 `docs/02_业务流程图.md` 为准，接口与表结构以 `docs/03_接口契约.md` 为准；修改业务逻辑前先核对 PRD，业务变更需同步更新 PRD。
- 两个工程：
  - `isuzu-visitor-h5/` — 前端 H5（访客扫码使用），Vue 3.5 + Vite 8 + Vue Router 5 + Pinia 4 + Vant 4 + axios，Node `^22.18.0 || >=24.12.0`。**业务页面、API 层已全部实现**（详见「架构与约定」）。
  - `RuoYi-Springboot4/` — 后端（若依 RuoYi v3.9.2，Spring Boot 4 分支）。**访客业务接口已实现**（`com.ruoyi.visitor` 包：申请单/随行人员/审批/邮件，详见「架构与约定」）。
- 三个业务角色：访客（申请人，扫码进入 H5）、被访问人（审批人，经邮件进入审批确认页）、门卫（**不进入系统**，仅线下核验访客手机上的列表页）。
- 仓库为 **git 仓库**（main 分支，远程 GitHub），改动可提交备份（提交前注意 `application-druid.yml` 等含明文凭据的配置文件勿提交）。

## 常用命令

前端（在 `isuzu-visitor-h5/` 下执行）：

```bash
npm run dev       # 启动开发服务器
npm run build     # 生产构建
npm run preview   # 预览构建产物
npm run lint      # 串行执行 oxlint + eslint，均带 --fix（会直接修复）
npm run format    # prettier 格式化 src/
```

- 无测试框架（未安装 vitest/jest/cypress）。

后端（在 `RuoYi-Springboot4/` 下执行，Maven 多模块，JDK 17）：

```bash
bin/run.bat           # 一键编译并启动（ruoyi-admin，端口 8080）
bin/package.bat       # 打包（生成 ruoyi-admin/target/ruoyi-admin.jar）
bin/clean.bat         # 清理 target
mvn spring-boot:run   # 在 ruoyi-admin 模块下直接启动（需先 mvn install 依赖模块）
```

- 启动类：`ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java`。
- 数据库初始化脚本：`sql/ry_20260417.sql`（若依系统表）+ `sql/quartz.sql`（定时任务表），**不含访客业务表**；访客业务表（`visitor`、`visitor_application`、`visitor_companion`）DDL 见 `docs/03_接口契约.md` §2，需手动执行建表。

## 架构与约定

### 前端（isuzu-visitor-h5/）

- `@` 别名指向 `./src`（vite.config.js 与 jsconfig.json 均已配置），新代码一律用 `@/` 导入，禁止相对路径。
- 样式体系：`src/styles/global.css` 为全局样式基准（**已由 `main.js` 引入**），包含设计 token（CSS 变量：`--page-bg` 浅灰白背景、`--color-title` 棕金标题、`--color-primary` 蓝主按钮、`--radius-*` 圆角等）与公共类（`.page` 页面容器、`.page-title` 棕金标题、`.page-card` 白卡片、`.field-label` 字段标签含 `.required` 红星）；Vant 主题变量 `--van-primary-color` 已与主色对齐。**页面开发统一使用这些变量与类，不另起样式体系**（视觉基调参考 `docs/pic/股份_访客系统/`）。
- `src/` 业务已全部实现：`views/` 下 5 个页面（Entry 入口分流、UserInfo 用户信息/我的信息、Application 申请单、List 访客预约列表、Approve 审批确认；登记成功为申请单页内弹层）、`api/visitor.js`（9 个 `/visitor/**` 接口）、`stores/visitor.js`、`utils/`（request/date/uuid/avatar/mask）、`components/CompanionList.vue`（随行人员名单展示）。
- `src/stores/visitor.js` 是 Pinia setup store 风格范本，新业务 store 沿用该写法。
- axios 已封装为 `src/utils/request.js`（响应拦截器统一处理 code≠200 并 toast）；API baseURL 采用 `VITE_API_BASE_URL` 环境变量约定。
- vite.config.js **已配置 proxy**：`/visitor` 与 `/profile` → `http://localhost:8080`（后端 8080 端口），联调无需再改。
- 主要依赖版本较新（Vue Router 5 / Pinia 4 / Vite 8），API 与网上常见旧版教程可能有差异，遇到问题时先查官方文档。

### 后端（RuoYi-Springboot4/）

- 若依 RuoYi v3.9.2（Spring Boot 4.0.3 分支，JDK 17，Maven 多模块聚合工程）。
- 模块划分：
  - `ruoyi-admin` — 启动模块（`RuoYiApplication`），含 `com.ruoyi.web.controller` 控制器与 `application.yml` 配置；
  - `ruoyi-system` — 系统业务模块，**访客业务应新增在此模块**，按若依分层：`domain`（实体）、`mapper`（MyBatis 接口 + `resources/mapper/` 下 XML）、`service`/`service/impl`、`controller` 放 ruoyi-admin；
  - `ruoyi-framework` — 安全/拦截器/Redis/JWT 等框架配置；
  - `ruoyi-common` — 通用工具（AjaxResult、BaseEntity、SecurityUtils 等）；
  - `ruoyi-quartz` — 定时任务；`ruoyi-generator` — 代码生成器（可自动生成 CRUD 前后端代码）。
- 技术栈：Spring Boot 4 + Spring Security + JWT（token 有效期 30 分钟）+ Redis + MyBatis + MySQL（Druid 连接池）+ PageHelper 分页 + springdoc（`/swagger-ui.html`）。
- 服务端口 8080，context-path `/`；配置：`ruoyi-admin/src/main/resources/application.yml` + `application-druid.yml`（**已配置远程 MySQL/Redis 环境，含明文凭据，勿提交到外部仓库**）。
- 安全注意：若依接口默认有登录鉴权；访客接口（`/visitor/**`，扫码即用、无登录）**匿名放行**——在 `SecurityConfig.filterChain` 追加 `.requestMatchers("/visitor/**").permitAll()` 或用 `@Anonymous` 注解（`com.ruoyi.common.annotation.Anonymous`，PermitAllUrlProperties 自动扫描）。
- **访客模块落地要点**（详见 `docs/03_接口契约.md` §4/§5）：
  - 被访人数据**复用 `sys_user`**（status='0' 且 del_flag='0'，姓名取 nick_name、部门取 sys_dept.dept_name、邮箱取 email），不新建表；
  - 审批 token 用 **JWT（HS512）**：独立密钥配置项 `approve.secret`（与登录 `token.secret` 解耦），claims `{appId, exp}`（7 天），链接 `/approve?token={jwt}`；
  - 审批邮件由后端发送：`spring-boot-starter-mail` 已引入（`ruoyi-system/pom.xml`），`VisitorMailService` 已实现——`spring.mail.*` 未配置时 `JavaMailSender` 不注入、发送跳过仅记日志，**邮件发送失败不影响申请单提交**；随行人员 v1.3 起正文仅报「随行人员：N 人」；
  - 若用代码生成器：`ruoyi-generator/src/main/resources/generator.yml` 需调整 `packageName: com.ruoyi.visitor`、`tablePrefix` 追加 `visitor_`。
- 项目使用阿里云 Maven 镜像仓库（pom.xml 已配置），无需改 settings.xml。

## 代码规范

- Prettier：无分号、单引号、printWidth 100；`.editorconfig` 为 2 空格缩进、LF 换行。
- lint 由 oxlint（`.oxlintrc.json`，correctness 为 error）+ ESLint 10 flat config（含 Vue 插件与 prettier 冲突规则关闭）组成。
- `.vscode/settings.json` 已配置保存时自动格式化（Prettier）。

后端（RuoYi-Springboot4/）代码规范：

- 新增业务代码时沿用若依现有分层与命名风格（如 `domain` 实体继承 `BaseEntity`、Service 接口 + `impl` 实现、`AjaxResult` 统一返回、Mapper 接口 + XML）。
- 后端无格式化/lint 脚本配置，保持与现有 Java 代码风格一致（4 空格缩进、类注释、`@RestController` + `@PreAuthorize` 注解惯例）。

## 业务规则要点（实现页面时必须遵守）

- **新老用户判断**：`visitor_id` 由前端生成（UUID 格式）存入 localStorage；无则视为新用户进入用户信息页，有则提交后台查询分流。
- **老用户分流**（按当前日期判断是否存在「有效审批记录」，即日期落在申请的开始~结束时间范围内）：
  - 无有效记录 → 申请单页；
  - 有记录且未审批/通过 → 访客预约列表页；
  - 有记录但拒绝且**当日拒绝数 < 3** → 申请单页（弹窗提醒剩余次数 `3 - 当日拒绝数`）；
  - 当日拒绝数 **≥ 3** → 列表页 + 弹窗「审批人拒绝近期访问，谢谢。」，**禁止再提交申请**。拒绝次数按自然日统计，次日清零。
- **申请单页约束**：被访问人只能弹窗搜索选择、**禁止手输**；开始时间不早于当前日期；结束时间必须晚于开始时间；访问事由 ≤200 字。
- **被访人查询结果字段**：部门、姓名（radio 单选）。**无 SAP 号**。被访人数据来源已确认：若依 `sys_user` 正常用户（status='0' 且 del_flag='0'），邮箱取 `sys_user.email` 用于发审批邮件。
- **邮件审批**：提交成功由**后端**发审批邮件（含「审批」/「拒绝」按钮，链接携带 **JWT**：`/approve?token={jwt}`，HS512 + 独立密钥 `approve.secret`，7 天有效）；审批确认页点击「批准」/「拒绝」**直接回写**（**无二次确认弹窗**），状态流转 `未审批 → 通过/拒绝`。
- **只能审批一次**：已审批申请单再次通过邮件链接进入时，详情接口直接返回 601「该申请单已完成审批」，前端**不渲染审批操作区**；回写接口条件更新 `WHERE status='0'` 兜底，绕过详情页直接调用也无法二次审批。
- **访客预约列表页双重职责**：既是访客查看审批记录页，也是**门卫核验凭证**（顶部醒目展示头像/姓名/单位防冒用 + 状态标签）；仅「通过」且日期有效期内放行。
  - **防伪（已确认）**：页面实时展示**当前时间戳**（前端本地时间），防截图冒用；不做水印与二维码。
  - **空态（已确认）**：无有效记录时仅空状态图标，**不展示文案**；顶部用户信息区**仅**「我的信息」编辑入口。
- 移动端 320~430px 适配，需兼容微信内置浏览器扫码场景；无 CSS 预处理器与 rem/vw 方案，样式基于 Vant 4（`--van-*` CSS 变量定制主题）。
- **已确认**（2026-08-13/2026-08-16）：头像照片**必填**（用户信息页/我的信息页校验）；申请单页**无**「个人信息保护政策」勾选（提交按钮不依赖勾选）；随行人员**姓名+身份证号**（18 位末位可 X）、最多 5 人可为空、不注册仅名单随申请单展示（申请单/门卫列表/审批详情，身份证展示脱敏）。PRD §9.2 待确认事项 1~9 **全部已确认**（详见 PRD v1.4）。

## 文档目录

- `docs/01_PRD.md` — 产品需求文档（v1.4，业务规则权威来源，含随行人员 v1.4）。
- `docs/02_业务流程图.md` — 6 张 mermaid 流程图（主流程、被访人选择、邮件审批时序、审批状态流转、页面流转、门卫核验）。
- `docs/03_接口契约.md` — **接口与表结构契约**（v1.3）：通用约定、DDL（visitor/visitor_application/visitor_companion）、9 个 `/visitor/**` 接口、审批 JWT 设计、邮件方案、联调约定。前后端开发均以此为准。
- `docs/pic/prototype_pic/` — 5 张原型图；`docs/pic/股份_访客系统/` — 参考截图。
- `docs/discard/` — 已弃用文档，勿参考。
- `RuoYi-Springboot4/README.md` — 若依官方模板说明（含各 Spring Boot 版本分支差异）；后端框架文档见 https://doc.ruoyi.vip。

## Agent skills

### Issue tracker

Issues 以 markdown 文件存于 `.scratch/<feature-slug>/`（每功能一个目录，单 ticket 单文件，`Status:` 记录 triage 状态）。See `docs/agents/issue-tracker.md`.

### Triage labels

五个规范 triage 角色使用默认标签：`needs-triage`、`needs-info`、`ready-for-agent`、`ready-for-human`、`wontfix`。See `docs/agents/triage-labels.md`.

### Domain docs

Single-context：根级 `CONTEXT.md` + `docs/adr/`，目前均不存在，由 `/domain-modeling` 惰性创建，探索时缺失则静默跳过。See `docs/agents/domain.md`.
