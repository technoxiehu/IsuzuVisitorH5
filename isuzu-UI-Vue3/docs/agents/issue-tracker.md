# Issue tracker: Local Markdown

本仓库的 issue 与 spec 以 markdown 文件形式存于 `.scratch/` 目录。

## Conventions

- 每个特性一个目录：`.scratch/<feature-slug>/`
- spec 文件为 `.scratch/<feature-slug>/spec.md`
- 实现 issue 按 ticket 逐个建文件：`.scratch/<feature-slug>/issues/<NN>-<slug>.md`，编号从 `01` 开始——不要合并为单个 tickets 文件
- Triage 状态通过各 issue 文件顶部的 `Status:` 行记录
- 评论与对话历史追加到文件底部 `## Comments` 段落下

## When a skill says "publish to the issue tracker"

在 `.scratch/<feature-slug>/` 下新建文件（目录不存在则先创建）。

## When a skill says "fetch the relevant ticket"

直接读取所引用的路径文件。用户通常会直接传路径或 issue 编号。

## Wayfinding operations

供 `/wayfinder` 使用。**map** 是一个文件，每个 **child** ticket 一个文件。

- **Map**：`.scratch/<effort>/map.md` —— 承载 Notes / Decisions-so-far / Fog 内容
- **Child ticket**：`.scratch/<effort>/issues/NN-<slug>.md`，编号从 `01` 开始，正文包含问题描述。`Type:` 行记录 ticket 类型（`research`/`prototype`/`grilling`/`task`）；`Status:` 行记录 `claimed`/`resolved`
- **Blocking**：文件顶部附近的 `Blocked by: NN, NN` 行。当列出的每个文件均为 `resolved` 时，ticket 解除阻塞
- **Frontier**：扫描 `.scratch/<effort>/issues/`，找出 open、未阻塞且未被认领的文件；按编号升序取第一个
- **Claim**：开工前先设置 `Status: claimed` 并保存
- **Resolve**：在 `## Answer` 段落下追加答案，设置 `Status: resolved`，然后在 `map.md` 的 Decisions-so-far 中追加一条 context 指针（gist + 链接）
