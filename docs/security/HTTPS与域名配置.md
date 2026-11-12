# HTTPS 与域名访问配置说明

> 目标：使用 `https://dangjian.morose.top/` 访问党员发展智能管理系统，同时保留 `https://morose.top/` 个人网站不变。

## 1. 推荐方案：独立子域名

| 项 | 值 |
| --- | --- |
| 访问地址 | `https://dangjian.morose.top/` |
| DNS 记录 | A `dangjian` → `64.83.11.147` |
| 服务器 | 本机 Nginx 80/443 |
| 前端目录 | `/var/www/party` |
| 后端 | `127.0.0.1:8080` |

### 为什么不直接用 `morose.top/party`

可以，但需要：

- Vite `base: '/party/'`；
- 前端静态资源路径改写；
- API 代理路径改写；
- 所有前端路由前缀调整。

工作量和回归风险都高于独立子域名，因此推荐 `dangjian.morose.top`。

## 2. 添加 DNS 解析

在域名服务商控制台添加：

```text
记录类型：A
主机记录：dangjian
记录值：64.83.11.147
TTL：600
```

验证：

```bash
getent hosts dangjian.morose.top
# 应返回 64.83.11.147
```

> 如果使用 Cloudflare 橙云代理，HTTP-01 校验可能失败；可先切灰云（仅 DNS），签发证书后再决定是否开代理。

## 3. 申请证书并启用 HTTPS

服务器已安装 `certbot`。DNS 生效后执行：

```bash
cd /root/develop/Party_Building_System
bash scripts/enable_https_domain.sh dangjian.morose.top 你的邮箱@example.com
```

脚本会：

1. 校验 DNS 是否指向本机公网 IP；
2. 创建 ACME 校验目录 `/var/www/letsencrypt`；
3. 写入临时 HTTP 配置；
4. 使用 `certbot --webroot` 申请 Let's Encrypt 证书；
5. 写入正式 443 站点配置 `/usr/local/nginx/conf/vhost/party-domain.conf`；
6. 配置 HTTP → HTTPS 跳转；
7. 配置证书续期后自动 `nginx -s reload`；
8. 执行 `nginx -t` 与 `nginx -s reload`。

完成后访问：

```text
https://dangjian.morose.top/
```

## 4. 现有 `morose.top` 个人网站

不会受影响：

- `morose.top` / `www.morose.top` 继续使用原配置与证书；
- 新增的是 `dangjian.morose.top` 独立 server 块；
- 原证书只包含 `morose.top` 与 `www.morose.top`，不包含子域名，所以必须单独申请证书。

## 5. 安全与运维

- 已配置 HSTS、CSP、X-Frame、X-Content-Type、Referrer-Policy；
- 登录接口继续使用 Nginx 限流；
- 上传大小 25MB；
- 证书自动续期检查：

```bash
certbot renew --dry-run
```

- 如遇证书续期失败，检查 80 端口 ACME 路径是否可访问：

```bash
curl http://dangjian.morose.top/.well-known/acme-challenge/test
```

## 6. 常见问题

| 现象 | 排查 |
| --- | --- |
| DNS 不生效 | `getent hosts dangjian.morose.top`；等待 TTL |
| certbot 校验失败 | 80 端口是否放行；Cloudflare 是否橙云；`/.well-known` 是否可访问 |
| 502 Bad Gateway | 后端 8080 未启动 |
| 403 首页 | `/var/www/party` 权限不足 |
| 仍显示个人网站 | 检查 `server_name` 是否匹配；`nginx -T | grep dangjian.morose.top` |
| 证书不匹配 | 确认使用 `dangjian.morose.top` 的证书，而不是 `morose.top` 的证书 |

## 7. 回滚

删除或重命名：

```bash
mv /usr/local/nginx/conf/vhost/party-domain.conf /usr/local/nginx/conf/vhost/party-domain.conf.bak
nginx -t && nginx -s reload
```

原 `party.conf`（IP 访问）仍在，可继续使用 `http://64.83.11.147/`。
