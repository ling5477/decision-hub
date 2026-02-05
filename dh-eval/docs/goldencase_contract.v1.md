# GoldenCase v1 契约（最小）

每条用例至少包含：
- id: 用例唯一
- input: user_prompt + constraints
- expect: 断言集合（结构断言优先）
  - must_include: string[]
  - must_not_include: string[]
  - min_quality: number（0~1）
  - require_passed: [constraint, consistency]（硬门槛）

建议：不要用“全量字符串相等”做断言，优先结构化断言与关键片段断言。
