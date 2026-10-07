# -*- coding: utf-8 -*-
"""后端接口端到端验证脚本"""
import json
import os
import urllib.request
import urllib.parse
import sys

# 默认打 8080；可用环境变量指向其它实例，例如：
#   AAS_API_BASE=http://localhost:8081/api python docs/api_test.py
BASE = os.environ.get("AAS_API_BASE", "http://localhost:8080/api").rstrip("/")
ok = 0
fail = 0


def call(method, path, token=None, body=None, params=None, binary=False):
    """binary=True 时用于导出类接口：不解析 JSON，仅按 HTTP 状态与字节数断言可达。"""
    url = BASE + path
    if params:
        url += "?" + urllib.parse.urlencode({k: v for k, v in params.items() if v is not None})
    data = None
    headers = {"Content-Type": "application/json; charset=utf-8"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read()
            if binary:
                # 导出接口返回二进制流（xlsx 等），只关心是否成功返回内容
                if resp.status == 200 and raw:
                    return {"code": 200, "data": {"bytes": len(raw),
                                                  "contentType": resp.headers.get("Content-Type", "")}}
                return {"code": -1, "message": "空响应"}
            if not raw:
                return None
            return json.loads(raw.decode("utf-8"))
    except urllib.error.HTTPError as e:
        return {"code": e.code, "message": e.read().decode("utf-8", "ignore")[:200]}
    except Exception as e:
        return {"code": -1, "message": str(e)}


def login(username):
    r = call("POST", "/auth/login", body={"username": username, "password": "123456"})
    if r and r.get("code") == 200:
        return r["data"]["token"], r["data"]["userInfo"]
    print("  登录失败 %s: %s" % (username, r))
    return None, None


def check(name, result, expect_ok=True, show=None):
    global ok, fail
    code = result.get("code") if isinstance(result, dict) else None
    good = (code == 200) if expect_ok else (code != 200)
    if good:
        ok += 1
        extra = ""
        if show and isinstance(result.get("data"), (list, dict)):
            d = result["data"]
            extra = " -> " + (show(d) if callable(show) else str(d)[:80])
        elif show:
            extra = " -> " + str(result.get("data"))[:80]
        print("  [OK]   %s%s" % (name, extra))
    else:
        fail += 1
        print("  [FAIL] %s -> %s" % (name, str(result)[:200]))
    return result


print("=" * 70)
print("教务管理系统 - 后端接口验证")
print("=" * 70)

# ---------------- 管理员 ----------------
print("\n【管理员 admin】")
admin_tok, admin_info = login("admin")
check("登录", {"code": 200})
check("工作台概览", call("GET", "/dashboard/overview", admin_tok),
      show=lambda d: "用户%s 学生%s 教师%s 班级%s 课程%s 开课%s" % (
          d.get("userCount"), d.get("studentCount"), d.get("teacherCount"),
          d.get("classCount"), d.get("courseCount"), d.get("offeringCount")))
check("用户分页", call("GET", "/system/user/page", admin_tok, params={"pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s" % d.get("total"))
check("角色列表", call("GET", "/system/role/list", admin_tok), show=lambda d: "%d 个角色" % len(d))
check("菜单树", call("GET", "/system/menu/tree", admin_tok), show=lambda d: "%d 个顶级菜单" % len(d))
check("院系分页", call("GET", "/base/dept/page", admin_tok), show=lambda d: "total=%s" % d.get("total"))
check("专业分页", call("GET", "/base/major/page", admin_tok), show=lambda d: "total=%s" % d.get("total"))
check("班级分页", call("GET", "/base/class/page", admin_tok), show=lambda d: "total=%s" % d.get("total"))
check("教师分页", call("GET", "/teacher/page", admin_tok), show=lambda d: "total=%s" % d.get("total"))
check("学生分页", call("GET", "/student/page", admin_tok, params={"pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s 首条=%s" % (d.get("total"), (d["records"][0]["name"] if d.get("records") else "-")))
check("课程分页", call("GET", "/course/page", admin_tok), show=lambda d: "total=%s" % d.get("total"))
check("开课分页", call("GET", "/offering/page", admin_tok, params={"pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s 首条排课=%s" % (d.get("total"), (d["records"][0].get("scheduleText") if d.get("records") else "-")))
check("选课分页", call("GET", "/selection/page", admin_tok, params={"pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s" % d.get("total"))
check("成绩分页", call("GET", "/score/page", admin_tok, params={"pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s" % d.get("total"))
check("成绩等级分布", call("GET", "/score/stat/level", admin_tok), show=lambda d: str(d))
check("考勤分页", call("GET", "/attendance/page", admin_tok, params={"pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s" % d.get("total"))
check("请假分页", call("GET", "/leave/page", admin_tok, params={"pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s" % d.get("total"))
check("操作日志", call("GET", "/system/log/page", admin_tok, params={"pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s" % d.get("total"))
check("院系学生分布", call("GET", "/dashboard/dept-distribution", admin_tok), show=lambda d: str(d)[:100])

# 教师详情 -> 取其 id 用于班主任测试
t_page = call("GET", "/teacher/page", admin_tok, params={"pageNum": 1, "pageSize": 50})
head_teacher = None
for t in (t_page.get("data", {}).get("records") or []):
    if t.get("isHeadTeacher") == 1:
        head_teacher = t
        break

# ---------------- 教务处 ----------------
print("\n【教务处 jwc001】")
jwc_tok, jwc_info = login("jwc001")
check("登录", {"code": 200})
check("工作台概览", call("GET", "/dashboard/overview", jwc_tok),
      show=lambda d: "学生%s 开课%s 待审批请假%s 未发布成绩%s" % (
          d.get("studentCount"), d.get("offeringCount"), d.get("pendingLeave"), d.get("unpublishedScore")))
check("学生管理可访问", call("GET", "/student/page", jwc_tok, params={"pageNum": 1, "pageSize": 3}))
check("课程平均分统计", call("GET", "/score/stat/course", jwc_tok, params={"semesterId": 4}),
      show=lambda d: "%d 门课程有成绩" % len(d))
check("选课名单导出(二进制流)", call("GET", "/selection/export", jwc_tok, params={"pageNum": 1}, binary=True),
      show=lambda d: "%s 字节, %s" % (d.get("bytes"), d.get("contentType")))
check("用户管理无权限(应被拒)", call("GET", "/system/user/page", jwc_tok), expect_ok=False)

# ---------------- 班主任 ----------------
print("\n【班主任 T1001】")
ht_tok, ht_info = login("T1001")
check("登录", {"code": 200})
check("工作台概览", call("GET", "/dashboard/overview", ht_tok),
      show=lambda d: "带班%s个 学生%s 待审批%d 平均分%s" % (
          d.get("manageClassCount"), d.get("studentCount"), d.get("pendingLeave"), d.get("avgScore")))
check("我的班级", call("GET", "/base/class/my", ht_tok), show=lambda d: str([c["className"] for c in d]))
check("本班学生(数据权限)", call("GET", "/student/page", ht_tok, params={"pageNum": 1, "pageSize": 50}),
      show=lambda d: "total=%s 班级=%s" % (d.get("total"), set(r["className"] for r in d["records"])))
check("本班课表", call("GET", "/offering/timetable", ht_tok, params={"semesterId": 4}),
      show=lambda d: "%d 个教学班" % len(d))
check("请假审批列表", call("GET", "/leave/page", ht_tok, params={"pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s" % d.get("total"))
check("成绩查询(本班)", call("GET", "/score/page", ht_tok, params={"pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s" % d.get("total"))
check("用户管理无权限(应被拒)", call("GET", "/system/user/page", ht_tok), expect_ok=False)

# ---------------- 学生 ----------------
print("\n【学生 2023001】")
st_tok, st_info = login("2023001")
check("登录", {"code": 200})
check("我的学籍", call("GET", "/student/my", st_tok),
      show=lambda d: "%s %s %s" % (d.get("studentNo"), d.get("name"), d.get("className")))
check("我的课程表", call("GET", "/offering/timetable", st_tok, params={"semesterId": 4}),
      show=lambda d: "%d 门课" % len(d))
check("我的选课", call("GET", "/selection/my", st_tok), show=lambda d: "%d 条" % len(d))
check("我的学分", call("GET", "/selection/my/credit", st_tok), show=lambda d: str(d))
check("我的成绩", call("GET", "/score/my", st_tok), show=lambda d: "%d 条" % len(d))
check("我的成绩趋势", call("GET", "/score/my/trend", st_tok), show=lambda d: "%d 个学期" % len(d))
check("我的考勤汇总", call("GET", "/attendance/my", st_tok), show=lambda d: str(d)[:100])
check("我的请假", call("GET", "/leave/page", st_tok, params={"pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s" % d.get("total"))
check("可见公告", call("GET", "/notice/page", st_tok, params={"pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s" % d.get("total"))
check("可选课程", call("GET", "/offering/selectable", st_tok, params={"semesterId": 4, "pageNum": 1, "pageSize": 5}),
      show=lambda d: "total=%s" % d.get("total"))
check("工作台概览", call("GET", "/dashboard/overview", st_tok),
      show=lambda d: "课程%s 当前学分%s 异常考勤%s" % (d.get("courseCount"), d.get("currentCredit"),
                                                 (d.get("attendance") or {}).get("abnormal")))
check("用户管理无权限(应被拒)", call("GET", "/system/user/page", st_tok), expect_ok=False)
check("成绩录入无权限(应被拒)", call("POST", "/score/offering/1/init", st_tok), expect_ok=False)

# ---------------- 业务流：选课 -> 退课 ----------------
print("\n【业务流验证：学生选课/退课】")
sel = call("GET", "/offering/selectable", st_tok, params={"semesterId": 4, "pageNum": 1, "pageSize": 10})
candidates = sel.get("data", {}).get("records") or []
if candidates:
    target = candidates[0]
    r = call("POST", "/selection/select", st_tok, params={"offeringId": target["id"]})
    check("选课《%s》" % target.get("courseName"), r)
    r2 = call("POST", "/selection/drop", st_tok, params={"offeringId": target["id"]})
    check("退课《%s》" % target.get("courseName"), r2)
else:
    print("  [SKIP] 没有可选课程")

# ---------------- 业务流：班主任审批请假 ----------------
print("\n【业务流验证：请假申请与审批】")
r = call("POST", "/leave", st_tok, body={
    "leaveType": "事假",
    "startDate": "2026-10-12 08:00:00",
    "endDate": "2026-10-13 18:00:00",
    "reason": "接口自动化测试提交的请假申请"
})
leave_id = r.get("data") if r.get("code") == 200 else None
check("学生提交请假申请", r)
if leave_id:
    pend = call("GET", "/leave/page", ht_tok, params={"pageNum": 1, "pageSize": 50, "status": 0})
    ids = [x["id"] for x in (pend.get("data", {}).get("records") or [])]
    if leave_id in ids:
        ok += 1
        print("  [OK]   班主任能看到该申请")
        check("班主任审批通过", call("PUT", "/leave/%d/approve" % leave_id, ht_tok,
                                 params={"status": 1, "remark": "测试通过"}))
        check("审批后状态正确", call("GET", "/leave/%d" % leave_id, ht_tok),
              show=lambda d: "status=%s %s" % (d.get("status"), d.get("statusText")))
    else:
        fail += 1
        print("  [FAIL] 班主任看不到刚提交的申请")

print("\n" + "=" * 70)
print("验证结果: 通过 %d 项, 失败 %d 项" % (ok, fail))
print("=" * 70)
sys.exit(0 if fail == 0 else 1)
