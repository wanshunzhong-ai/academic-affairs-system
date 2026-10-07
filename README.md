# 教务管理系统（Academic Affairs System）

基于 **Spring Boot 3 + Vue 3** 的前后端分离教务管理系统，覆盖 **学生 / 班主任 / 教务处 / 系统管理员** 四个身份，
实现了学籍、教学、成绩、考勤、公告与系统管理共 6 大业务域，并内置三级数据权限隔离。

---

## 一、技术栈

| 层次 | 技术选型 |
| --- | --- |
| 后端框架 | Spring Boot 3.3.5（JDK 21） |
| 持久层 | MyBatis-Plus 3.5.9 + MySQL 8.0/9.x |
| 安全认证 | Spring Security + JWT（jjwt 0.12.6，HS256） |
| 接口文档 | Knife4j 4.5.0（OpenAPI 3） |
| 报表导出 | EasyExcel 4.0.3 |
| 工具库 | Hutool 5.8.32、Lombok 1.18.48（已从 Spring Boot 管理的 1.18.34 抬高，兼容 JDK 21~27） |
| 前端框架 | Vue 3.5（Composition API + `<script setup>`） |
| UI 组件 | Element Plus 2.9 |
| 状态 / 路由 | Pinia 2.3、Vue Router 4.5（后端菜单驱动动态路由） |
| 图表 / 构建 | ECharts 5.5、Vite 5.4、Axios 1.7 |

---

## 二、项目结构

```
AcademicAffairsSystem/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/aas/
│       │   ├── common/               # 统一返回、异常、操作日志注解与切面
│       │   ├── config/               # Security / MyBatis-Plus / Jackson / Knife4j
│       │   ├── controller/           # 20 个 REST 控制器
│       │   ├── dto/                  # 分页、查询条件、数据权限 DataScope
│       │   ├── entity/               # 22 个实体
│       │   ├── mapper/               # Mapper 接口
│       │   ├── security/             # JWT 过滤器、登录用户、权限表达式
│       │   ├── service/              # 业务服务（15+）
│       │   ├── util/                 # Excel 导出、菜单树、IP 工具
│       │   └── vo/                   # 登录 / 用户信息 / 开课表单
│       └── resources/
│           ├── application.yml
│           └── mapper/*.xml          # 11 个 MyBatis XML（复杂查询 + 数据权限）
├── frontend/                         # Vue 3 前端
│   ├── vite.config.js                # /api 代理到 localhost:8080
│   └── src/
│       ├── api/index.js              # 全部接口封装
│       ├── components/               # StatCard / ChartBox / TimetableGrid
│       ├── composables/useTable.js   # 列表页通用逻辑
│       ├── layout/                   # 侧边栏 + 顶栏 + 修改密码
│       ├── router/index.js           # 动态路由 + 登录守卫（单一入口 /login）
│       ├── store/                    # user / app
│       ├── utils/                    # request / format / dict / auth
│       │   └── loginRoles.js         # 身份配置（唯一入口 + 四身份元数据，单一来源）
│       └── views/                    # 30 个页面
├── sql/
│   ├── 01_schema.sql                 # 22 张表 DDL
│   ├── 02_data.sql                   # 演示数据（约 1.3 万行）
│   ├── 03_add_teacher_menu.sql       # 增量：补充教师管理菜单
│   └── generate_demo_data.py         # 演示数据生成脚本（可重复生成）
├── docs/
│   ├── api_test.py                   # 端到端接口验证（真实调接口）
│   ├── api_test_result.txt           # 端到端验证结果快照
│   ├── api_path_crosscheck.py        # 前后端接口路径交叉比对（静态）
│   ├── api_path_crosscheck_result.txt
│   ├── menu_view_crosscheck.py       # 菜单 component ↔ 前端视图校验（静态）
│   ├── menu_view_crosscheck_result.txt
│   ├── login_entry_crosscheck.py     # 登录入口收口 + 身份码 ↔ 数据库角色一致性校验
│   └── login_entry_crosscheck_result.txt
├── .run/                             # IDEA 共享运行配置（导入 Maven 后即可直接选用）
│   ├── 一键启动-前后端.run.xml        #   Compound：一次拉起后端 + 前端
│   ├── 后端-启动服务.run.xml          #   启动后端主类（Application 类型，8080）
│   ├── 后端-打包jar.run.xml           #   clean package → target/*.jar
│   ├── 前端-启动开发服务(兼容).run.xml #   Shell Script：frontend 里跑 npm run dev（免插件）
│   └── 前端-启动开发服务.run.xml      #   npm 类型（需启用 JetBrains Ultimate 插件）
├── start-backend.bat                 # 一键启动后端（自动探测 JDK 21 与 Maven）
├── start-frontend.bat                # 一键启动前端（自动探测 Node.js）
├── 运行指南.md                       # 本地运行详细手册（IDEA 全流程 + 部署 + 排错表）
└── README.md
```

---

## 三、四身份与权限模型

系统采用 **RBAC + 数据范围（DataScope）** 双层控制：

| 角色 | 标识 | 数据范围 | 核心职责 |
| --- | --- | --- | --- |
| 学生 | `STUDENT` | `SELF` 仅本人 | 查看个人学籍/课表/成绩、在线选课、提交请假 |
| 班主任 | `HEAD_TEACHER` | `CLASS` 本班 | 管理本班学生、审批本班请假、录入本班成绩与考勤 |
| 教务处 | `ACADEMIC` | `ALL` 全校 | 全校教务：学籍/排课/选课/成绩审核发布/统计报表 |
| 系统管理员 | `ADMIN` | `ALL` 全校 | 用户、角色、菜单权限、操作日志等系统管理 |

数据范围在 **SQL 层** 落地：`DataScope.of(loginUser)` 生成过滤条件，
在 Mapper XML 中用 `<choose>` 分支拼装 —— 学生按 `student_id` 过滤、班主任按 `manageClassIds` 过滤、
教务处/管理员不加条件。因此**即使前端被绕过，后端也不会泄露越权数据**。

权限点遵循 `模块:操作` 约定（如 `student:add`、`score:audit`），后端用
`@PreAuthorize("@ss.hasPerm('xxx')")` 校验，前端用 `userStore.hasPerm()` 控制按钮显隐。

---

## 四、功能模块

### 1. 工作台（按角色分流）
四种角色展示不同内容：管理员看全局规模与院系分布；教务处看成绩等级分布、课程排名与待办；
班主任看本班学生、各科平均分、今日考勤与快捷入口；学生看学分进度、成绩/考勤图表与个人课表。

### 2. 学籍管理
- **学生管理**：多条件查询、CRUD、批量删除、Excel 导入 / 导出 / 模板下载、学籍状态变更、详情抽屉
- **教师管理**：CRUD、带班班级分配、班主任账号自动开通
- **班级管理**：CRUD、专业联动、班主任指派、学生数维护
- **院系 / 专业 / 学期 / 教室管理**：基础数据维护，学期支持「设为当前学期」与选课时间窗

### 3. 教学管理
- **课程管理**：课程编码、学分、学时、课程性质（必修/选修/公共）、考核方式
- **开课安排**：开课（含班级/教师/容量/公共课标记）+ **排课冲突自动校验**
- **课表查询**：按班级 / 教师 / 学生三种视角查看周课表（自动合并节次、跨周次显示）
- **在线选课**（学生）：可选课程分页、学分上限（30）校验、时间冲突校验、退选
- **选课管理**（教务）：选课记录查询、批量分配选课、名单导出

> **排课冲突检测**：针对同一开课，比较星期、节次区间、周次区间是否重叠，
> 分别校验 **教师冲突 / 班级冲突 / 教室冲突** 三类，命中即拒绝保存并返回冲突详情。

### 4. 成绩管理
- **成绩录入**：选择开课 → 生成成绩单 → 网格化录入平时/期末分 → 自动计算总评与绩点 → 保存 / 发布 / 撤回
- **我的成绩**（学生）：成绩单、绩点、等级、各学期趋势折线、等级分布饼图
- **成绩审核**（教务）：按时段/班级/开课筛选、批量发布与撤回、成绩概览与分布
- **成绩统计**：课程平均分对比、全校等级分布、班级学生排名、及格率/优秀率

> **总评成绩** = 平时 × 30% + 期末 × 70%（`Math.round` 保留 1 位小数）
> **绩点换算**：90+→4.0、85+→3.7、82+→3.3、78+→3.0、75+→2.7、72+→2.3、68+→2.0、64+→1.5、60+→1.0、<60→0

### 5. 考勤管理
- **考勤记录**：条件查询、**课堂点名**（选开课 → 载入选课名单 → 逐人设置出勤状态 → 一键保存）、补录、批量删除、班级考勤统计
- **请假审批**（班主任/教务）：待审批筛选、单条通过/驳回（驳回需填理由）、批量审批、详情抽屉
- **我的请假**（学生）：提交申请（自动计算天数，按 0.5 天取整）、撤销、审批结果查看

### 6. 通知公告
- **公告发布**：按范围发布（全校 / 指定班级 / 指定角色）、上下架、预览、富文本内容
- **公告列表**：所有身份的可见公告（列表 / 卡片两种视图）、已读标记、未读角标、一键已读

### 7. 系统管理（仅管理员）
- **用户管理**：CRUD、角色分配、密码重置、启用停用、批量删除
- **角色管理**：CRUD、数据范围设置、**菜单权限树勾选分配**、内置角色保护
- **菜单管理**：树形菜单 CRUD、图标选择、路由/组件/权限标识配置
- **操作日志**：AOP 自动记录（模块、操作、方法、URI、参数、IP、耗时、异常），支持检索、删除、清理历史

### 8. 个人中心
当前登录用户的账号信息、教师/学生档案、学业概况、权限清单、修改密码。

---

## 五、快速启动

> 📖 **首次在本机运行，推荐直接看 [`运行指南.md`](运行指南.md)** ——
> 那份文档按「装软件 → 建库 → 改密码 → 启后端（含 IDE 详细步骤）→ 启前端 → 排错」逐步展开，
> 并附常见报错对照表。本章只给最快的路径。

### 环境要求

| 组件 | 版本 | 本机路径（示例） |
| --- | --- | --- |
| JDK | 21 | `D:\SoftWare\JetBrains\PyCharm 2025.2\jbr` |
| Maven | 3.9+ | `C:\Users\15726\.workbuddy\tools\apache-maven-3.9.9` |
| MySQL | 8.0+ / 9.x | `D:\SoftWare\MySQL\MySQL Server 9.0\bin\mysql.exe` |
| Node.js | 18+ | `C:\Users\15726\.workbuddy\binaries\node\versions\22.22.2-6` |

### 步骤 1：初始化数据库

```bash
# 建库建表
mysql -uroot -p < sql/01_schema.sql
# 导入演示数据（约 1.3 万行）
mysql -uroot -p --default-character-set=utf8mb4 < sql/02_data.sql
# 补充教师管理菜单（增量脚本，幂等）
mysql -uroot -p < sql/03_add_teacher_menu.sql
```

或重新生成演示数据（随机但可复现，`random.seed` 固定）：

```bash
python sql/generate_demo_data.py     # 输出 sql/02_data.sql
```

### 步骤 2：启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/academic-affairs-system.jar --server.port=8080
```

或直接双击 `start-backend.bat`：

```bat
start-backend.bat                  直接启动（缺 jar 时自动构建）
start-backend.bat 8081             换端口启动
start-backend.bat rebuild 8081     重新构建 + 换端口
```

脚本会自动探测 JDK 21（`PATH` → `JAVA_HOME` → JetBrains 自带的 JBR → 常见 JDK 目录）
与 Maven（`PATH` → 独立安装目录 → IDEA 内置），**换电脑无需修改脚本**。

- 服务地址：http://localhost:8080
- 接口文档：http://localhost:8080/doc.html
- 健康检查：http://localhost:8080/api/auth/ping

> **用 IDEA 跑请见 [`运行指南.md`](运行指南.md) 第四章「方式 A」** —— IntelliJ IDEA 全流程
> （导入 Maven → 指定 JDK 21 → 开启 Lombok 注解处理 → 一键运行/调试 → 打包发布）。
> 其中 **A0** 专门讲「已经把项目根目录导入 IDEA、但 Java 文件全红」的补救办法，
> **A3** 讲本机 SDK 被设成 `openjdk-27` 时的处理办法。
>
> ⚠️ **关于 198 条「找不到符号」**：这是本机最容易踩的坑，成因有两个且都会命中 ——
> ① **JDK 23 起 javac 不再自动扫描 classpath 上的注解处理器**；
> ② **Lombok 1.18.34 的最高支持版本是 JDK 23**，本机 SDK 是 `openjdk-27`，Lombok 会直接崩。
> **这两条项目已在 `backend/pom.xml` 里修好**：抬高 `<lombok.version>1.18.48</lombok.version>`
> 并给 `maven-compiler-plugin` 显式声明 `annotationProcessorPaths`（配 `<proc>full</proc>`）。
> 实测 JDK 21 与 JDK 27 下均 `BUILD SUCCESS`，**打包出的 jar 在两种 JDK 下也都启动正常**
> （健康检查 + 学生登录均通过，日志无 ERROR），所以**换不换 JDK 都能跑**。
> **改了 pom 后记得点 Maven 面板的刷新图标重载工程。**
>
> 项目根目录 `.run/` 下已有**五条**预置运行配置，导入 Maven 后可在右上角下拉框直接选用，
> 其中 `一键启动-前后端` 能一次把前后端都拉起来。
>
> ⚠️ **本机 IDEA 禁用了 `JetBrains Ultimate` 插件模块**，Spring / Node.js 等插件不加载，
> 所以 `后端-启动服务` 改用核心 `Application` 类型、前端请用 `前端-启动开发服务(兼容)`
> （`Shell Script` 类型）—— 这两条**不依赖任何 Ultimate 功能**。详见 `运行指南.md` 的 **A0.1**。

### 步骤 3：启动前端

**在 IDEA 里**：右上角运行下拉框选 `一键启动-前后端`（推荐，前后端一起起），
或单独选 `前端-启动开发服务(兼容)`（免插件、免 Node 解释器），点绿色三角即可。

命令行方式：

```bash
cd frontend
npm install   # 仅新机器需要；本机 node_modules 已装好，可跳过
npm run dev
```

或直接双击 `start-frontend.bat`。访问 http://localhost:5173

> **端口锁定 5173**（`vite.config.js` 设 `strictPort: true`，与后端启动横幅打印的入口地址一致）。
> 若 5173 被占用，`npm run dev` 会先自动打印**占用者 PID 与清理命令**
> （`frontend/scripts/check-port.mjs`，由 `predev` 钩子触发），不会只抛一句英文报错。
> 处理办法见 [`运行指南.md`](运行指南.md) 7.1。

### 登录入口（只有一个）

系统**只有一个登录入口**：<http://localhost:5173/login>

登录时不需要选身份 —— 后端按账号返回该身份应有的菜单、按钮权限与数据范围，
前端据此装载动态路由，所以四个身份进去后是**四个不同的后台**：

| 身份 | 账号形式 | 可见页面数 | 落地页 |
| --- | --- | --- | --- |
| 学生 | 学号 | 7 | `/dashboard` |
| 班主任 | 教师工号 | 10 | `/dashboard` |
| 教务处 | 教务处账号 | 20 | `/dashboard` |
| 管理员 | 管理员账号 | 27 | `/dashboard` |

> 旧版遗留的 `/login/student`、`/login/headteacher`、`/login/academic`、`/login/admin`
> 会自动重定向回 `/login`，老书签不会失效。
> 登录态失效、退出登录、修改密码之后也都统一回 `/login`。

启动时会打印入口地址（`com.aas.config.StartupBanner`）：
内嵌前端时打印后端地址（`http://localhost:8080/login`），
前后端分开跑时打印前端开发服务器地址（`http://localhost:5173/login`）；
也可以用 `aas.entry-url` 直接写死。

### 演示账号

| 身份 | 账号 | 密码 |
| --- | --- | --- |
| 系统管理员 | `admin` | `123456` |
| 教务处 | `jwc001`、`jwc002` | `123456` |
| 班主任 | `T1001` ~ `T1012` | `123456` |
| 学生 | `2023001` 起（学号即账号） | `123456` |

> 登录页**默认不显示**演示账号（`aas.demo-accounts: false`）。改成 `true` 重启后，
> 登录页会出现四个身份的一键填充按钮（`GET /api/auth/login-config` 返回该开关）。
> 身份配置集中在 `frontend/src/utils/loginRoles.js`，新增身份只需追加一项（详见第八章）。

---

## 六、演示数据规模

| 数据 | 数量 |
| --- | --- |
| 用户 / 学生 / 教师 | 213 / 198 / 20（含 12 名班主任） |
| 院系 / 专业 / 班级 / 教室 | 4 / 8 / 12 / 12 |
| 课程 / 开课 / 排课 | 26 / 76 / 76 |
| 选课 / 成绩 | 1355 / 1355 |
| 考勤 / 请假 / 公告 | 2388 / 60 / 10 |
| 合计 | 约 1.3 万行 |

---

## 七、接口验证

### 7.1 端到端接口验证

`docs/api_test.py` 是一个零依赖的端到端验证脚本，覆盖 4 角色登录、各模块列表与统计接口、
权限拒绝断言，以及两条完整业务流（选课→退课、请假→审批）。

```bash
# 确保后端已在 8080 端口运行
python docs/api_test.py
```

最近一次运行结果：**通过 53 项 / 失败 0 项**（完整输出见 `docs/api_test_result.txt`）。

覆盖清单：

| 角色 | 用例数 | 说明 |
| --- | --- | --- |
| 管理员 admin | 19 | 工作台、用户/角色/菜单、院系/专业/班级/教师/学生/课程/开课/选课/成绩/考勤/请假、日志、院系统计 |
| 教务处 jwc001 | 6 | 工作台、学生管理、课程平均分、选课名单导出（xlsx 二进制流）、越权拒绝 |
| 班主任 T1001 | 7 | 工作台、我的班级、本班学生（数据权限收敛到 1 个班）、本班课表、请假审批、本班成绩、越权拒绝 |
| 学生 2023001 | 13 | 学籍、课表、选课、学分、成绩、成绩趋势、考勤、请假、公告、可选课程、工作台、2 项越权拒绝 |
| 业务流 | 6 | 选课→退课；提交请假→班主任可见→审批通过→状态回读为「已通过」 |

### 7.2 前后端接口路径交叉比对

构建工具只能保证前端「编译通过」，无法发现**前端调用了一个后端并不存在的路径**。
`docs/api_path_crosscheck.py` 解析后端全部 20 个 Controller 的映射注解
（含 `@PostMapping` 这类无参裸写法），与前端 `src/api/index.js` 中声明的请求逐一比对。

```bash
python docs/api_path_crosscheck.py
```

最近一次结果：

| 指标 | 数值 |
| --- | --- |
| 后端 Controller 映射总数 | 170 |
| 前端 API 声明总数（含 `download()` 导出） | 170 |
| 匹配成功 | 170 |
| 未匹配 | 0 |
| 后端已实现但前端未使用 | 0 |

### 7.3 菜单 component 与前端视图校验

动态路由依赖「菜单表 `component` 字符串 → `import.meta.glob('../views/**/*.vue')`」的运行时解析。
`docs/menu_view_crosscheck.py` 登录管理员拉取真实菜单树，逐一确认每个 C 类菜单的
`component` 都能落到真实存在的 `.vue` 文件（构建期无法发现此类问题）。

```bash
python docs/menu_view_crosscheck.py
```

最近一次结果：**27 个菜单 component 全部命中**；前端 30 个 `.vue` 中未被菜单引用的是
`login/index.vue`、`error/403.vue`、`error/404.vue`（属预期）。

### 7.4 前端构建

```bash
cd frontend && npm run build
```

最近一次结果：`✓ built in 14.90s`，2245 个模块，30 个视图各自产出独立异步 chunk，
`element` / `echarts` / `vue` 拆为 vendor chunk。

### 7.5 登录入口一致性校验

登录入口收口之后，配置写错仍然是**静默的**：`roleCode` 与数据库对不上会让身份识别失效，
演示账号写错会让文档里给出的账号根本登不进去。`docs/login_entry_crosscheck.py` 把两端钉死 ——
校验「前端确实只注册了一个登录入口、旧地址确实做了重定向、登录态失效确实回 `/login`」，
以及 `loginRoles.js` 的 `roleCode` 与数据库 `sys_role.role_code` 一致、演示账号存在且角色匹配。

```bash
python docs/login_entry_crosscheck.py
```

最近一次结果：**通过 23 项 / 失败 0 项**（四个 roleCode 与 `sys_role` 完全一致，
四个演示账号角色均匹配，入口收口相关 11 项检查全过）。

---

## 八、关键设计说明

**统一响应结构**：所有接口返回 `{ code, message, data, timestamp }`，
前端响应拦截器统一解包，`code === 200` 视为成功，`401 / 1005` 触发重新登录。

**JWT 无状态认证**：登录签发 HS256 Token，`JwtAuthenticationFilter` 每次请求解析并装载
`LoginUser`（含角色、权限集合、学生/教师档案、带班班级 ID），业务层通过 `SecurityUtils` 直接取用。

**操作日志**：`@OperLog` 注解 + AOP 环绕通知自动落库，参数序列化后截断至 1900 字符，
自动过滤 `MultipartFile` 与 Servlet 对象，支持 `X-Forwarded-For` 真实 IP 解析。

**前端动态路由**：登录后拉取后端菜单树 → `import.meta.glob` 按 `component` 字段映射本地组件 →
`router.addRoute` 注入 → 路由守卫按 `meta.perm` 二次校验；退出登录时移除全部动态路由，避免角色切换串权。

**单一登录入口 + 按身份进后台**：系统只有 `/login` 一个入口，登录后由后端按账号返回
菜单、按钮权限与数据范围，前端 `addDynamicRoutes` 据此装载动态路由 —— 四个身份进去后是
四个不同的后台（学生 7 个页面 / 班主任 10 / 教务处 20 / 管理员 27）。
身份元数据（roleCode、中文名、标签配色、演示账号）集中在 `frontend/src/utils/loginRoles.js`，
新增身份只需追加一项；旧的四入口地址保留 302 式前端重定向，老书签不失效。
需要说明的是，**前端只负责展示，真正的权限边界在后端 RBAC + 数据权限** ——
即使有人绕过前端直接调接口，菜单与数据范围依然按账号真实身份下发，不存在越权。

**列表页复用**：`useTable` 组合式函数统一了分页查询、加载状态、条件重置、多选与删除后翻页回退逻辑，
30 个页面中 25 个列表页基于它实现。

---

## 九、注意事项

1. **端口覆盖**：若环境变量中存在 `SERVER__PORT`，会覆盖 `application.yml` 的 `server.port`。
   建议启动时显式追加 `--server.port=8080`。
2. **数据库连接**：`characterEncoding` 必须使用 Java 字符集名（`UTF-8`），
   而非 MySQL 字符集名（`utf8mb4`）；排序规则通过 `connectionCollation=utf8mb4_general_ci` 指定。
3. **生产部署**：请修改 `application.yml` 中的 `aas.jwt.secret` 与数据库密码，
   `aas.cors.allowed-origins` 需按实际前端域名调整。
4. **前端部署**：`npm run build` 产物在 `frontend/dist`，可用 Nginx 托管并将 `/api` 反向代理到后端 8080 端口。
5. **验证范围说明**：本项目的后端接口与前后端契约均已通过脚本化验证（见第七章）。
   但**前端页面的浏览器实际渲染效果未经自动化验证** —— 开发环境沙箱内 Chromium 无法启动
   （Chrome / Edge / agent-browser 三种方式均在同一处 `DevToolsActivePort` 失败）。
   前端仅通过 `vite build` 全量编译与静态契约校验确认无误；
   页面视觉效果、点击交互与 ECharts 渲染请在本地启动后人工确认（`start-frontend.bat`）。
