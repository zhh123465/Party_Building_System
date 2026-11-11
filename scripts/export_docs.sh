#!/usr/bin/env bash
# 将 docs/export 下的 Markdown（Mermaid 已转为 PNG）导出为 Word 与 PDF。
# 依赖：pandoc、wkhtmltopdf、中文字体（Noto CJK）。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
EXPORT="$ROOT/docs/export"
CSS="$ROOT/scripts/export.css"

cd "$ROOT"

for md in "$EXPORT"/*.md; do
  base="$(basename "$md" .md)"
  echo "==> $base"

  pandoc "$md" \
    -o "$EXPORT/$base.docx" \
    --toc --toc-depth=2 \
    --resource-path="$EXPORT" \
    2>&1 | sed 's/^/    /'

  pandoc "$md" \
    -o "/tmp/opencode-export-$base.html" \
    --standalone --toc --toc-depth=2 \
    --resource-path="$EXPORT" \
    --css="$CSS" \
    --embed-resources \
    --metadata title=" " \
    2>&1 | sed 's/^/    /'

  wkhtmltopdf \
    --enable-local-file-access \
    --page-size A4 \
    --margin-top 16mm --margin-bottom 16mm \
    --margin-left 14mm --margin-right 14mm \
    --footer-center "第 [page] 页 / 共 [topage] 页" \
    --footer-font-size 8 \
    --encoding utf-8 \
    "/tmp/opencode-export-$base.html" \
    "$EXPORT/$base.pdf" \
    >/dev/null 2>&1

  echo "    -> $base.docx / $base.pdf"
done

echo "完成：$EXPORT"