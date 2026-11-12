#!/usr/bin/env python3
"""整体联调冒烟测试：覆盖登录、权限、台账、材料、复核、预警、报表、档案、审计。

用法：
  python3 scripts/smoke_test.py
  BASE_URL=http://64.83.11.147 python3 scripts/smoke_test.py
"""
import json
import os
import sys

import requests

BASE = os.environ.get("BASE_URL", "http://127.0.0.1:8080").rstrip("/")
TIMEOUT = 30
results = []


def check(name, cond, detail=""):
    results.append((name, bool(cond), detail))
    print(("[PASS] " if cond else "[FAIL] ") + name + ("  " + detail if detail else ""))
    return cond


def login(username, password="Party@2026"):
    r = requests.post(f"{BASE}/api/v1/auth/login", json={"username": username, "password": password}, timeout=TIMEOUT)
    body = r.json()
    assert r.status_code == 200 and body.get("code") == 0, body
    return body["data"]["token"], body["data"]["user"]


def get(path, token=None):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    return requests.get(BASE + path, headers=headers, timeout=TIMEOUT)


def main():
    health = get("/api/v1/health")
    check("健康检查", health.status_code == 200 and health.json().get("code") == 0)

    admin, admin_user = login("admin")
    assistant, assistant_user = login("assistant")
    student, student_user = login("student")
    check("三类角色登录", admin_user["roleCode"] == "R1" and assistant_user["roleCode"] == "R3"
          and student_user["roleCode"] == "R4")

    r = get("/api/v1/persons?page=1&size=10", admin)
    check("管理员人员台账", r.status_code == 200 and r.json()["data"]["total"] >= 6)

    r = get("/api/v1/persons?page=1&size=10", student)
    items = r.json()["data"]["items"]
    check("学生仅本人台账", r.status_code == 200 and len(items) == 1 and items[0]["personId"] == 1)

    r = get("/api/v1/persons/2", student)
    check("学生越权访问他人", r.status_code == 403)

    r = get("/api/v1/persons/6", assistant)
    check("助理跨支部访问", r.status_code == 403)

    r = get("/api/v1/persons/1/materials", student)
    check("学生本人材料清单", r.status_code == 200 and len(r.json()["data"]) >= 1)

    r = get("/api/v1/materials/1", admin)
    check("材料详情与报告", r.status_code == 200 and r.json()["data"]["material"]["materialId"] == 1)

    r = get("/api/v1/reviews/pending?page=1&size=10", admin)
    check("待复核列表", r.status_code == 200)

    r = get("/api/v1/warnings/summary", admin)
    check("预警汇总", r.status_code == 200 and "overdue" in r.json()["data"])

    r = get("/api/v1/reports/overview", admin)
    check("报表总览", r.status_code == 200 and r.json()["data"]["totalPersons"] >= 6)

    r = get("/api/v1/reports/compliance", admin)
    check("合规指数", r.status_code == 200 and len(r.json()["data"]) >= 6)

    r = get("/api/v1/reports/compliance/heatmap", admin)
    check("合规热力图", r.status_code == 200 and len(r.json()["data"]) >= 1)

    r = get("/api/v1/persons/6/archive/tree", admin)
    check("电子档案目录树", r.status_code == 200 and len(r.json()["data"]["stages"]) >= 1)

    r = get("/api/v1/materials/search?keyword=%E5%85%A5%E5%85%9A%E5%8A%A8%E6%9C%BA&page=1&size=5", admin)
    check("材料全文检索", r.status_code == 200)

    r = get("/api/v1/audit-logs?page=1&size=5", admin)
    check("审计日志查询", r.status_code == 200 and r.json()["data"]["total"] >= 1)

    r = get("/api/v1/export-logs?page=1&size=5", admin)
    check("导出记录查询", r.status_code == 200)

    r = get("/api/v1/poc/config")
    check("POC 接口未登录拒绝", r.status_code == 401)

    r = get("/api/v1/poc/config", student)
    check("POC 接口学生拒绝", r.status_code == 403)

    r = get("/api/v1/audit-logs?page=1&size=5", student)
    check("审计日志仅 R1", r.status_code == 403)

    failed = [x for x in results if not x[1]]
    print("\n" + "=" * 60)
    print(f"总计 {len(results)} 项，通过 {len(results) - len(failed)} 项，失败 {len(failed)} 项")
    print("=" * 60)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())