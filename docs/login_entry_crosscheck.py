# -*- coding: utf-8 -*-
"""
登录入口一致性校验
======================================================================
系统**只有一个登录入口** `/login`：登录成功后后端按账号身份下发菜单与数据，
不同身份进入各自的后台。因此这里要钉死的是：

  1. 前端确实只有「一个」登录入口，旧的四身份地址（/login/student 等）只会回落；
  2. frontend/src/utils/loginRoles.js 里定义的身份 roleCode 与数据库 sys_role 一致；
  3. 演示账号确实存在、且其真实角色与配置一致（一键填充/文档里的账号不能是错的）。

配置写错时后果是静默的，所以逐项校验。

检查项：
  [1] 入口收口：只注册一个登录路由，旧子路径回落，登录态失效统一回 /login
  [2] 身份定义：key / roleCode 唯一，四个身份齐备，roleCode 与演示账号均已填写
  [3] 与数据库 sys_role / sys_user 比对：roleCode 存在、演示账号角色匹配

用法：python docs/login_entry_crosscheck.py
数据库连接可用环境变量覆盖：AAS_DB_NAME / AAS_DB_USER / AAS_DB_PASS / AAS_MYSQL_BIN
"""

import os
import re
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROLES_JS = os.path.join(ROOT, "frontend", "src", "utils", "loginRoles.js")
ROUTER_JS = os.path.join(ROOT, "frontend", "src", "router", "index.js")
LAYOUT_VUE = os.path.join(ROOT, "frontend", "src", "layout", "index.vue")
REQUEST_JS = os.path.join(ROOT, "frontend", "src", "utils", "request.js")

DB_NAME = os.environ.get("AAS_DB_NAME", "academic_affairs")
DB_USER = os.environ.get("AAS_DB_USER", "root")
DB_PASS = os.environ.get("AAS_DB_PASS", "123456")

ok = 0
fail = 0


def check(name, passed, detail=""):
    global ok, fail
    if passed:
        ok += 1
        print("  [PASS] %s" % name)
    else:
        fail += 1
        print("  [FAIL] %s%s" % (name, ("  -> " + detail) if detail else ""))


def find_mysql():
    for c in (
        os.environ.get("AAS_MYSQL_BIN"),
        shutil.which("mysql"),
        r"D:/SoftWare/MySQL/MySQL Server 9.0/bin/mysql.exe",
        r"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe",
    ):
        if c and os.path.exists(c):
            return c
    return None


def query(sql):
    """返回 [[col, ...], ...]"""
    mysql = find_mysql()
    if not mysql:
        return None
    cmd = [mysql, "-u" + DB_USER, "-p" + DB_PASS, "-N", "-B",
           "--default-character-set=utf8mb4", "-e", sql]
    try:
        p = subprocess.run(cmd, capture_output=True, text=True,
                           encoding="utf-8", errors="ignore", timeout=30)
    except Exception:
        return None
    if p.returncode != 0:
        return None
    rows = []
    for line in (p.stdout or "").splitlines():
        line = line.rstrip("\n")
        if line.strip() == "":
            continue
        rows.append(line.split("\t"))
    return rows


# ---------- 1. 解析 loginRoles.js ----------
if not os.path.exists(ROLES_JS):
    print("找不到 %s" % ROLES_JS)
    sys.exit(1)

src = open(ROLES_JS, encoding="utf-8").read()
entries = []
for block in re.split(r"\bkey:\s*'", src)[1:]:
    key = block.split("'", 1)[0]
    role = re.search(r"roleCode:\s*'([^']+)'", block)
    demo = re.search(r"demoUsername:\s*'([^']+)'", block)
    label = re.search(r"\blabel:\s*'([^']+)'", block)
    entries.append({
        "key": key,
        "roleCode": role.group(1) if role else "",
        "demoUsername": demo.group(1) if demo else "",
        "label": label.group(1) if label else "",
    })

login_path = re.search(r"export\s+const\s+LOGIN_PATH\s*=\s*'([^']+)'", src)
login_path = login_path.group(1) if login_path else ""
home_path = re.search(r"export\s+const\s+HOME_PATH\s*=\s*'([^']+)'", src)
home_path = home_path.group(1) if home_path else ""

print("=" * 70)
print("登录入口一致性校验（单一入口 /login）")
print("=" * 70)

print("\n[1] 四个身份定义（frontend/src/utils/loginRoles.js）")
for e in entries:
    print("      %-6s  roleCode=%-14s 演示账号=%s"
          % (e["label"], e["roleCode"], e["demoUsername"]))
print("      数量：%d" % len(entries))

print("\n[2] 入口收口检查（只允许一个登录入口）")
rsrc = open(ROUTER_JS, encoding="utf-8").read()
lsrc = open(LAYOUT_VUE, encoding="utf-8").read()
qsrc = open(REQUEST_JS, encoding="utf-8").read()

check("loginRoles.js 定义了唯一登录入口 LOGIN_PATH = /login",
      login_path == "/login", "实际：%r" % login_path)
check("loginRoles.js 定义了登录后落地页 HOME_PATH",
      bool(home_path), "未定义 HOME_PATH")
check("已移除四入口派生函数 loginPathOf()", "export function loginPathOf" not in src)
check("已移除四入口常量 DEFAULT_LOGIN_PATH", "DEFAULT_LOGIN_PATH" not in src)
check("router 只注册一个登录路由（component: LoginView 仅 1 处）",
      rsrc.count("component: LoginView") == 1,
      "出现 %d 处" % rsrc.count("component: LoginView"))
check("router 不再按 LOGIN_ROLES 派生登录路由", "LOGIN_ROLES.map" not in rsrc)
check("旧地址 /login/xxx（student、admin 等）回落唯一入口",
      "rest(.*)" in rsrc and "redirect: LOGIN_PATH" in rsrc)
check("路由守卫按登录路径前缀判断（含子路径）", "isLoginPath(to.path)" in rsrc)
check("未登录访问受保护页面会带 redirect 跳到唯一入口",
      "${LOGIN_PATH}?redirect=" in rsrc)
check("Layout 退出登录回唯一入口", "LOGIN_PATH" in lsrc and "loginPathOfRoleCode" not in lsrc)
check("请求拦截器 401 后回唯一入口", "LOGIN_PATH" in qsrc and "loginPathOfRoleCode" not in qsrc)

print("\n[3] 身份定义唯一性与完整性")
keys = [e["key"] for e in entries]
codes = [e["roleCode"] for e in entries]
check("身份 key 无重复", len(keys) == len(set(keys)),
      "重复：%s" % [k for k in keys if keys.count(k) > 1])
check("roleCode 无重复（一个身份只能有一条配置）",
      len(codes) == len(set(codes)),
      "重复：%s" % [c for c in codes if codes.count(c) > 1])
check("roleCode / 演示账号均已填写",
      all(e["roleCode"] and e["demoUsername"] for e in entries))
check("四个身份齐备（STUDENT / HEAD_TEACHER / ACADEMIC / ADMIN）",
      sorted(codes) == sorted(["STUDENT", "HEAD_TEACHER", "ACADEMIC", "ADMIN"]),
      "实际：%s" % sorted(codes))

# ---------- 4. 与数据库比对 ----------
print("\n[4] 与数据库 sys_role / sys_user 比对")
mysql = find_mysql()
if not mysql:
    print("      [SKIP] 未找到 mysql 客户端，跳过数据库校验")
    print("             可用 AAS_MYSQL_BIN 环境变量指定路径")
else:
    role_rows = query("SELECT role_code FROM %s.sys_role;" % DB_NAME)
    if role_rows is None:
        print("      [SKIP] 无法连接数据库 %s，跳过" % DB_NAME)
    else:
        db_codes = set(r[0] for r in role_rows)
        print("      数据库角色码：%s" % ", ".join(sorted(db_codes)))
        for e in entries:
            check("身份 [%s] 的 roleCode '%s' 在 sys_role 中存在"
                  % (e["key"], e["roleCode"]), e["roleCode"] in db_codes,
                  "数据库无此角色码")

        names = ",".join("'%s'" % e["demoUsername"] for e in entries)
        user_rows = query(
            "SELECT u.username, u.real_name, IFNULL(r.role_code,'(无角色)') "
            "FROM %s.sys_user u "
            "LEFT JOIN %s.sys_user_role ur ON ur.user_id = u.id "
            "LEFT JOIN %s.sys_role r ON r.id = ur.role_id "
            "WHERE u.username IN (%s);" % (DB_NAME, DB_NAME, DB_NAME, names)
        ) or []
        user_map = {r[0]: (r[1] if len(r) > 1 else "", r[2] if len(r) > 2 else "") for r in user_rows}
        for e in entries:
            u = user_map.get(e["demoUsername"])
            if not u:
                check("演示账号 %s 存在且角色匹配" % e["demoUsername"], False, "sys_user 中不存在")
                continue
            check("演示账号 %-8s (%s) 角色匹配 -> %s"
                  % (e["demoUsername"], u[0], e["roleCode"]),
                  u[1] == e["roleCode"], "实际角色为 %s" % u[1])

# ---------- 5. 汇总 ----------
print("\n[5] 登录地址")
print("      唯一登录入口  : %s" % login_path)
print("      登录后落地页  : %s（内容与左侧菜单按账号身份区分）" % home_path)
print("      旧地址回落    : /login/student、/login/headteacher、"
      "/login/academic、/login/admin -> %s" % login_path)

print("\n" + "=" * 70)
print("通过 %d 项 / 失败 %d 项" % (ok, fail))
print("=" * 70)
sys.exit(0 if fail == 0 else 1)
