# -*- coding: utf-8 -*-
"""
菜单 component 与前端视图文件的存在性校验。

动态路由由「数据库菜单表 -> component 字符串 -> import.meta.glob('../views/**/*.vue')」解析。
若某个菜单的 component 在前端不存在对应 .vue，路由将加载失败。
构建工具不会发现这类问题（glob 是运行时的），因此单独校验。

用法: python docs/menu_view_crosscheck.py   (需后端运行在 8080)
"""
import json
import os
import sys
import urllib.parse
import urllib.request

BASE = "http://localhost:8080/api"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VIEWS = os.path.join(ROOT, "frontend", "src", "views")


def call(method, path, token=None, body=None):
    url = BASE + path
    headers = {"Content-Type": "application/json; charset=utf-8"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        return {"code": -1, "message": str(e)}


# 1) 登录管理员并拉菜单
r = call("POST", "/auth/login", body={"username": "admin", "password": "123456"})
if r.get("code") != 200:
    print("登录失败: %s" % r)
    sys.exit(1)
tok = r["data"]["token"]

tree = call("GET", "/system/menu/tree", tok).get("data") or []

# 2) 递归收集所有 type=C 且 component 非空的菜单
menus = []


def walk(nodes):
    for n in nodes:
        if n.get("menuType") == "C" and n.get("component"):
            menus.append((n.get("menuName"), n.get("path"), n.get("component")))
        if n.get("children"):
            walk(n["children"])


walk(tree)

# 3) 建立前端实际存在的视图文件集合（相对 views/ 的路径，去掉 .vue）
existing = set()
for dirpath, _, files in os.walk(VIEWS):
    for f in files:
        if f.endswith(".vue"):
            rel = os.path.relpath(os.path.join(dirpath, f), VIEWS).replace("\\", "/")
            existing.add(rel[:-4])

print("=" * 74)
print("菜单 component 与前端视图文件校验")
print("=" * 74)
print("后端菜单树中 C 类菜单数:  %d" % len(menus))
print("前端 views 下 .vue 文件数: %d" % len(existing))
print("-" * 74)

missing = []
for name, path, comp in menus:
    if comp in existing:
        continue
    missing.append((name, path, comp))

if missing:
    print("\n[未匹配] 以下菜单的 component 在前端找不到对应 .vue：")
    for name, path, comp in missing:
        print("  %-14s path=%-28s component=%s" % (name, path, comp))
else:
    print("\n全部命中：每一个菜单的 component 都能解析到真实存在的 .vue 文件。")

# 4) 反向：前端有视图但没有任何菜单引用（page 类说明可接受，但值得提示）
used = {c for _, _, c in menus}
unreferenced = sorted(existing - used)
print("\n" + "-" * 74)
print("[提示] 前端存在但未被任何菜单引用的视图（如 profile / error 页属正常）：")
if unreferenced:
    for v in unreferenced:
        print("  %s.vue" % v)
else:
    print("  无")

print("\n" + "=" * 74)
print("结论: %s" % ("全部菜单均可加载" if not missing else "存在 %d 处缺失，会导致路由加载失败" % len(missing)))
print("=" * 74)
sys.exit(0 if not missing else 1)
