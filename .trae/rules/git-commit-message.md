---
alwaysApply: false
description: Git 提交信息格式规范
scene: git_message
---

# Git 提交信息规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

```
<type>: <description>
```

## Type 类型

| Type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 Bug |
| `refactor` | 重构（不改变功能） |
| `docs` | 文档变更 |
| `style` | 代码格式（不影响逻辑） |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具/依赖变更 |
| `ci` | CI/CD 配置 |
| `build` | 构建系统变更 |

## 示例

```
feat: add folder batch import for dictionaries
fix: correct FSRS meanReversion weight calculation
refactor: split AppViewModel into domain ViewModels
test: add unit tests for MdxParser V2.0
chore: update Kotlin to 1.9.24
```

## 规则

- description 使用英文，小写开头，不加句号
- 不超过 72 个字符
- 一次提交只做一件事
- Breaking Change 在 footer 中标注 `BREAKING CHANGE:`

