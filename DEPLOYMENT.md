# 部署与访问说明（云服务器 POC 演示）

> 当前部署为**公网 POC 演示环境**，全部数据为虚构样例。
> 禁止上传真实政审材料；正式使用前必须更换默认密码、启用 HTTPS，并按学校要求做安全加固。

## 1. 访问地址

```text
http://64.83.11.147/
```

前端由 Nginx 静态托管，`/api/` 反向代理到本机 `127.0.0.1:8080`。

演示账号（密码均为 `Party@2026`）：

| 账号 | 角色 | 可体验 |
| --- | --- | --- |
| `admin` | R1 超级管理员 | 清单配置、全院台账、材料核验、POC 实验室 |
| `secretary` | R2 支部书记 | 本支部台账、材料 |
| `assistant` | R3 党务助理 | 本支部台账、材料 |
| `student` | R4 学生 | 仅本人材料 |

## 2. 部署结构

```text
浏览器
  │ HTTP :80
  ▼
Nginx（/usr/local/nginx）
  ├── /            → /var/www/party（Vue 构建产物）
  └── /api/        → http://127.0.0.1:8080（Spring Boot）
                          ├── MySQL 8.0（127.0.0.1:3306，库 party_build）
                          └── /root/develop/Party_Building_System/backend/data/files（材料文件）
```

关键路径：

| 项 | 路径 |
| --- | --- |
| Nginx 站点配置 | `/usr/local/nginx/conf/vhost/party.conf` |
| 前端静态目录 | `/var/www/party` |
| 后端 Jar | `/root/develop/Party_Building_System/backend/target/party-build-system-0.1.0-SNAPSHOT.jar` |
| 后端日志 | `/tmp/opencode/app4.log`（当前进程） |
| Nginx 访问日志 | `/usr/local/nginx/logs/party.access.log` |
| Nginx 错误日志 | `/usr/local/nginx/logs/party.error.log` |
| 数据库 | `party_build`，账号 `party` |

## 3. 启动/重启

### 3.1 后端

```bash
cd /root/develop/Party_Building_System/backend
set -a; source ../.env; set +a
nohup java -jar target/party-build-system-0.1.0-SNAPSHOT.jar > /tmp/opencode/app.log 2>&1 &
```

健康检查：

```bash
curl http://127.0.0.1:8080/api/v1/health
```

### 3.2 前端

```bash
cd /root/develop/Party_Building_System/frontend
npm run build
rm -rf /var/www/party/*
cp -r dist/. /var/www/party/
chmod -R a+rX /var/www/party
```

### 3.3 Nginx

```bash
nginx -t
nginx -s reload
```

## 4. 当前已做的访问控制

- `/api/v1/auth/login`、`/api/v1/health` 公开；
- 其余 API 必须携带 JWT；
- `/api/v1/poc/**` 仅 R1/R2/R3 可访问，R4 返回 403；
- 材料/人员接口按角色 + 支部/本人数据范围校验；
- Nginx 设置 `X-Content-Type-Options`、`X-Frame-Options`、`Referrer-Policy`、CSP；
- 登录接口按 IP 限流（10 次/分钟，突发 5 次）；
- 上传大小限制 25MB；
- 前端 `dist` 放在 `/var/www/party`，Nginx 不以 root 身份读取用户目录。

## 5. 上线前必须处理

1. **更换默认密码**：`admin/Party@2026` 等账号只适合 POC；正式使用必须改密。
2. **启用 HTTPS**：当前为 HTTP，登录令牌与材料内容明文传输，正式环境必须配置证书。
3. **限制访问范围**：建议加 Nginx Basic Auth、校园网 IP 白名单或 VPN。
4. **关闭 POC 接口**：正式环境应移除或限制 `/api/v1/poc/**`。
5. **修改 JWT/加密密钥**：`JWT_SECRET`、`CRYPTO_KEY` 必须替换为随机值。
6. **真实材料合规**：不得将真实政审材料上传公网；如确需使用，按学校审批与脱敏要求执行。
7. **模型费用**：`/api/v1/poc/llm` 会调用真实模型，建议设置配额与告警。

## 6. 常见问题

| 现象 | 排查 |
| --- | --- |
| 502 Bad Gateway | 后端未启动或端口不是 8080：`jps -l`、`curl 127.0.0.1:8080/api/v1/health` |
| 403 Forbidden（首页） | `/var/www/party` 权限不足：`chmod -R a+rX /var/www/party` |
| 444 / 连接被重置 | 请求 Host 不是 `64.83.11.147`，被默认 server 拦截；检查访问地址 |
| API 401 | 未登录或 JWT 过期；重新登录 |
| API 403 | 角色/支部数据范围不允许，或访问了 R4 不可用的 POC 接口 |
| 上传失败 413 | 文件超过 25MB，或 Nginx `client_max_body_size` 未生效 |
| 核验一直 RUNNING | 查看后端日志；OCR 单页约 25 秒，扫描件 + 模型可能接近 60 秒 |

## 7. 说明

- 云服务器安全组需放行 TCP 80；当前 Nginx 已监听 80/443。
- 443 上已有其他站点证书，本演示站暂用 80，避免影响现有站点。
- 本文件只描述当前 POC 部署，不代表生产环境最终方案。