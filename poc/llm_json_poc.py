#!/usr/bin/env python3
"""POC-1: 验证模型 JSON 输出、解析成功率与时延。

用法：
  export LLM_BASE_URL=https://tokenrhythm.studio/v1
  export LLM_MODEL=deepseek-v4.1-flash-1017
  export LLM_API_KEY=...
  python3 poc/llm_json_poc.py --rounds 10
"""
import argparse
import json
import os
import statistics
import time
import urllib.request

PROMPTS = [
    '只输出 JSON：{"ok":true,"index":%d}',
    '请输出 JSON：{"ok":true,"index":%d,"message":"pong"}',
]


def call(base_url, model, key, prompt):
    body = json.dumps({
        "model": model,
        "temperature": 0,
        "messages": [{"role": "user", "content": prompt}],
        "response_format": {"type": "json_object"},
    }).encode("utf-8")
    req = urllib.request.Request(
        base_url.rstrip("/") + "/chat/completions",
        data=body,
        headers={"Authorization": "Bearer " + key, "Content-Type": "application/json"},
        method="POST",
    )
    start = time.time()
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    latency = int((time.time() - start) * 1000)
    content = data["choices"][0]["message"]["content"]
    try:
        parsed = json.loads(content)
        valid = True
    except Exception:
        parsed = None
        valid = False
    return {
        "latencyMs": latency,
        "validJson": valid,
        "parsed": parsed,
        "content": content,
        "usage": data.get("usage", {}),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--rounds", type=int, default=10)
    parser.add_argument("--out", default="poc/results/llm_json_poc.json")
    args = parser.parse_args()

    base_url = os.environ.get("LLM_BASE_URL", "https://tokenrhythm.studio/v1")
    model = os.environ.get("LLM_MODEL", "deepseek-v4.1-flash-1017")
    key = os.environ.get("LLM_API_KEY", "")
    if not key:
        raise SystemExit("请先设置 LLM_API_KEY 环境变量")

    results = []
    for i in range(args.rounds):
        prompt = PROMPTS[i % len(PROMPTS)] % i
        try:
            r = call(base_url, model, key, prompt)
            r["index"] = i
            results.append(r)
            print(f"[{i + 1}/{args.rounds}] valid={r['validJson']} latency={r['latencyMs']}ms")
        except Exception as e:
            results.append({"index": i, "validJson": False, "error": str(e)})
            print(f"[{i + 1}/{args.rounds}] ERROR {e}")

    latencies = [r["latencyMs"] for r in results if "latencyMs" in r]
    summary = {
        "model": model,
        "baseUrl": base_url,
        "rounds": args.rounds,
        "validJsonCount": sum(1 for r in results if r.get("validJson")),
        "validRate": round(sum(1 for r in results if r.get("validJson")) / args.rounds, 4),
        "latencyP50Ms": int(statistics.median(latencies)) if latencies else None,
        "latencyP90Ms": int(sorted(latencies)[max(0, int(len(latencies) * 0.9) - 1)]) if latencies else None,
        "latencyMaxMs": max(latencies) if latencies else None,
        "results": results,
    }
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)
    print(json.dumps({k: v for k, v in summary.items() if k != "results"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()