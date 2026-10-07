# -*- coding: utf-8 -*-
"""
前后端接口路径交叉比对。

构建工具不会发现「前端调用了后端不存在的路径」这类问题，
本脚本把 frontend/src/api/index.js 里声明的每个请求路径
与 backend 各 Controller 的 @RequestMapping / @XxxMapping 实际映射做比对。

用法: python docs/api_path_crosscheck.py
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
API_JS = os.path.join(ROOT, "frontend", "src", "api", "index.js")
CTRL_DIR = os.path.join(ROOT, "backend", "src", "main", "java", "com", "aas", "controller")

# 统一前缀（application.yml 中的 context-path，这里是网关层加的 /api）
GLOBAL_PREFIX = "/api"

# ---------- 1. 解析后端 ----------
METHOD_ANN = {
    "GetMapping": "GET",
    "PostMapping": "POST",
    "PutMapping": "PUT",
    "DeleteMapping": "DELETE",
    "PatchMapping": "PATCH",
    "RequestMapping": "*",
}

backend = {}  # (METHOD, path) -> "Class#method"


def norm(p):
    """归一化路径：去掉重复斜杠、去掉末尾斜杠"""
    p = re.sub(r"/+", "/", p)
    if len(p) > 1 and p.endswith("/"):
        p = p[:-1]
    return p or "/"


for fn in sorted(os.listdir(CTRL_DIR)):
    if not fn.endswith("Controller.java"):
        continue
    cls = fn[:-5]
    src = open(os.path.join(CTRL_DIR, fn), encoding="utf-8").read()

    # 类级 @RequestMapping("...")
    m = re.search(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']', src)
    base = m.group(1) if m else ""

    # 逐个方法级注解：
    #   @PostMapping              -> 裸注解，路径即类级 base
    #   @PostMapping("/page")     -> 追加路径
    #   @PostMapping(path="/page")-> 追加路径
    for ann, http in METHOD_ANN.items():
        pattern = r"@" + ann + r"\b\s*(?:\(\s*(?:(?:value|path)\s*=\s*)?[\"']([^\"']*)[\"'])?"
        for mm in re.finditer(pattern, src):
            sub = mm.group(1)  # 可能为 None（裸注解 / 空的类级注解）
            full = norm(base + "/" + sub) if sub else norm(base)
            if norm(sub or "") == norm(base):  # 跳过类级 @RequestMapping 自身
                continue
            backend[(http, full)] = cls
            backend.setdefault(("ALL", full), cls)

# ---------- 2. 解析前端 ----------
front_src = open(API_JS, encoding="utf-8").read()
calls = []  # (line_no, method, path, 原文)

# 1) 常规请求: request.get/post/put/delete/patch('...')   -> URL 在第 2 组，方法在第 1 组
# 2) 下载请求: download('...', params, filename)          -> URL 在第 1 组，一律按 GET 处理
PATTERNS = [
    (re.compile(r"""request\.(get|post|put|delete|patch)\s*\(\s*(`[^`]*`|'[^']*'|"[^"]*")"""), 2, 1),
    (re.compile(r"""\bdownload\s*\(\s*(`[^`]*`|'[^']*'|"[^"]*")"""), 1, None),
]

for i, line in enumerate(front_src.splitlines(), 1):
    for pat, url_grp, method_grp in PATTERNS:
        m = pat.search(line)
        if not m:
            continue
        raw = m.group(url_grp)[1:-1]
        method = m.group(method_grp).upper() if method_grp else "GET"
        # 模板字面量里的 ${...} 替换为占位符，便于与后端 {id} 比对
        path = re.sub(r"\$\{[^}]*\}", "{p}", raw)
        calls.append((i, method, path, raw))

# ---------- 3. 比对 ----------


def match(method, path):
    """精确匹配，或把 {p}/{id} 之类的路径变量视为通配再匹配一次"""
    full = norm(GLOBAL_PREFIX + path) if not path.startswith(GLOBAL_PREFIX) else norm(path)
    if (method, full) in backend or ("ALL", full) in backend:
        return full, True
    # 路径变量通配：把 {xxx} 段替换成 \{[^/]+\}
    pat = re.compile("^" + re.escape(full).replace(r"\{p\}", r"\{[^/]+\}") + "$")
    for (mm, bp) in backend:
        if mm in (method, "ALL") and pat.match(bp):
            return bp, True
    return full, False


print("=" * 74)
print("前后端接口路径交叉比对")
print("=" * 74)
print("后端 Controller 映射总数: %d (含方法级重复)" % len([k for k in backend if k[0] != "ALL"]))
print("前端 API 声明总数:        %d" % len(calls))

missing = []
hit = 0
for line_no, method, path, raw in calls:
    full, ok = match(method, path)
    if ok:
        hit += 1
    else:
        missing.append((line_no, method, full, raw))

print("匹配成功:                %d" % hit)
print("未匹配:                  %d" % len(missing))
print("-" * 74)

if missing:
    print("\n[未匹配明细] 前端调用了后端未声明的路径：")
    for line_no, method, full, raw in missing:
        print("  index.js:%-4d %-6s %-52s (原文: %s)" % (line_no, method, full, raw))
else:
    print("\n全部命中：前端声明的每一个接口路径都能在后端找到对应映射。")

# ---------- 4. 反向：后端有但前端未调用（仅提示，不算失败）----------
print("\n" + "-" * 74)
print("[提示] 后端已实现但前端 API 层未声明的接口（可能属预留能力）：")

def strip_var(p):
    return re.sub(r"\{[^/]+\}", "{}", p)

used = set()
for _, method, path, _ in calls:
    _, ok = match(method, path)
    if ok:
        full = norm(GLOBAL_PREFIX + path) if not path.startswith(GLOBAL_PREFIX) else norm(path)
        used.add(strip_var(full))

unused = []
for (mm, bp), cls in sorted(backend.items(), key=lambda x: (x[0][1], x[0][0])):
    if mm == "ALL":
        continue
    if strip_var(bp) in used:
        continue
    unused.append((mm, bp, cls))

if unused:
    for mm, bp, cls in unused:
        print("  %-6s %-48s %s" % (mm, bp, cls))
else:
    print("  无")

print("\n" + "=" * 74)
print("结论: %s" % ("全部前端接口均可命中后端映射" if not missing else "存在 %d 处未命中，需修复" % len(missing)))
print("=" * 74)
sys.exit(0 if not missing else 1)
