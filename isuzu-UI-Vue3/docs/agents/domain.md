# Domain Docs

工程技能在探索代码库时应如何消费本仓库的领域文档。

## Before exploring, read these

- 仓库根目录下的 **`CONTEXT.md`**，或
- 若存在 **`CONTEXT-MAP.md`**，则按其指向逐一阅读各 context 的 `CONTEXT.md`（仅多上下文仓库）
- **`docs/adr/`** —— 阅读与你即将工作的区域相关的 ADR

以上文件若不存在，**静默跳过**。不要标注缺失，也不要主动建议提前创建。`/domain-modeling` 技能（经 `/grill-with-docs` 与 `/improve-codebase-architecture` 触达）会在术语或决策真正落地时懒创建它们。

## File structure

单上下文仓库（绝大多数仓库）：

```
/
├── CONTEXT.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

多上下文仓库（根目录存在 `CONTEXT-MAP.md`）：

```
/
├── CONTEXT-MAP.md
├── docs/adr/                          ← 系统级决策
└── src/
    ├── ordering/
    │   ├── CONTEXT.md
    │   └── docs/adr/                  ← context 级决策
    └── billing/
        ├── CONTEXT.md
        └── docs/adr/
```

## Use the glossary's vocabulary

输出中涉及领域概念时（issue 标题、重构提案、假设、测试名），使用 `CONTEXT.md` 中定义的术语，不要偏移到 glossary 明确规避的同义词。

若所需概念不在 glossary 中，这是一个信号——要么你正在发明项目未使用的语言（请重新考虑），要么存在真实缺口（记下来供 `/domain-modeling` 处理）。

## Flag ADR conflicts

若你的输出与既有 ADR 冲突，显式指出而非静默覆盖：

> _Contradicts ADR-0007 (event-sourced orders) — but worth reopening because…_
