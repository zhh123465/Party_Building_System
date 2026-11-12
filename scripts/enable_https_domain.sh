#!/usr/bin/env bash
# 为党员发展智能管理系统启用 HTTPS 子域名
# 用法：
#   bash scripts/enable_https_domain.sh dangjian.morose.top your@email.com
# 可选：
#   SKIP_DNS_CHECK=1 bash scripts/enable_https_domain.sh dangjian.morose.top your@email.com
#
# 前置条件：
#   1. 域名服务商已添加 A 记录：dangjian.morose.top -> 64.83.11.147
#   2. 云服务器安全组放行 TCP 80/443
#   3. 已安装 certbot（apt-get install -y certbot）
set -euo pipefail

DOMAIN="${1:-dangjian.morose.top}"
EMAIL="${2:-}"
PUBLIC_IP="${PUBLIC_IP:-$(curl -sS -m 10 https://api.ipify.org || true)}"
WEBROOT="/var/www/letsencrypt"
CONF="/usr/local/nginx/conf/vhost/party-domain.conf"
CERT="/etc/letsencrypt/live/$DOMAIN/fullchain.pem"
KEY="/etc/letsencrypt/live/$DOMAIN/privkey.pem"

if [ -z "$EMAIL" ]; then
  echo "用法：bash scripts/enable_https_domain.sh dangjian.morose.top your@email.com"
  exit 1
fi

if ! command -v certbot >/dev/null 2>&1; then
  echo "未安装 certbot，请先执行：apt-get install -y certbot"
  exit 1
fi

RESOLVED="$(getent ahostsv4 "$DOMAIN" | awk 'NR==1{print $1}' || true)"
if [ "${SKIP_DNS_CHECK:-0}" != "1" ] && [ "$RESOLVED" != "$PUBLIC_IP" ]; then
  cat <<EOF
DNS 尚未指向本服务器，暂不修改 Nginx。

请先在域名服务商添加：
  记录类型：A
  主机记录：dangjian
  记录值：$PUBLIC_IP
  TTL：600

当前解析：${RESOLVED:-无}
服务器公网 IP：$PUBLIC_IP

解析生效后重新执行：
  bash scripts/enable_https_domain.sh $DOMAIN $EMAIL

如使用 Cloudflare 代理（橙云），可跳过 DNS 校验：
  SKIP_DNS_CHECK=1 bash scripts/enable_https_domain.sh $DOMAIN $EMAIL
EOF
  exit 1
fi

mkdir -p "$WEBROOT/.well-known/acme-challenge"
chmod -R 755 "$WEBROOT"

# 1. 临时 HTTP 配置，用于 ACME 校验
if [ ! -f "$CERT" ]; then
  cat > "$CONF" <<EOF
server {
    listen 80;
    server_name $DOMAIN;
    location ^~ /.well-known/acme-challenge/ {
        root $WEBROOT;
        allow all;
    }
    location / {
        return 404;
    }
}
EOF
  nginx -t
  nginx -s reload

  echo "==> 申请 Let's Encrypt 证书：$DOMAIN"
  certbot certonly --webroot -w "$WEBROOT" -d "$DOMAIN" \
    --email "$EMAIL" --agree-tos --non-interactive --keep-until-expiring
fi

# 2. 正式 HTTPS 配置
cat > "$CONF" <<EOF
# $DOMAIN —— 党员发展智能管理系统 HTTPS 站点
# 由 scripts/enable_https_domain.sh 生成

server {
    listen 80;
    server_name $DOMAIN;

    location ^~ /.well-known/acme-challenge/ {
        root $WEBROOT;
        allow all;
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl;
    http2 on;
    server_name $DOMAIN;

    ssl_certificate     $CERT;
    ssl_certificate_key $KEY;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:HIGH:!aNULL:!MD5:!RC4:!DHE;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;

    add_header Strict-Transport-Security "max-age=31536000" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'self'" always;

    charset utf-8;
    server_tokens off;
    client_max_body_size 25m;

    root  /var/www/party;
    index index.html;

    gzip on;
    gzip_vary on;
    gzip_comp_level 6;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/javascript application/json image/svg+xml;

    access_log /usr/local/nginx/logs/party.https.access.log;
    error_log  /usr/local/nginx/logs/party.https.error.log warn;

    # 登录接口限流
    location = /api/v1/auth/login {
        limit_req zone=party_login burst=5 nodelay;
        proxy_pass http://127.0.0.1:8080/api/v1/auth/login;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_connect_timeout 10s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_connect_timeout 10s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location ~* \.(?:css|js|png|jpe?g|svg|woff2?|ico)$ {
        expires 7d;
        access_log off;
        try_files \$uri =404;
    }

    location = /index.html {
        expires -1;
    }

    location ~ /\\. {
        deny all;
    }
}
EOF

# 3. 证书续期后自动 reload
mkdir -p /etc/letsencrypt/renewal-hooks/deploy
cat > /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh <<'HOOK'
#!/bin/sh
nginx -s reload
HOOK
chmod +x /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh

nginx -t
nginx -s reload

echo "完成：https://$DOMAIN/"
echo "证书：$CERT"
echo "续期：certbot renew --dry-run"