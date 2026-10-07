@echo off
setlocal enabledelayedexpansion

rem ===================================================================
rem  教务管理系统 - 后端一键启动
rem
rem  用法:  start-backend.bat                  直接启动（缺少 jar 时自动构建）
rem         start-backend.bat 8081             换端口启动
rem         start-backend.bat rebuild          强制重新构建后启动
rem         start-backend.bat rebuild 8081     重新构建并换端口
rem
rem  说明:  JDK 与 Maven 会自动探测，顺序为
rem           JDK   : PATH -> JAVA_HOME -> JetBrains 自带 JBR -> 常见 JDK 安装目录
rem           Maven : PATH -> 独立安装目录 -> JetBrains 内置 Maven
rem         换台电脑通常无需修改本文件。
rem         如需手工指定，直接在本行下方设置 JAVA_EXE / MVN_BIN 即可。
rem
rem  编码:  本文件保存为 GBK/ANSI，请勿改成 UTF-8，也不要添加 chcp 命令。
rem         cmd 在切换代码页后读取含中文的批处理文件会定位错位，
rem         导致命令被截断，甚至让脚本调用自己（本项目实测踩过这个坑）。
rem ===================================================================

set "PROJ=%~dp0"
set "BACKEND=%PROJ%backend"
set "JAR=%BACKEND%\target\academic-affairs-system.jar"
set "PORT=8080"
set "REBUILD=0"
set "PF86=%ProgramFiles(x86)%"

rem 参数解析: rebuild 与端口可任意组合、任意顺序
rem   start-backend.bat              默认 8080
rem   start-backend.bat 8081         用 8081
rem   start-backend.bat rebuild      重新构建后启动
rem   start-backend.bat rebuild 8081 重新构建并用 8081 启动
for %%A in (%*) do (
    if /I "%%~A"=="rebuild" (
        set "REBUILD=1"
    ) else (
        set "PORT=%%~A"
    )
)

echo ============================================================
echo   教务管理系统 - 后端启动
echo ============================================================
echo   项目目录 : %BACKEND%
echo   服务端口 : %PORT%
echo   接口文档 : http://localhost:%PORT%/doc.html
echo ------------------------------------------------------------

rem ------------------- 1. 探测 JDK 21 -------------------
set "JAVA_EXE="

rem 1.1 PATH 中的 java
for %%J in (java.exe) do if not "%%~$PATH:J"=="" call :probe_java "%%~$PATH:J"

rem 1.2 环境变量 JAVA_HOME
if not defined JAVA_EXE if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" call :probe_java "%JAVA_HOME%\bin\java.exe"

rem 1.3 JetBrains 系 IDE 自带的 JBR（IntelliJ IDEA / PyCharm / WebStorm ...）
if not defined JAVA_EXE call :probe_java_jetbrains

rem 1.4 常见 JDK 21 安装目录
if not defined JAVA_EXE for /d %%X in (
    "%ProgramFiles%\Java\jdk-21*"
    "%ProgramFiles%\Eclipse Adoptium\jdk-21*"
    "%ProgramFiles%\Microsoft\jdk-21*"
    "%ProgramFiles%\Zulu\zulu-21*"
    "%ProgramFiles%\Amazon Corretto\jdk21*"
    "%ProgramFiles%\BellSoft\LibericaJDK-21*"
    "%PF86%\Java\jdk-21*"
    "D:\SoftWare\Java\jdk-21*"
    "D:\Java\jdk-21*"
) do (
    if not defined JAVA_EXE if exist "%%~X\bin\java.exe" call :probe_java "%%~X\bin\java.exe"
)

if not defined JAVA_EXE (
    echo [错误] 没有找到可用的 JDK 21。
    echo        本项目基于 Spring Boot 3.3.5，必须使用 JDK 21。
    echo        请安装 JDK 21 后重试，或在本脚本中直接设置 JAVA_EXE。
    echo.
    pause
    exit /b 1
)

"%JAVA_EXE%" -version > "%TEMP%\_aas_jver.txt" 2>&1
set /p "JVER="<"%TEMP%\_aas_jver.txt"
del "%TEMP%\_aas_jver.txt" >nul 2>nul
echo   JDK      : %JAVA_EXE%
echo              %JVER%
for %%P in ("%JAVA_EXE%") do set "JAVA_BIN=%%~dpP"
set "PATH=%JAVA_BIN%;%PATH%"
echo ------------------------------------------------------------

rem ------------------- 2. 需要时构建 -------------------
if "%REBUILD%"=="1" goto do_build
if not exist "%JAR%" goto do_build
echo [1/2] 已存在构建产物，跳过编译。
echo       %JAR%
goto run

:do_build
rem 构建前检测产物是否被占用：后端若仍在运行会锁住 jar，导致 mvn clean 失败
if exist "%JAR%" (
    ren "%JAR%" "__aas_locked__.tmp" >nul 2>nul
    if exist "%JAR%" (
        echo [错误] 构建产物正被占用，说明后端仍在运行中。
        echo        请先停止正在运行的后端（在它所在的窗口按 Ctrl+C），
        echo        然后重新执行本脚本。
        echo.
        pause
        exit /b 1
    )
    ren "%BACKEND%\target\__aas_locked__.tmp" "academic-affairs-system.jar" >nul 2>nul
)
call :probe_mvn
if not defined MVN_BIN (
    echo [错误] 未找到 Maven，无法构建后端。
    echo        可安装 Maven 3.9+，或直接改用 IDE 运行本项目，
    echo        也可在本脚本中设置 MVN_BIN 指向 mvn.cmd。
    echo.
    pause
    exit /b 1
)
echo [1/2] 正在编译打包（首次执行需下载依赖，请耐心等待）...
echo       Maven: %MVN_BIN%
pushd "%BACKEND%"
call "%MVN_BIN%" clean package -DskipTests
set "MVN_RC=!errorlevel!"
popd
if not "!MVN_RC!"=="0" (
    echo [错误] Maven 构建失败，退出码 !MVN_RC!
    pause
    exit /b 1
)
if not exist "%JAR%" (
    echo [错误] 构建完成但未找到产物: %JAR%
    pause
    exit /b 1
)
echo [1/2] 构建完成。

:run
echo [2/2] 正在启动后端服务...
echo       按 Ctrl+C 可停止服务
echo ------------------------------------------------------------
"%JAVA_EXE%" -jar "%JAR%" --server.port=%PORT%

echo.
echo 服务已停止。
pause
endlocal
exit /b 0

rem ===================================================================
rem  子程序
rem ===================================================================

:probe_java
rem 参数: java.exe 完整路径。版本号含 "21." 视为可用
if not exist "%~1" exit /b 1
"%~1" -version 2>&1 | findstr /c:"21." >nul 2>nul
if not errorlevel 1 (
    set "JAVA_EXE=%~1"
    exit /b 0
)
exit /b 1

:probe_java_jetbrains
rem 探测 JetBrains 系 IDE 自带的 JBR
for %%B in (
    "D:\SoftWare\JetBrains"
    "D:\JetBrains"
    "%ProgramFiles%\JetBrains"
    "%PF86%\JetBrains"
    "%LOCALAPPDATA%\Programs"
    "%LOCALAPPDATA%\JetBrains\Toolbox\apps"
) do (
    if exist "%%~B" for /d %%R in ("%%~B\*") do (
        if not defined JAVA_EXE if exist "%%~R\jbr\bin\java.exe" call :probe_java "%%~R\jbr\bin\java.exe"
    )
)
exit /b 0

:probe_mvn
rem 依次尝试: PATH -> 独立安装目录 -> JetBrains IDE 内置 Maven
set "MVN_BIN="
for %%M in (mvn.cmd) do if not "%%~$PATH:M"=="" set "MVN_BIN=%%~$PATH:M"
if not defined MVN_BIN for /d %%X in (
    "%ProgramFiles%\apache-maven*"
    "D:\SoftWare\apache-maven*"
    "D:\apache-maven*"
    "C:\apache-maven*"
    "%USERPROFILE%\apache-maven*"
    "%USERPROFILE%\.workbuddy\tools\apache-maven*"
    "%USERPROFILE%\scoop\apps\maven\current"
) do (
    if not defined MVN_BIN if exist "%%~X\bin\mvn.cmd" set "MVN_BIN=%%~X\bin\mvn.cmd"
)
if not defined MVN_BIN for %%B in (
    "D:\SoftWare\JetBrains"
    "D:\JetBrains"
    "%ProgramFiles%\JetBrains"
    "%PF86%\JetBrains"
    "%LOCALAPPDATA%\Programs"
    "%LOCALAPPDATA%\JetBrains\Toolbox\apps"
) do (
    if exist "%%~B" for /d %%R in ("%%~B\*") do (
        if not defined MVN_BIN if exist "%%~R\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_BIN=%%~R\plugins\maven\lib\maven3\bin\mvn.cmd"
    )
)
exit /b 0
