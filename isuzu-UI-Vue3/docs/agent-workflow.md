# Agent 工作流(OpenSpec + mattpocock-skills)

> 本文档从 CLAUDE.md 拆分而来,仅在**开发新特性 / 接续任务 / 需要 skills 索引**时阅读。
> 主文件 CLAUDE.md 第 6 节为文档索引。

## 1. Agent 工作流(OpenSpec + mattpocock-skills)

### 1.1 核心分工

- **OpenSpec**：管页面交互、表单校验、路由规则、接口字段映射等前端系统行为规范
- **mattpocock-skills**：管需求澄清、组件设计、代码评审、架构治理

### 1.2 硬性规则(必须遵守)

1. **新特性统一使用 `/opsx:propose "需求描述"`** 创建 `openspec/changes` 变更包
2. **❗严禁执行 `/opsx:apply`**，所有编码实现全部交给 mattpocock-skills 的 `/implement`
3. matt 读取 OpenSpec 输入**仅限**：
   - `openspec/changes/{feature}/proposal.md`
   - `openspec/changes/{feature}/specs/` 全部增量 spec
4. **❗强制忽略**同目录下的 `design.md` 与 `tasks.md`：
   - `design.md`：OpenSpec 提案草稿，仅供人工阅读，禁止作为技能输入
   - `tasks.md`：OpenSpec 内部任务，matt 不读取，使用 `/to-tickets` 生成自有工单
5. 正式架构决策输出至 `docs/adr/`，输出标准 ADR 文档，**禁止读写 `openspec` 下的 `design.md`**
6. 业务术语统一维护在根目录 `CONTEXT.md`，由 `/grill-with-docs` / `/domain-modeling` 更新
7. `openspec/specs/` 基准库**禁止人工修改**，只能通过 `/opsx:archive` 自动合并

### 1.3 统一命名规则

| 对象 | 规则 | 示例 |
|------|------|------|
| 需求 ID | `模块缩写-三位序号` | `USR-001`、`ORD-003`、`PAY-002` |
| OpenSpec 变更文件夹 | `需求ID-功能英文短标识` | `USR-003-user-freeze` |
| ADR 文件名 | `需求ID-功能描述.md` | `USR-003-user-freeze-ui-design.md` |
| Git 分支 | `feature/需求ID-功能标识` | `feature/USR-003-user-freeze` |
| Commit Message | `feat(需求ID): 描述` | `feat(USR-003): 新增用户冻结按钮` |

**模块缩写**：USR=用户、ORD=订单、PAY=支付、SYS=系统(新增模块需在 `FEATURES.md` 中登记)

**跨仓约束**：OpenSpec 变更文件夹名**必须与后端仓对应变更的文件夹名字完全一致**。

### 1.4 跨仓协作规则(前端特有)

1. **接口契约以后端仓 `openspec/specs/` 为唯一事实源**，前端照着写请求和字段映射
2. 后端接口变更时，前端必须同步更新对应 delta-spec 和 API 封装
3. 字段命名必须和后端保持一致，**禁止前端自行转驼峰/下划线**
4. 破坏性变更(改字段名、删字段)必须等后端版本升级(如 `/api/v2/`)后再适配

### 1.5 开发顺序约束

1. **必须等后端接口契约确定后，再启动前端开发**
2. 后端 delta-spec 评审通过、接口定义稳定后，前端再开始 grill 和 implement
3. 开发过程中后端接口有调整，同步更新前端 spec 和 API 封装
4. 前端可先用 Mock 数据开发页面，但联调前必须替换为真实接口

### 1.6 标准完整工作流(中大型功能)

1. 确认需求 ID，更新 `FEATURES.md`
2. `/opsx:propose "需求描述"`
3. 人工审阅修改 `proposal.md`、`delta-spec`(重点定好页面交互、表单校验、接口字段映射)
4. 读取修改后的文件，执行 `/grill-with-docs`
5. `/to-spec` → `/to-tickets`
6. `/implement`
7. 自测完成执行 `/opsx:archive`
8. 更新 `FEATURES.md` 状态为已上线

### 1.7 轻量流程(简单小需求)

1. 确认需求 ID，更新 `FEATURES.md`
2. `/opsx:propose "需求描述"`
3. 人工审阅修改
4. `/implement`
5. `/opsx:archive`
6. 更新 `FEATURES.md` 状态

### 1.8 文档关联规则

每个变更的 `proposal.md` 顶部必须包含：
```markdown
## 关联信息
- 需求ID：USR-003
- 对应后端仓变更：USR-003-user-freeze
- 接口契约来源：后端仓 openspec/specs/user/
```

### 1.9 需求总索引

项目根目录维护 `FEATURES.md`，记录所有需求的前后端对应关系：

| 需求ID | 功能名称 | 后端变更目录 | 前端变更目录 | 状态 | 上线时间 |
|--------|----------|--------------|--------------|------|----------|
| USR-003 | 用户冻结 | USR-003-user-freeze | USR-003-user-freeze | 🚧开发中 | - |

- 新开需求先确认需求 ID、更新 `FEATURES.md`，再执行 `/opsx:propose`
- 开发完成归档后，将状态改为 ✅已上线 并补充上线时间

---

## 2. 会话恢复模板(新开会话接续)

当需要接续之前的开发任务时：

1. 读取 `FEATURES.md`，找到对应需求 ID 和变更目录
2. 读取 `openspec/changes/{feature-folder-name}/proposal.md`
3. 读取该目录 `specs/` 下所有增量 spec
4. **忽略** `design.md`、`tasks.md`
5. 确认对应后端仓接口契约是否已稳定
6. 接续 mattpocock-skills 流程，不确定点向用户确认

---

## 3. Agent Skills 索引

- **Issue tracker**：Issues and specs live as local markdown files under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.
- **Domain docs**：Single-context — one `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
