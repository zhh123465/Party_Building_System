#!/usr/bin/env python3
"""POC-2: 用 Tesseract 对扫描件/图片做中文 OCR，记录时延与关键词命中。

用法：
  python3 poc/ocr_poc.py /path/to/page.png --keywords 文档说明 需求规格说明书 V2.1
"""
import argparse
import json
import os
import subprocess
import time


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("image")
    parser.add_argument("--keywords", nargs="*", default=[])
    parser.add_argument("--lang", default="chi_sim+eng")
    parser.add_argument("--out", default="poc/results/ocr_poc.json")
    args = parser.parse_args()

    start = time.time()
    proc = subprocess.run(
        ["tesseract", args.image, "stdout", "-l", args.lang, "--psm", "6"],
        capture_output=True, text=True, timeout=120
    )
    latency = int((time.time() - start) * 1000)
    text = proc.stdout
    hits = [k for k in args.keywords if k in text]
    result = {
        "image": os.path.basename(args.image),
        "lang": args.lang,
        "latencyMs": latency,
        "charCount": len(text),
        "keywords": args.keywords,
        "keywordHits": hits,
        "keywordHitRate": round(len(hits) / len(args.keywords), 4) if args.keywords else None,
        "textPreview": text[:800],
        "exitCode": proc.returncode,
    }
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    print(json.dumps({k: v for k, v in result.items() if k != "textPreview"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()