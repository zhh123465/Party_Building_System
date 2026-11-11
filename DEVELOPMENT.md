# 开发与运行说明（工程骨架 / POC）

> 当前版本只实现“可运行的工程骨架 + 关键 POC”，不是完整业务系统。
>
> 云服务器公网演示地址：`http://64.83.11.147/`；部署与排障见 `DEPLOYMENT.md`。
> 所有演示数据均为虚构，禁止上传真实政审材料。

## 1. 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 后端 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端（当前验证 18.19） |
| MySQL | 8.0 | 数据库 |
| Tesseract | 5.x | OCR，需中文包 `chi_sim` |
| 中文字体 | Noto CJK / 宋体 | 页面与导出 |

## 2. 数据库初始化

```sql
CREATE DATABASE party_build CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'party'@'localhost' IDENTIFIED BY 'party_dev_2026';
GRANT ALL PRIVILEGES ON party_build.* TO 'party'@'localhost';
FLUSH PRIVILEGES;
```

后端启动时由 Flyway 自动执行 `V1__init.sql` 与 `V2__seed.sql`。

## 3. 环境变量

复制 `.env.example` 为 `.env`，填入模型 Key：

```bash
cp .env.example .env
# 编辑 .env，至少填写 LLM_API_KEY
```

`.env` 已被 `.gitignore` 忽略，不会进入仓库。

当前 POC 默认使用 OpenAI 兼容接口：

```text
LLM_PROVIDER=tokenrhythm
LLM_BASE_URL=https://tokenrhythm.studio/v1
LLM_MODEL=deepseek-v4.1-flash-1017
```

也可切换为豆包或其他 OpenAI 兼容服务，只需改 `LLM_BASE_URL`、`LLM_MODEL`、`LLM_API_KEY`。

## 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

或：

```bash
cd backend
mvn -DskipTests package
set -a; source ../.env; set +a
java -jar target/party-build-system-0.1.0-SNAPSHOT.jar
```

健康检查：

```bash
curl http://127.0.0.1:8080/api/v1/health
```

## 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://127.0.0.1:5173`。Vite 已配置 `/api` 代理到 `http://127.0.0.1:8080`。

生产构建：

```bash
npm run build
```

## 6. 演示账号

| 账号 | 密码 | 角色 | 数据范围 |
| --- | --- | --- | --- |
| `admin` | `Party@2026` | R1 超级管理员 | 全院 |
| `secretary` | `Party@2026` | R2 支部书记 | 本支部 |
| `assistant` | `Party@2026` | R3 党务助理 | 本支部 |
| `student` | `Party@2026` | R4 学生 | 仅本人 |

## 7. 已实现接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/auth/login` | 登录，返回 JWT |
| POST | `/api/v1/auth/logout` | 登出，tokenVersion+1 |
| GET | `/api/v1/auth/me` | 当前用户 |
| PUT | `/api/v1/auth/password` | 修改密码 |
| GET | `/api/v1/persons` | 人员台账，按角色/支部过滤 |
| GET | `/api/v1/persons/{id}` | 人员详情 |
| POST | `/api/v1/persons` | 建档 |
| GET | `/api/v1/persons/{id}/id-card` | 完整身份证号（R1/R2） |
| GET | `/api/v1/persons/import/template` | 导入模板 |
| POST | `/api/v1/persons/import` | Excel 批量导入 |
| GET | `/api/v1/material-types` | 材料类型 |
| POST/PUT | `/api/v1/material-types`、`/material-types/{id}` | 维护材料类型 |
| GET/POST | `/api/v1/checklists/templates` | 清单模板列表/新建 |
| GET/PUT | `/api/v1/checklists/templates/{id}` | 模板详情/修改 |
| POST | `/api/v1/checklists/templates/{id}/clone` | 克隆新版本 |
| POST | `/api/v1/checklists/templates/{id}/validate` | 校验模板 |
| POST | `/api/v1/checklists/templates/{id}/publish` | 发布模板 |
| POST/PUT/DELETE | `/api/v1/checklists/templates/{id}/items`、`/checklists/items/{itemId}` | 清单条目维护 |
| GET | `/api/v1/persons/{personId}/materials` | 人员材料清单 |
| GET | `/api/v1/materials/{id}` | 材料详情与最新报告 |
| POST | `/api/v1/materials/{id}/versions` | 上传材料版本 |
| GET | `/api/v1/materials/{id}/versions/{versionId}/download` | 鉴权下载 |
| POST | `/api/v1/materials/{id}/verify` | 发起/重试核验 |
| GET | `/api/v1/verify-tasks/{taskId}` | 异步任务状态 |
| GET | `/api/v1/materials/{id}/reports/latest` | 最新报告 |
| GET | `/api/v1/reports/{reportId}` | 报告详情 |
| GET | `/api/v1/persons/{id}/archive/tree` | 电子档案目录树 |
| POST | `/api/v1/persons/{id}/archive?stage=S0` | 阶段归档 |
| GET | `/api/v1/persons/{id}/archives` | 归档记录 |
| GET | `/api/v1/archives/{id}/download` | 下载归档 ZIP |
| GET | `/api/v1/materials/search?keyword=` | 材料全文检索 |
| GET | `/api/v1/reports/overview` | 报表总览 |
| GET | `/api/v1/reports/stage-count` | 阶段人数 |
| GET | `/api/v1/reports/completion` | 完成率与逾期 |
| GET | `/api/v1/reports/compliance` | 人员合规指数 |
| GET | `/api/v1/reports/compliance/heatmap` | 支部热力图 |
| GET | `/api/v1/reports/export` | 导出 Excel |
| GET | `/api/v1/time-rules` | 时限规则 |
| PUT | `/api/v1/time-rules/{id}` | 修改时限规则 |
| GET | `/api/v1/warnings` | 预警清单 |
| GET | `/api/v1/warnings/summary` | 预警汇总 |
| PUT | `/api/v1/warnings/{id}/handle` | 处置预警 |
| POST | `/api/v1/warnings/generate` | 手动生成预警（R1） |
| GET | `/api/v1/schedule/calendar` | 年度预警日历 |
| POST | `/api/v1/schedule/reverse` | 倒推排期 |
| GET | `/api/v1/process/board` | 流程看板 |
| GET | `/api/v1/process/overview` | 看板总览 |
| GET | `/api/v1/process/stage-distribution` | 阶段分布 |
| GET | `/api/v1/process/persons/{id}/advance-check` | 六规则推进预校验 |
| POST | `/api/v1/process/persons/{id}/advance` | 推进阶段 |
| POST | `/api/v1/process/persons/{id}/rollback` | 回退阶段 |
| GET | `/api/v1/reviews/pending` | 待复核材料 |
| GET | `/api/v1/reviews/templates` | 退回意见模板 |
| POST | `/api/v1/materials/{id}/review` | 提交复核（R3 初审 / R1、R2 终审） |
| GET | `/api/v1/materials/{id}/reviews` | 复核流水 |
| GET | `/api/v1/messages` | 站内消息 |
| GET | `/api/v1/messages/unread-count` | 未读数 |
| PUT | `/api/v1/messages/{id}/read`、`/read-all` | 标记已读 |
| GET | `/api/v1/poc/config` | POC 配置 |
| POST | `/api/v1/poc/llm` | 模型 JSON 输出验证 |
| POST | `/api/v1/poc/ocr` | OCR/文本抽取 |
| POST | `/api/v1/poc/verify` | 端到端双引擎核验 |
| GET | `/api/v1/health` | 健康检查 |

## 8. POC 验证命令

```bash
# POC-1 模型 JSON
export LLM_API_KEY=...
python3 poc/llm_json_poc.py --rounds 10

# POC-2 OCR（示例图片可用 PDF 页面渲染）
pdftoppm -f 5 -l 5 -png -r 150 docs/export/05-需求规格说明书V2.1.pdf /tmp/page
python3 poc/ocr_poc.py /tmp/page-05.png --keywords 文档说明 需求规格说明书 V2.1

# POC-4/5/6 结果见 poc/results/poc_summary.md
```

## 9. 当前实现边界

已实现：

- Spring Boot 3 + MySQL + Flyway 工程骨架；
- JWT 登录、BCrypt、R1—R4 角色、支部/本人数据范围；
- 人员台账查询、建档、身份证号加密+掩码、完整值授权接口；
- Excel 批量导入（部分成功、错误行定位、学号幂等）；
- POC：模型 JSON、OCR、规则+语义端到端核验、失败降级；
- M3 材料清单配置：材料类型、模板、条目、校验、发布；
- M4 核验闭环：材料上传、数据库任务队列、异步核验、报告与发现项落库；
- M5 复核闭环：待办、初审/终审、通过/退回、意见模板、版本留痕、站内消息；
- M6 流程看板：人员进度、锁定/风险、阶段分布、六规则推进校验、二次确认、回退；
- M7 时限预警：四级预警、定时任务、预警清单与处置、倒推排期、年度日历、时限规则维护；
- M8 统计报表：阶段人数、完成率、逾期、合规指数 M/T/Q、支部热力图、Excel 导出；
- M9 电子档案：档案目录树、归档完整性核查、ZIP 归档包、清单/水印、全文检索；
- Vue 3 + Element Plus 登录、布局、工作台、台账、清单配置、材料核验、材料复核、流程看板、时限预警、发展日历、时限规则、统计报表、电子档案、档案检索、消息中心、POC 实验室。

尚未实现（按设计后续迭代）：

- M10 审计日志与数据安全完善；
- 流程看板与六规则推进；
- 时限预警与倒推排期；
- 报表导出、合规指数、归档、审计日志。

## 10. 注意事项

1. `application.yml` 中的 JWT/加密密钥仅为开发默认值，生产必须通过环境变量替换。
2. 演示数据全部虚构；真实材料不得上传公网或发送第三方模型。
3. 发送模型前只发送核验必需文本；POC 阶段尚未实现完整脱敏，正式接入真实材料前必须补齐。
4. OCR 当前使用 Tesseract，扫描件质量直接影响识别效果；POC 实测约 25 秒/页。
5. 前端当前只覆盖 POC 所需页面，不是完整业务 UI。