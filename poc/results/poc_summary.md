# POC 验证结果汇总

> 环境：Ubuntu 24.04 / JDK 17 / Spring Boot 3.2.5 / MySQL 8.0.46 / Node 18.19 / Tesseract 5.3.4
> 模型：tokenrhythm `deepseek-v4.1-flash-1017`（OpenAI 兼容接口）
> 日期：2026-11-11
> 数据：全部为虚构演示数据

## 1. 结论总览

| 编号 | 验证项 | 结果 | 关键数据 | 判定 |
| --- | --- | --- | --- | --- |
| POC-1 | 模型 JSON 输出 | 10 次调用全部可解析 | 成功率 100%；P50 1,813ms；P90 2,010ms；最大 2,218ms | 通过 |
| POC-2 | 扫描件中文 OCR | 1,770 字，4/4 关键词命中 | 单页 25,197ms（含启动与渲染外耗时） | 通过，但需优化 |
| POC-3 | 端到端核验时延 | DOCX 材料 16,397ms；扫描件预计 >30s | 规则 3 项、语义 4 项；总体 DOUBT | 按类型拆分后通过 |
| POC-4 | 模型失败降级 | 超时/异常转人工，不阻断 | 单元测试通过；`manualRequired=true`，`UNKNOWN` 落库 | 通过 |
| POC-5 | 登录与权限隔离 | 四类角色登录、R4 仅本人、R3 本支部 | 越权请求返回 403/10003 | 通过 |
| POC-6 | 100 行 Excel 导入 | 95 成功、5 失败，错误行定位 | 二次导入仍 95/5，人员表 101 条且学号 101 个 | 通过 |

## 2. POC-1 模型 JSON

命令：

```bash
export LLM_API_KEY=...
python3 poc/llm_json_poc.py --rounds 10
```

结果文件：`poc/results/llm_json_poc.json`

```text
validJsonCount: 10 / 10
validRate: 1.0
latencyP50Ms: 1813
latencyP90Ms: 2010
latencyMaxMs: 2218
```

结论：

- JSON 模式（`response_format={"type":"json_object"}`）稳定可用；
- 单次 1.3—2.2 秒，远低于 30 秒超时；
- 仍需保留“解析失败重试一次 + 转人工”的降级逻辑。

## 3. POC-2 OCR

命令：

```bash
pdftoppm -f 5 -l 5 -png -r 150 docs/export/05-需求规格说明书V2.1.pdf /tmp/page
python3 poc/ocr_poc.py /tmp/page-05.png --keywords 文档说明 需求规格说明书 V2.1 党员
```

结果文件：`poc/results/ocr_poc.json`

```text
charCount: 1770
latencyMs: 25197
keywordHitRate: 1.0 (4/4)
lang: chi_sim+eng
```

结论：

- 中文识别质量满足 POC 演示，关键词全部命中；
- 单页 25 秒偏慢，扫描件“OCR + 模型”串行会超过 30 秒目标；
- 建议优化：降低渲染 DPI（150→120）、启用 Tesseract `--oem 1`、按页并行、或对扫描件改用视觉模型。

## 4. POC-3 端到端核验

请求：

```bash
curl -X POST http://127.0.0.1:8080/api/v1/poc/verify \
  -F "file=@sample_application.docx" \
  -F "points=入党动机是否具体,是否结合个人经历,是否存在空泛套话,思想变化是否真实"
```

结果：

| 项 | 值 |
| --- | --- |
| 总体结论 | `DOUBT` |
| 总耗时 | 16,397ms |
| 规则层 | 正文完整性 PASS、本人署名 PASS、材料日期 PASS |
| 语义层 | 入党动机 DOUBT、结合经历 PASS、空泛套话 DOUBT、思想变化 DOUBT |
| 文本长度 | 241 字（DOCX 文本抽取，无 OCR） |
| OCR | 未使用 |

结论：

- 规则 + 语义双引擎可以跑通，结论可落盘到 `Finding`；
- DOCX/文本型 PDF 可在 30 秒内完成；
- 扫描件因 OCR 约 25 秒，串行总耗时预计 40 秒以上，超过统一 30 秒目标；
- **已确认按材料类型拆分 SLO：文本类 ≤30s，扫描件 ≤60s**；后续继续优化 OCR。

## 5. POC-4 降级

单元测试：

```bash
cd backend && mvn -q test
```

覆盖：

- `SemanticVerifierTest`：模型持续返回非 JSON 时抛出业务异常；
- `PocServiceTest`：模型超时时 `manualRequired=true`、语义结论 `UNKNOWN`、总体 `DOUBT`，主流程不崩溃。

## 6. POC-5 登录与权限

| 场景 | 结果 |
| --- | --- |
| `admin` 登录 | 200，R1，全院 |
| `secretary` 登录 | 200，R2，本支部 |
| `assistant` 登录 | 200，R3，本支部 |
| `student` 登录 | 200，R4，仅本人 |
| R4 查看他人档案 | 403 / 10003 |
| R3 查看其他支部人员 | 403 / 10003 |
| R4 查看完整身份证号 | 403 / 10003 |
| R4 人员列表 | 仅返回本人 1 条 |
| R3 人员列表 | 仅返回本支部 5 条 |

## 7. POC-6 批量导入

生成 100 行样例（95 行正确 + 5 行错误）：

| 错误类型 | 行号 | 返回 |
| --- | --- | --- |
| 文件内学号重复 | 92 | `学号: 文件内重复` |
| 日期格式错误 | 93 | `递交申请书日期: 日期格式应为 yyyy-MM-dd` |
| 姓名为空 | 94 | `姓名: 格式不正确` |
| 支部不存在 | 95 | `所属支部: 不存在` |
| 阶段起始日早于申请日 | 96 | `阶段起始日期: 不得早于申请书日期` |

结果：

```text
total: 100
success: 95
failed: 5
```

二次导入同一文件：

```text
success: 95
failed: 5
```

数据库校验：

```text
person 总数 = 101
distinct student_no = 101
```

结论：部分成功、错误行定位、学号幂等均通过。

## 8. 待改进项

1. **OCR 性能**：25 秒/页偏慢，需优化或拆分 SLO。
2. **模型提示词**：当前语义核验偏保守，DOUBT 较多，需用标注样例集调优。
3. **脱敏**：POC 尚未在发送模型前做完整姓名/身份证号/电话替换，正式接入真实数据前必须补齐。
4. **持久化**：已在 M4 切片中接入 `verify_task` 异步任务与 `verify_report`/`verify_finding` 报告落库。
5. **前端**：当前仅覆盖登录、台账、POC 实验室，业务页面待后续迭代。
## 9. M3/M4 正式链路验证（2026-11-11）

在 POC 链路之外，已将核验接入正式表结构与异步任务：

| 验证项 | 结果 |
| --- | --- |
| 材料清单模板读取 | 模板 1，共 20 条材料条目 |
| 人员材料初始化 | 人员 1（S0）自动生成 3 条材料（申请书、谈话记录、纪实表） |
| 上传材料 | `sample_application.docx` 上传成功，生成 v1 与任务 1 |
| 异步核验 | 任务状态 PENDING → RUNNING → SUCCESS，耗时 11,541ms |
| 材料状态 | M2（待核验）→ M3（待人工复核） |
| 报告落库 | `verify_report` 1 条，`verify_finding` 7 条 |
| 报告结论 | rule=PASS、semantic=PASS、overall=PASS |
| 清单配置 API | 新建模板、添加条目成功；仅 1 个阶段时校验返回 S1—S4 缺项 |
| 权限 | R4 仅本人材料；R3 跨支部 403；R4 查看他人材料 403 |

> 说明：报告结论仍为“AI 建议”，材料进入 M3 后必须由人工复核，未接入 M5 复核前不会自动变成 M4。


## 10. M5 复核闭环验证（2026-11-11）

| 验证项 | 结果 |
| --- | --- |
| 待复核列表 | 返回 M3 材料，包含人员、材料类型、版本、AI 结论、初审状态 |
| R3 初审通过 | 写入 `review_log`，`review_level=INITIAL`，材料仍为 M3 |
| R1 终审通过 | 写入 `review_log`，`review_level=FINAL`，材料 M3 → M4 |
| 终审退回 | 意见不少于 10 字，材料 M3 → M5，`review_log` 记录原因 |
| 站内消息 | 学生账号收到 `REVIEW_RETURN` 消息，包含退回意见 |
| 重新上传 | M5 → M2，生成新版本 v2，旧版本 v1 保留 |
| 复核流水 | 按材料查询，包含复核人、初审/终审、结论、意见、时间 |
| 意见模板 | 10 条预置模板可读取 |
