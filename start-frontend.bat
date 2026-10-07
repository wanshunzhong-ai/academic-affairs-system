@echo off
setlocal enabledelayedexpansion

rem ===================================================================
rem  教务管理系统 - 前端一键启动
rem
rem  用法:  start-frontend.bat            启动开发服务器（自动安装依赖）
rem         start-frontend.bat install    仅安装/更新依赖
rem         start-frontend.bat build      构建生产包到 frontend\dist
rem
rem  说明:  Node.js 会自动探测，顺序为
rem           PATH - 常见安装位置（nvm / 官方安装包 / scoop）- WorkBuddy 托管版本
rem         换台电脑通常无需修改本文件。
rem
rem  编码:  本文件保存为 GBK/ANSI，请勿改成 UTF-8，也不要添加 chcp 命令。
rem         cmd 在切换代码页后读取含中文的批处理文件会定位错位，
rem         导致命令被截断，甚至让脚本调用自己（本项目实测踩过这个坑）。
rem ===================================================================

set "PROJ=%~dp0"
set "FRONTEND=%PROJ%frontend"
set "MODE=dev"

if /I "%~1"=="install" set "MODE=install"
if /I "%~1"=="build" set "MODE=build"

echo ============================================================
echo   教务管理系统 - 前端
echo ============================================================
echo   项目目录 : %FRONTEND%
echo ------------------------------------------------------------

if not exist "%FRONTEND%\package.json" (
    echo [错误] 未找到前端工程: %FRONTEND%
    pause
    exit /b 1
)

rem ------------------- 1. 探测 Node.js -------------------
set "NODE_EXE="

rem 1.1 PATH 中的 node
for %%N in (node.exe) do if not "%%~$PATH:N"=="" set "NODE_EXE=%%~$PATH:N"

rem 1.2 常见安装位置
if not defined NODE_EXE for %%C in (
    "%ProgramFiles%\nodejs\node.exe"
    "C:\nvm4w\nodejs\node.exe"
    "%APPDATA%\nvm\current\node.exe"
    "%LOCALAPPDATA%\Programs\nodejs\node.exe"
    "%USERPROFILE%\scoop\apps\nodejs\current\node.exe"
    "D:\SoftWare\nodejs\node.exe"
) do if not defined NODE_EXE if exist "%%~C" set "NODE_EXE=%%~C"

rem 1.3 WorkBuddy 托管版本（本机开发环境常见）
if not defined NODE_EXE for /d %%X in ("%USERPROFILE%\.workbuddy\binaries\node\versions\*") do (
    if not defined NODE_EXE if exist "%%~X\node.exe" set "NODE_EXE=%%~X\node.exe"
)

if not defined NODE_EXE (
    echo [错误] 未找到 Node.js，请先安装 Node 18 或更高版本。
    echo.
    pause
    exit /b 1
)

for %%P in ("%NODE_EXE%") do set "NODE_BIN=%%~dpP"
set "PATH=%NODE_BIN%;%PATH%"
for /f "delims=" %%V in ('node -v') do set "NODE_VER=%%V"
echo   Node 版本 : %NODE_VER%
echo   Node 路径 : %NODE_EXE%
echo ------------------------------------------------------------

pushd "%FRONTEND%"

rem ------------------- 2. 安装依赖 -------------------
if not exist "node_modules" goto do_install
if "%MODE%"=="install" goto do_install
goto after_install

:do_install
echo.
echo [1/2] 正在安装依赖（首次约 1-2 分钟）...
call "%NODE_BIN%npm.cmd" install
if not "!errorlevel!"=="0" (
    echo [错误] npm install 失败。
    popd
    pause
    exit /b 1
)
rem esbuild 的平台二进制在部分环境下不会被自动安装，这里做一次补偿
if not exist "node_modules\@esbuild" (
    echo       补装 esbuild 平台二进制...
    call "%NODE_BIN%npm.cmd" install @esbuild/win32-x64@0.21.5 --no-save
)
echo [1/2] 依赖安装完成。

:after_install
if "%MODE%"=="install" (
    echo 依赖已就绪，退出。
    popd
    pause
    endlocal
    exit /b 0
)

if "%MODE%"=="build" (
    echo.
    echo [2/2] 正在构建生产包...
    call "%NODE_BIN%npm.cmd" run build
    popd
    echo ------------------------------------------------------------
    echo 构建完成，产物目录: %FRONTEND%\dist
    pause
    endlocal
    exit /b 0
)

echo.
echo [2/2] 正在启动开发服务器...
echo       前端地址 : http://localhost:5173
echo       登录入口 : http://localhost:5173/login
echo       接口代理 : /api -^> http://localhost:8080（需先启动后端）
echo       按 Ctrl+C 可停止服务
echo ------------------------------------------------------------
call "%NODE_BIN%npm.cmd" run dev

popd
echo.
echo 开发服务器已停止。
pause
endlocal
exit /b 0
