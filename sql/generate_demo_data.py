# -*- coding: utf-8 -*-
"""
教务管理系统 - 演示数据生成脚本
运行: python generate_demo_data.py
输出: ./02_data.sql

说明: 该脚本用于生成规模化的演示数据(200名学生/26门课程/完整课表/选课/成绩/考勤)。
      生成的 02_data.sql 可直接通过 mysql 客户端导入, 不依赖本脚本。
"""
import random
import datetime
import os

random.seed(20261006)
TODAY = datetime.date(2026, 10, 6)
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "02_data.sql")

# 所有演示账号的统一密码: 123456
PWD = "$2a$10$YTbuQrBbZZWPHSEFanqnZuC4NRQpY7nbGlfax2Mdsigb.HbTer4FG"


def q(v):
    """SQL 字面量序列化"""
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, (datetime.date, datetime.datetime)):
        return "'" + v.strftime("%Y-%m-%d %H:%M:%S" if isinstance(v, datetime.datetime) else "%Y-%m-%d") + "'"
    s = str(v).replace("\\", "\\\\").replace("'", "''")
    return "'" + s + "'"


class Builder:
    def __init__(self):
        self.out = []

    def w(self, line=""):
        self.out.append(line)

    def section(self, title):
        self.w()
        self.w("-- " + "=" * 74)
        self.w("--  " + title)
        self.w("-- " + "=" * 74)

    def insert(self, table, cols, rows, batch=500):
        if not rows:
            return
        self.w()
        self.w("INSERT INTO `%s` (%s) VALUES" % (table, ", ".join("`%s`" % c for c in cols)))
        for i in range(0, len(rows), batch):
            chunk = rows[i:i + batch]
            lines = []
            for r in chunk:
                lines.append("  (" + ", ".join(q(v) for v in r) + ")")
            suffix = "," if i + batch < len(rows) else ";"
            self.w(",\n".join(lines) + suffix)

    def text(self):
        return "\n".join(self.out) + "\n"


b = Builder()

# ============================================================================
# 1. 角色
# ============================================================================
b.section("1. 角色数据")
b.w("DELETE FROM `sys_role`;")
roles = [
    (1, "STUDENT", "学生", "SELF", 1, "学生身份: 查看个人信息/课表/成绩, 在线选课, 提交请假"),
    (2, "HEAD_TEACHER", "班主任", "CLASS", 2, "班主任身份: 管理本班学生, 审批本班请假, 录入本班成绩"),
    (3, "ACADEMIC", "教务处", "ALL", 3, "教务处身份: 全校教务管理, 排课, 成绩审核发布, 统计报表"),
    (4, "ADMIN", "系统管理员", "ALL", 4, "管理员身份: 用户/角色/权限/日志等系统管理, 拥有全部权限"),
]
b.insert("sys_role", ["id", "role_code", "role_name", "data_scope", "sort", "description"], roles)

# ============================================================================
# 2. 菜单
# ============================================================================
b.section("2. 菜单权限数据")
b.w("DELETE FROM `sys_menu`;")
# (id, parent, name, type, path, component, perms, icon, sort, visible)
menus = [
    (1, 0, "工作台", "C", "/dashboard", "dashboard/index", "dashboard:view", "Odometer", 1, 1),

    (10, 0, "学籍管理", "M", "/academic", None, None, "School", 10, 1),
    (11, 10, "学生管理", "C", "/academic/student", "academic/student/index", "student:list", "User", 1, 1),
    (17, 10, "教师管理", "C", "/academic/teacher", "academic/teacher/index", "teacher:list", "Postcard", 2, 1),
    (12, 10, "班级管理", "C", "/academic/class", "academic/class/index", "class:list", "OfficeBuilding", 3, 1),
    (13, 10, "院系管理", "C", "/academic/dept", "academic/dept/index", "dept:list", "Grid", 4, 1),
    (14, 10, "专业管理", "C", "/academic/major", "academic/major/index", "major:list", "Collection", 5, 1),
    (15, 10, "学期管理", "C", "/academic/semester", "academic/semester/index", "semester:list", "Calendar", 6, 1),
    (16, 10, "教室管理", "C", "/academic/classroom", "academic/classroom/index", "classroom:list", "Location", 7, 1),

    (110, 11, "学生查询", "F", None, None, "student:query", None, 1, 1),
    (111, 11, "学生新增", "F", None, None, "student:add", None, 2, 1),
    (112, 11, "学生修改", "F", None, None, "student:edit", None, 3, 1),
    (113, 11, "学生删除", "F", None, None, "student:remove", None, 4, 1),
    (114, 11, "学生导入", "F", None, None, "student:import", None, 5, 1),
    (115, 11, "学生导出", "F", None, None, "student:export", None, 6, 1),

    (20, 0, "教学管理", "M", "/teaching", None, None, "Reading", 20, 1),
    (21, 20, "课程管理", "C", "/teaching/course", "teaching/course/index", "course:list", "Notebook", 1, 1),
    (22, 20, "开课安排", "C", "/teaching/offering", "teaching/offering/index", "offering:list", "Tickets", 2, 1),
    (23, 20, "课表查询", "C", "/teaching/timetable", "teaching/timetable/index", "schedule:view", "Clock", 3, 1),
    (24, 20, "在线选课", "C", "/teaching/select", "teaching/select/index", "selection:select", "ShoppingCart", 4, 1),
    (25, 20, "选课管理", "C", "/teaching/selection", "teaching/selection/index", "selection:list", "List", 5, 1),

    (30, 0, "成绩管理", "M", "/score", None, None, "DataLine", 30, 1),
    (31, 30, "成绩录入", "C", "/score/input", "score/input/index", "score:input", "EditPen", 1, 1),
    (32, 30, "我的成绩", "C", "/score/my", "score/my/index", "score:my", "TrendCharts", 2, 1),
    (33, 30, "成绩审核", "C", "/score/audit", "score/audit/index", "score:audit", "CircleCheck", 3, 1),
    (34, 30, "成绩统计", "C", "/score/stat", "score/stat/index", "score:stat", "PieChart", 4, 1),

    (40, 0, "考勤管理", "M", "/attendance", None, None, "Calendar", 40, 1),
    (41, 40, "考勤记录", "C", "/attendance/record", "attendance/record/index", "attendance:list", "Finished", 1, 1),
    (42, 40, "请假审批", "C", "/attendance/approve", "attendance/approve/index", "leave:approve", "Stamp", 2, 1),
    (43, 40, "我的请假", "C", "/attendance/my", "attendance/my/index", "leave:my", "Document", 3, 1),

    (50, 0, "通知公告", "M", "/notice", None, None, "Bell", 50, 1),
    (51, 50, "公告发布", "C", "/notice/manage", "notice/manage/index", "notice:manage", "Promotion", 1, 1),
    (52, 50, "公告列表", "C", "/notice/list", "notice/list/index", "notice:list", "ChatDotSquare", 2, 1),

    (60, 0, "系统管理", "M", "/system", None, None, "Setting", 60, 1),
    (61, 60, "用户管理", "C", "/system/user", "system/user/index", "user:list", "Avatar", 1, 1),
    (62, 60, "角色管理", "C", "/system/role", "system/role/index", "role:list", "Key", 2, 1),
    (63, 60, "菜单管理", "C", "/system/menu", "system/menu/index", "menu:list", "Menu", 3, 1),
    (64, 60, "操作日志", "C", "/system/log", "system/log/index", "log:list", "Document", 4, 1),

    (70, 0, "个人中心", "C", "/profile", "profile/index", "profile:view", "UserFilled", 70, 1),
]
b.insert("sys_menu", ["id", "parent_id", "menu_name", "menu_type", "path", "component", "perms", "icon", "sort", "visible"], menus)

# 角色 -> 菜单
ROLE_MENU = {
    4: [m[0] for m in menus if m[0] != 0],                                    # ADMIN 全部
    3: [1, 10, 11, 17, 12, 13, 14, 15, 16, 110, 111, 112, 113, 114, 115,
        20, 21, 22, 23, 25, 30, 31, 33, 34, 40, 41, 42, 50, 51, 52, 70],
    2: [1, 10, 11, 110, 20, 23, 25, 30, 31, 40, 41, 42, 50, 51, 52, 70],
    1: [1, 20, 23, 24, 30, 32, 40, 43, 50, 52, 70],
}
rows = []
for rid, mids in ROLE_MENU.items():
    for mid in mids:
        rows.append((rid, mid))
b.insert("sys_role_menu", ["role_id", "menu_id"], rows)

# ============================================================================
# 3. 院系 / 专业 / 学期 / 教室
# ============================================================================
b.section("3. 基础数据: 院系/专业/学期/教室")
b.w("DELETE FROM `base_dept`;")
depts = [
    (1, "CS", "计算机学院", "王建国", "010-88880101", "涵盖计算机科学与技术、软件工程、网络工程等专业", 1),
    (2, "EI", "电子信息学院", "刘志强", "010-88880102", "涵盖电子信息工程、通信工程等专业", 2),
    (3, "EM", "经济管理学院", "陈美玲", "010-88880103", "涵盖工商管理、会计学等专业", 3),
    (4, "FL", "外国语学院", "赵雅琴", "010-88880104", "涵盖英语、商务英语等专业", 4),
]
b.insert("base_dept", ["id", "dept_code", "dept_name", "dean", "phone", "description", "sort"], depts)

b.w("DELETE FROM `base_major`;")
majors = [
    (1, "CS01", "计算机科学与技术", 1, "工学学士", 4),
    (2, "CS02", "软件工程", 1, "工学学士", 4),
    (3, "CS03", "网络工程", 1, "工学学士", 4),
    (4, "EI01", "电子信息工程", 2, "工学学士", 4),
    (5, "EI02", "通信工程", 2, "工学学士", 4),
    (6, "EM01", "工商管理", 3, "管理学学士", 4),
    (7, "EM02", "会计学", 3, "管理学学士", 4),
    (8, "FL01", "英语", 4, "文学学士", 4),
]
b.insert("base_major", ["id", "major_code", "major_name", "dept_id", "degree", "duration"], majors)

b.w("DELETE FROM `base_semester`;")
semesters = [
    (1, "2024-2025学年第一学期", "2024-2025", 1, "2024-09-02", "2025-01-17", 0, 2),
    (2, "2024-2025学年第二学期", "2024-2025", 2, "2025-02-24", "2025-07-11", 0, 2),
    (3, "2025-2026学年第一学期", "2025-2026", 1, "2025-09-01", "2026-01-16", 0, 2),
    (4, "2026-2027学年第一学期", "2026-2027", 1, "2026-09-01", "2027-01-15", 1, 1),
]
rows = []
for sid, name, sy, term, sd, ed, cur, st in semesters:
    rows.append((sid, name, sy, term, sd, ed, sd, ed, cur, st))
b.insert("base_semester", ["id", "semester_name", "school_year", "term", "start_date", "end_date",
                           "select_start", "select_end", "is_current", "status"], rows)

b.w("DELETE FROM `base_classroom`;")
buildings = ["第一教学楼", "第二教学楼", "实验楼", "信息楼"]
rooms = []
for i in range(1, 13):
    bidx = (i - 1) // 3
    rtype = "机房" if i in (7, 8) else ("多媒体" if i <= 6 else "普通教室")
    rooms.append((i, "R%03d" % i, "%s-%s%02d" % (buildings[bidx], "ABCD"[bidx], i),
                  buildings[bidx], 60 if i <= 8 else 120, rtype, 1))
b.insert("base_classroom", ["id", "room_code", "room_name", "building", "capacity", "room_type", "status"], rooms)

# ============================================================================
# 4. 教师 (1-12 为班主任, 13-22 为普通教师)
# ============================================================================
b.section("4. 教师数据")
surnames = "王李张刘陈杨黄赵周吴徐孙马朱胡郭何高林罗郑梁谢宋唐许韩冯邓曹彭曾肖田董袁潘于蒋蔡余杜叶程苏魏吕丁任沈姚卢姜崔钟谭陆汪范金石廖贾夏韦付方白邹孟熊秦邱江尹薛闫段雷侯龙史陶黎贺顾毛郝龚邵万钱严覃武戴莫孔向汤"
given1 = "伟芳娜秀英敏静丽强磊军洋勇艳杰娟涛明超秀霞平刚桂英建华文军晓东志强建国春梅玉兰金凤秀兰海燕小明志明俊杰雪梅晓丽国强建华文博雨欣嘉怡子涵浩然欣怡梓涵诗涵一诺宇轩浩宇欣妍梦琪思远雅静"
given2 = "华明强军伟杰峰辉磊涛勇军平刚毅斌嘉俊杰豪宇轩然浩宁康乐安泰和顺福禄祥瑞春夏秋冬梅兰竹菊"

teacher_rows = []
user_rows = []
user_role_rows = []


def gen_name():
    return random.choice(surnames) + random.choice(given1) + (random.choice(given2) if random.random() < 0.35 else "")


teachers = []
titles = ["助教", "讲师", "副教授", "教授"]
edus = ["本科", "硕士", "博士"]
for i in range(1, 21):
    is_head = 1 if i <= 12 else 0
    dept_id = [1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 3, 3, 1, 1, 1, 2, 2, 3, 3, 4][i - 1]
    name = gen_name()
    teachers.append({
        "id": i, "no": "T%04d" % (1000 + i), "name": name, "dept": dept_id,
        "title": titles[min(3, (i % 4))], "edu": edus[i % 3], "head": is_head,
        "gender": 1 if i % 3 else 2,
    })

# 用户: 1=admin, 2-3=教务处, 4-15=班主任教师, 16...=学生
user_rows.append((1, "admin", PWD, "系统管理员", "ADMIN", 1, "13800000001", "admin@aas.edu.cn", 1))
user_rows.append((2, "jwc001", PWD, "张国华", "TEACHER", 1, "13800000002", "jwc001@aas.edu.cn", 1))
user_rows.append((3, "jwc002", PWD, "李慧敏", "TEACHER", 2, "13800000003", "jwc002@aas.edu.cn", 1))
user_role_rows.append((1, 4))
user_role_rows.append((2, 3))
user_role_rows.append((3, 3))

uid = 4
for t in teachers:
    if t["head"]:
        t["user_id"] = uid
        user_rows.append((uid, t["no"], PWD, t["name"], "TEACHER", t["gender"],
                          "139%08d" % random.randint(0, 99999999), t["no"].lower() + "@aas.edu.cn", 1))
        user_role_rows.append((uid, 2))
        uid += 1
    else:
        t["user_id"] = None

rows = []
for t in teachers:
    rows.append((t["id"], t["user_id"], t["no"], t["name"], t["gender"],
                 datetime.date(1975 + (t["id"] % 20), 1 + (t["id"] % 12), 1 + (t["id"] % 27)),
                 "139%08d" % random.randint(0, 99999999), t["no"].lower() + "@aas.edu.cn",
                 None, t["dept"], t["title"], t["edu"],
                 datetime.date(2010 + (t["id"] % 14), 7, 1), t["head"], 1))
b.w("DELETE FROM `tea_teacher`;")
b.insert("tea_teacher", ["id", "user_id", "teacher_no", "name", "gender", "birth_date", "phone",
                         "email", "id_card", "dept_id", "title", "education", "hire_date",
                         "is_head_teacher", "status"], rows)

# ============================================================================
# 5. 班级
# ============================================================================
b.section("5. 班级数据")
class_defs = [
    (1, "CS2301", "计算机科学与技术2301班", 1, "2023", 1),
    (2, "CS2302", "计算机科学与技术2302班", 1, "2023", 2),
    (3, "CS2401", "计算机科学与技术2401班", 1, "2024", 3),
    (4, "SE2301", "软件工程2301班", 2, "2023", 4),
    (5, "SE2401", "软件工程2401班", 2, "2024", 5),
    (6, "SE2402", "软件工程2402班", 2, "2024", 6),
    (7, "NE2301", "网络工程2301班", 3, "2023", 7),
    (8, "EI2301", "电子信息工程2301班", 4, "2023", 8),
    (9, "TE2301", "通信工程2301班", 5, "2023", 9),
    (10, "BA2301", "工商管理2301班", 6, "2023", 10),
    (11, "AC2301", "会计学2301班", 7, "2023", 11),
    (12, "EN2301", "英语2301班", 8, "2023", 12),
]
cls_rows = []
for cid, code, name, major, grade, head in class_defs:
    major_dept = {1: 1, 2: 1, 3: 1, 4: 2, 5: 2, 6: 3, 7: 3, 8: 4}[major]
    cls_rows.append((cid, code, name, major, grade, head, int(grade), 0, "R%03d" % ((cid % 12) + 1), 1))
b.w("DELETE FROM `base_class`;")
b.insert("base_class", ["id", "class_code", "class_name", "major_id", "grade", "head_teacher_id",
                        "enrollment_year", "student_count", "classroom", "status"], cls_rows)

# ============================================================================
# 6. 学生
# ============================================================================
b.section("6. 学生数据")
CLASS_STUDENT_COUNT = {1: 20, 2: 18, 3: 18, 4: 18, 5: 16, 6: 16, 7: 16, 8: 16, 9: 15, 10: 15, 11: 15, 12: 15}
politics = ["群众", "共青团员", "共青团员", "共青团员", "中共党员", "预备党员"]
CITIES = ["北京", "上海", "广州", "深圳", "杭州", "南京", "武汉", "成都"]
DISTRICTS = ["海淀", "朝阳", "浦东", "天河", "西湖", "鼓楼", "洪山", "武侯"]
STREETS = ["文化", "建设", "人民", "解放", "中山"]
GUARDIAN_SUR = ["张", "李", "王", "刘", "陈", "杨", "黄", "赵"]
GUARDIAN_GIVEN = ["建国", "秀兰", "志强", "桂芳", "海涛", "玉梅", "国强", "淑芬"]
stu_rows = []
class_students = {}
sno = 1
for cid, code, name, major, grade, head in class_defs:
    n = CLASS_STUDENT_COUNT[cid]
    ids = []
    major_dept = {1: 1, 2: 1, 3: 1, 4: 2, 5: 2, 6: 3, 7: 3, 8: 4}[major]
    for k in range(n):
        sid = sno
        s_no = "%s%03d" % (grade, sno)
        s_name = gen_name()
        gender = 1 if random.random() < 0.55 else 2
        birth = datetime.date(int(grade) - 19, random.randint(1, 12), random.randint(1, 28))
        uid_x = uid
        user_rows.append((uid_x, s_no, PWD, s_name, "STUDENT", gender,
                          "15%09d" % random.randint(0, 999999999), s_no + "@stu.aas.edu.cn", 1))
        user_role_rows.append((uid_x, 1))
        uid += 1
        enrol = datetime.date(int(grade), 9, 1)
        address = "%s市%s区%s路%d号" % (random.choice(CITIES), random.choice(DISTRICTS),
                                        random.choice(STREETS), random.randint(1, 200))
        stu_rows.append((sid, uid_x, s_no, s_name, gender, birth,
                         "110101%d%02d%02d%04d" % (1990 + (int(grade) - 2000 + 10), random.randint(1, 12),
                                                   random.randint(1, 28), random.randint(0, 9999)),
                         "15%09d" % random.randint(0, 999999999),
                         s_no + "@stu.aas.edu.cn", major_dept, major, cid, enrol,
                         random.choice(politics), address,
                         random.choice(GUARDIAN_SUR) + random.choice(GUARDIAN_GIVEN),
                         "138%08d" % random.randint(0, 99999999),
                         "%d号楼%d%02d" % (random.randint(1, 8), random.randint(1, 6), random.randint(1, 30)),
                         None, 1))
        ids.append(sid)
        sno += 1
    class_students[cid] = ids
b.w("DELETE FROM `stu_student`;")
b.insert("stu_student", ["id", "user_id", "student_no", "name", "gender", "birth_date", "id_card",
                         "phone", "email", "dept_id", "major_id", "class_id", "enrollment_date",
                         "political_status", "address", "guardian_name", "guardian_phone",
                         "dormitory", "photo", "status"], stu_rows)

# 回填班级人数
b.w()
for cid, ids in class_students.items():
    b.w("UPDATE `base_class` SET `student_count` = %d WHERE `id` = %d;" % (len(ids), cid))

# ============================================================================
# 7. 用户 / 用户角色
# ============================================================================
b.section("7. 系统用户与用户角色")
b.w("DELETE FROM `sys_user_role`;")
b.w("DELETE FROM `sys_user`;")
b.insert("sys_user", ["id", "username", "password", "real_name", "user_type", "gender",
                      "phone", "email", "status"], user_rows)
b.insert("sys_user_role", ["user_id", "role_id"], user_role_rows)

# ============================================================================
# 8. 课程
# ============================================================================
b.section("8. 课程数据")
COURSES = [
    ("C0001", "高等数学(上)", 1, 5.0, 80, "必修", "考试"),
    ("C0002", "高等数学(下)", 1, 5.0, 80, "必修", "考试"),
    ("C0003", "线性代数", 1, 3.0, 48, "必修", "考试"),
    ("C0004", "概率论与数理统计", 1, 3.0, 48, "必修", "考试"),
    ("C0005", "大学英语(一)", 4, 4.0, 64, "公共", "考试"),
    ("C0006", "大学英语(二)", 4, 4.0, 64, "公共", "考试"),
    ("C0007", "程序设计基础(C语言)", 1, 4.0, 64, "必修", "考试"),
    ("C0008", "面向对象程序设计(Java)", 1, 4.0, 64, "必修", "考试"),
    ("C0009", "数据结构与算法", 1, 4.0, 72, "必修", "考试"),
    ("C0010", "计算机组成原理", 1, 3.5, 56, "必修", "考试"),
    ("C0011", "操作系统", 1, 4.0, 64, "必修", "考试"),
    ("C0012", "计算机网络", 1, 3.5, 56, "必修", "考试"),
    ("C0013", "数据库系统原理", 1, 4.0, 64, "必修", "考试"),
    ("C0014", "软件工程导论", 2, 3.0, 48, "必修", "考查"),
    ("C0015", "Web前端开发技术", 2, 3.0, 48, "选修", "考查"),
    ("C0016", "Spring Boot 企业级开发", 2, 3.0, 48, "选修", "考查"),
    ("C0017", "网络安全技术", 3, 3.0, 48, "选修", "考查"),
    ("C0018", "数字电路与逻辑设计", 2, 3.5, 56, "必修", "考试"),
    ("C0019", "信号与系统", 2, 3.5, 56, "必修", "考试"),
    ("C0020", "通信原理", 2, 3.5, 56, "必修", "考试"),
    ("C0021", "管理学原理", 3, 3.0, 48, "必修", "考试"),
    ("C0022", "基础会计学", 3, 3.0, 48, "必修", "考试"),
    ("C0023", "大学体育(一)", 1, 1.0, 32, "公共", "考查"),
    ("C0024", "思想道德与法治", 1, 3.0, 48, "公共", "考查"),
    ("C0025", "Python数据分析", 1, 3.0, 48, "选修", "考查"),
    ("C0026", "人工智能导论", 1, 3.0, 48, "选修", "考查"),
]
b.w("DELETE FROM `course_course`;")
rows = [(i + 1, c[0], c[1], c[2], c[3], c[4], c[5], c[6], "%s课程，培养学生在该领域的理论素养与实践能力。" % c[1], 1)
        for i, c in enumerate(COURSES)]
b.insert("course_course", ["id", "course_code", "course_name", "dept_id", "credit", "hours",
                           "course_type", "exam_type", "description", "status"], rows)

# ============================================================================
# 9. 开课安排 + 排课 (当前学期)
# ============================================================================
b.section("9. 开课安排与排课数据")
CUR_SEM = 4
MAJOR_COURSES = {
    1: [9, 11, 12, 13, 6, 23],
    2: [8, 9, 13, 14, 16, 23],
    3: [9, 12, 17, 13, 6, 23],
    4: [18, 19, 10, 6, 23, 24],
    5: [19, 20, 10, 6, 23, 24],
    6: [21, 22, 6, 23, 24, 4],
    7: [22, 21, 6, 23, 24, 4],
    8: [5, 6, 23, 24, 21, 4],
}
PUBLIC_ELECTIVES = [15, 16, 25, 26]

# 教师授课偏好: 课程 -> 教师池
TEACHER_POOL = {
    1: [13, 14], 2: [14, 15], 3: [13, 16], 4: [16, 20], 5: [20], 6: [20],
    7: [13, 15], 8: [14, 15, 16], 9: [1, 3, 13], 10: [17, 18], 11: [2, 17],
    12: [3, 12], 13: [4, 18], 14: [5, 19], 15: [6], 16: [7], 17: [8],
    18: [9, 19], 19: [10], 20: [11, 19], 21: [12], 22: [12], 23: [11, 20],
    24: [13, 14], 25: [1, 2], 26: [3, 4],
}
# 班主任本身也授课
for c in list(TEACHER_POOL.keys()):
    if c in (9, 11, 13, 21, 22):
        TEACHER_POOL[c] = TEACHER_POOL[c] + [c % 12 + 1]
TEACHER_POOL[9] = [1, 3, 13]
TEACHER_POOL[11] = [2, 4, 17]
TEACHER_POOL[13] = [4, 5, 18]
TEACHER_POOL[21] = [12, 11]
TEACHER_POOL[22] = [12, 11]

online_slots = [(d, s, s + 1) for d in range(1, 6) for s in (1, 3, 5)]
evening_slots = [(d, 7, 8) for d in range(1, 6)]

teacher_busy = set()
room_busy = set()
class_busy = set()

offerings = []
schedules = []
off_id = 1
sch_id = 1

# 班级开课
for cid, code, cname, major, grade, head in class_defs:
    home_room = (cid % 12) + 1
    for k, course_id in enumerate(MAJOR_COURSES[major]):
        teacher_candidates = TEACHER_POOL.get(course_id, [1])
        slot = None
        assigned_teacher = None
        for t in teacher_candidates:
            for (d, s, e) in sorted(online_slots, key=lambda x: (x[0] * 7 + x[1] + cid) % 37):
                if (cid, d, s) in class_busy:
                    continue
                if (t, d, s) in teacher_busy:
                    continue
                if (home_room, d, s) in room_busy:
                    continue
                slot = (d, s, e)
                assigned_teacher = t
                break
            if slot:
                break
        if not slot:
            slot = (1, 1, 2)
            assigned_teacher = teacher_candidates[0]
        d, s, e = slot
        class_busy.add((cid, d, s))
        teacher_busy.add((assigned_teacher, d, s))
        room_busy.add((home_room, d, s))
        offerings.append((off_id, "OF%05d" % off_id, course_id, CUR_SEM, assigned_teacher, cid,
                          60, 0, 0, 1))
        schedules.append((sch_id, off_id, d, s, e, 1, 16, home_room))
        sch_id += 1
        off_id += 1

# 公选课
elective_room = 12
for k, course_id in enumerate(PUBLIC_ELECTIVES):
    t = TEACHER_POOL[course_id][0]
    d, s, e = evening_slots[k % len(evening_slots)]
    while (t, d, s) in teacher_busy:
        d = d % 5 + 1
    teacher_busy.add((t, d, s))
    offerings.append((off_id, "OF%05d" % off_id, course_id, CUR_SEM, t, None, 120, 0, 1, 1))
    schedules.append((sch_id, off_id, d, s, e, 1, 16, elective_room))
    sch_id += 1
    off_id += 1

b.w("DELETE FROM `course_schedule`;")
b.w("DELETE FROM `course_offering`;")
b.insert("course_offering", ["id", "offering_code", "course_id", "semester_id", "teacher_id",
                             "class_id", "capacity", "selected_count", "is_public", "status"], offerings)
b.insert("course_schedule", ["id", "offering_id", "week_day", "start_section", "end_section",
                             "start_week", "end_week", "classroom_id"], schedules)

# ============================================================================
# 10. 选课
# ============================================================================
b.section("10. 选课数据")
selections = []
sel_id = 1
cur_time = datetime.datetime(2026, 9, 1, 9, 30)
class_openings = {}
public_openings = []
for o in offerings:
    if o[8] == 1:
        public_openings.append(o[0])
    else:
        class_openings.setdefault(o[5], []).append(o[0])

for cid, ids in class_students.items():
    for oid in class_openings.get(cid, []):
        for sid in ids:
            selections.append((sel_id, sid, oid, 2, cur_time + datetime.timedelta(minutes=random.randint(0, 600)), 1))
            sel_id += 1

for cid, ids in class_students.items():
    for sid in ids:
        if random.random() < 0.55:
            picked = random.sample(public_openings, random.randint(1, 2))
            for oid in picked:
                selections.append((sel_id, sid, oid, 1, cur_time + datetime.timedelta(days=random.randint(0, 3),
                                                                                     minutes=random.randint(0, 900)), 1))
                sel_id += 1
b.w("DELETE FROM `course_selection`;")
b.insert("course_selection", ["id", "student_id", "offering_id", "select_type", "select_time", "status"], selections)

# 更新已选人数
b.w()
for oid in class_openings:
    pass
b.w("UPDATE `course_offering` o SET o.`selected_count` = (SELECT COUNT(*) FROM `course_selection` s WHERE s.`offering_id` = o.`id` AND s.`status` = 1);")

# ============================================================================
# 11. 成绩
# ============================================================================
b.section("11. 成绩数据")
# 开课 -> 课程学分
off_course = {o[0]: o[2] for o in offerings}
credit_of = {i + 1: COURSES[i][3] for i in range(len(COURSES))}


def grade_point(total):
    if total is None:
        return None
    if total >= 90:
        return 4.0
    if total >= 85:
        return 3.7
    if total >= 82:
        return 3.3
    if total >= 78:
        return 3.0
    if total >= 75:
        return 2.7
    if total >= 72:
        return 2.3
    if total >= 68:
        return 2.0
    if total >= 64:
        return 1.5
    if total >= 60:
        return 1.0
    return 0.0


scores = []
sc_id = 1
stu_ability = {}
for cid, ids in class_students.items():
    for sid in ids:
        stu_ability[sid] = random.gauss(0, 1)

for (sel_id_x, sid, oid, stype, stime, st) in selections:
    if st != 1:
        continue
    base = 78 + stu_ability.get(sid, 0) * 6
    usual = max(50, min(100, round(random.gauss(base + 4, 4), 1)))
    exam = max(35, min(100, round(random.gauss(base, 9), 1)))
    total = round(usual * 0.3 + exam * 0.7, 1)
    gp = grade_point(total)
    r = random.random()
    if r < 0.85:
        status = 2
    elif r < 0.95:
        status = 1
    else:
        status = 0
    pub = datetime.datetime(2026, 9, 20, 10, 0) + datetime.timedelta(days=random.randint(0, 10)) if status == 2 else None
    scores.append((sc_id, sid, oid, usual, exam, total, gp, status, 0,
                   (2 if stype == 2 else None), datetime.datetime(2026, 9, 15, 14, 0) + datetime.timedelta(minutes=random.randint(0, 3000)),
                   pub))
    sc_id += 1
b.w("DELETE FROM `score_record`;")
b.insert("score_record", ["id", "student_id", "offering_id", "usual_score", "exam_score",
                          "total_score", "grade_point", "status", "is_retake", "input_by",
                          "input_time", "publish_time"], scores)

# ============================================================================
# 12. 考勤
# ============================================================================
b.section("12. 考勤数据")
att = []
aid = 1
weekdays = []
d0 = TODAY - datetime.timedelta(days=TODAY.weekday())
for w in range(4):
    for dd in range(5):
        weekdays.append(d0 - datetime.timedelta(days=7 * w) + datetime.timedelta(days=dd))
for cid, ids in class_students.items():
    for oid in class_openings.get(cid, []):
        for date in weekdays:
            for sid in ids:
                if random.random() < 0.10:
                    t = random.choices([1, 2, 3, 4, 5], weights=[55, 15, 8, 8, 14])[0]
                    att.append((aid, sid, oid, date, t, {1: "正常", 2: "上课迟到10分钟", 3: "提前离开", 4: "无故缺勤", 5: "已请假"}[t], None))
                    aid += 1
b.w("DELETE FROM `att_attendance`;")
b.insert("att_attendance", ["id", "student_id", "offering_id", "attend_date", "attend_type", "remark", "recorder_id"], att)

# ============================================================================
# 13. 请假
# ============================================================================
b.section("13. 请假数据")
leaves = []
lid = 1
reasons = ["感冒发烧，需要输液治疗", "家中有事需回家处理", "参加校级学科竞赛集训",
           "牙齿治疗需复诊", "陪同家人就医", "参加社会实践活动", "身体不适需要休息",
           "参加普通话等级考试", "家中有长辈生病需照顾", "参加计算机等级考试"]
ltypes = ["病假", "事假", "事假", "病假", "事假", "公假", "病假", "事假", "事假", "公假"]
all_ids = [s for ids in class_students.values() for s in ids]
for i in range(60):
    sid = random.choice(all_ids)
    st = random.choices([0, 1, 2, 3], weights=[20, 55, 15, 10])[0]
    start = datetime.datetime(2026, 9, random.randint(1, 28), random.choice([8, 9, 13, 14]), 0)
    days = random.choice([0.5, 1.0, 1.0, 2.0, 3.0])
    end = start + datetime.timedelta(days=days)
    j = i % len(reasons)
    appr = None
    an = None
    at = None
    ar = None
    if st in (1, 2):
        appr = random.choice([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12])
        an = teachers[appr - 1]["name"]
        at = start - datetime.timedelta(hours=random.randint(2, 20))
        ar = "同意，注意安全，按时返校。" if st == 1 else "请假事由不充分，不予批准。"
    leaves.append((lid, sid, ltypes[j], start, end, days, reasons[j], st, appr, an, at, ar,
                   start - datetime.timedelta(days=1)))
    lid += 1
b.w("DELETE FROM `att_leave`;")
b.insert("att_leave", ["id", "student_id", "leave_type", "start_date", "end_date", "days",
                       "reason", "status", "approver_id", "approver_name", "approve_time",
                       "approve_remark", "create_time"], leaves)

# ============================================================================
# 14. 公告
# ============================================================================
b.section("14. 通知公告数据")
notices = [
    ("关于开展2026-2027学年第一学期期中教学检查的通知",
     "<p>各学院、各班级：</p><p>为加强教学过程管理，切实提高教学质量，教务处决定于第9-10周开展期中教学检查。请各班主任组织本班学生认真配合，如实填写教学评价表。</p><p>检查内容包括：教学进度执行情况、课堂教学秩序、作业布置与批改情况、实验实践教学开展情况。</p>",
     "教务", "ALL", None, None, 1),
    ("关于2026-2027学年第一学期期末考试安排的通知",
     "<p>各位同学：</p><p>本学期期末考试将于第17-18周进行，具体考试时间及地点将于第15周公布。请同学们合理安排复习时间，诚信应考。</p><p>考试违纪将按《学生手册》相关规定严肃处理。</p>",
     "教务", "ALL", None, None, 1),
    ("关于评选2025-2026学年优秀学生的通知",
     "<p>各位同学：</p><p>本年度优秀学生评选工作现已启动，评选条件：学年平均绩点3.0以上，无违纪记录，综合素质测评成绩排名专业前30%。</p><p>请符合条件的同学于10月20日前向班主任提交申请材料。</p>",
     "通知", "ALL", None, None, 1),
    ("2026届毕业生就业指导讲座通知",
     "<p>为帮助毕业班同学了解就业形势、提升求职技能，学院特邀请知名企业HR开展就业指导讲座。</p><p>时间：10月15日 14:00<br/>地点：第一教学楼报告厅<br/>主讲人：某科技公司人力资源总监</p>",
     "通知", "ALL", None, None, 1),
    ("关于调整作息时间的通知",
     "<p>各位同学：</p><p>自10月8日起执行冬季作息时间，上午第一节课上课时间调整为8:00，下午第一节课调整为14:00，晚自习19:00-21:00。</p>",
     "通知", "ALL", None, None, 1),
    ("计算机学院23级学风建设主题班会安排",
     "<p>本班定于10月10日（周五）下午15:00在R001教室召开学风建设主题班会，请全体同学准时参加，不得无故缺席。</p>",
     "通知", "CLASS", 1, None, 1),
    ("关于软件工程2301班课程实践项目分组通知",
     "<p>本学期《Spring Boot 企业级开发》课程实践项目要求4人一组，请各位于本周五前完成分组并将名单报给班长。</p>",
     "通知", "CLASS", 4, None, 1),
    ("关于国家奖学金评审结果的公示",
     "<p>经学生申请、学院初评、学校评审委员会审定，现将2025-2026学年国家奖学金拟推荐名单予以公示，公示期5个工作日。</p>",
     "公告", "ALL", None, None, 1),
    ("图书馆国庆节后开放时间调整公告",
     "<p>国庆节后图书馆恢复正常开放时间：周一至周五 8:00-22:00，周六周日 9:00-21:00。</p>",
     "公告", "ALL", None, None, 1),
    ("关于加强学生宿舍安全管理的通知",
     "<p>近期将开展学生宿舍安全专项检查，重点检查违规电器使用、私拉电线等情况，请同学们自觉配合。</p>",
     "通知", "ALL", None, None, 1),
]
rows = []
for i, (title, content, ntype, scope, class_id, trole, status) in enumerate(notices):
    if scope == "CLASS":
        pub_id = 4  # 某班主任
        pub_name = teachers[0]["name"]
        pub_role = "班主任"
    else:
        pub_id = 2 if i % 2 == 0 else 3
        pub_name = "张国华" if pub_id == 2 else "李慧敏"
        pub_role = "教务处"
    rows.append((i + 1, title, content, ntype, scope, class_id, trole, pub_id, pub_name, pub_role,
                 status, datetime.datetime(2026, 9, 25, 9, 0) + datetime.timedelta(days=i)))
b.w("DELETE FROM `sys_notice`;")
b.insert("sys_notice", ["id", "title", "content", "notice_type", "scope", "class_id", "target_role",
                        "publisher_id", "publisher_name", "publisher_role", "status", "publish_time"], rows)

# ============================================================================
content = b.text()
with open(OUT, "w", encoding="utf-8") as f:
    f.write("-- ============================================================================\n")
    f.write("--  教务管理系统 - 演示数据脚本 (由 generate_demo_data.py 生成)\n")
    f.write("--  所有账号默认密码: 123456\n")
    f.write("--    admin   / 123456  系统管理员\n")
    f.write("--    jwc001  / 123456  教务处\n")
    f.write("--    jwc002  / 123456  教务处\n")
    f.write("--    T1001~T1012 / 123456  班主任\n")
    f.write("--    2023xxx ~ 2024xxx / 123456  学生\n")
    f.write("-- ============================================================================\n\n")
    f.write("USE `academic_affairs`;\nSET NAMES utf8mb4;\n")
    f.write(content)
print("生成完成:", OUT)
print("行数:", content.count("\n"))
