@echo off
chcp 65001 >nul
title 餐廳分析器打包工具

echo 🚀 開始打包餐廳分析器應用程式...
echo 📦 Restaurant Analyzer Packaging Started...

REM 設定變數
set "APP_NAME=Restaurant Analyzer"
set "VERSION=1.0.0"
set "BUILD_DIR=build"
set "DIST_DIR=dist"
set "PACKAGE_DIR=%DIST_DIR%\RestaurantAnalyzer-%VERSION%"

REM 清理舊的構建文件
echo 🧹 清理舊的構建文件...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%PACKAGE_DIR%"

REM 檢查 Java 和 Gradle
gradlew.bat --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 錯誤: 無法執行 Gradle
    echo 請確保 gradlew.bat 存在且可執行
    pause
    exit /b 1
)

REM 編譯 Java 應用程式
echo ☕ 編譯 Java 應用程式...
call gradlew.bat clean build --no-daemon
if %errorlevel% neq 0 (
    echo ❌ Java 編譯失敗
    pause
    exit /b 1
)

REM 創建可執行 JAR
echo 📦 創建可執行 JAR...
call gradlew.bat createExecutableJar --no-daemon
if %errorlevel% neq 0 (
    echo ❌ JAR 創建失敗
    pause
    exit /b 1
)

REM 創建打包目錄結構
echo 📁 創建打包目錄結構...
mkdir "%PACKAGE_DIR%\bin"
mkdir "%PACKAGE_DIR%\lib"
mkdir "%PACKAGE_DIR%\data-collector"
mkdir "%PACKAGE_DIR%\scripts"
mkdir "%PACKAGE_DIR%\docs"

REM 複製 JAR 文件
echo 📋 複製應用程式文件...
copy "build\libs\*.jar" "%PACKAGE_DIR%\lib\" >nul 2>&1

REM 複製 Python 數據收集器
echo 🐍 複製 Python 數據收集器...
if exist "data-collector" (
    xcopy "data-collector\*" "%PACKAGE_DIR%\data-collector\" /s /e /i /q >nul 2>&1
) else (
    echo ⚠️ data-collector 目錄不存在，跳過
)

if exist "requirements.txt" (
    copy "requirements.txt" "%PACKAGE_DIR%\" >nul 2>&1
) else (
    echo ⚠️ requirements.txt 不存在，跳過
)

REM 複製腳本和資源
echo 📜 複製腳本和資源...
if exist "scripts" (
    xcopy "scripts\*" "%PACKAGE_DIR%\scripts\" /s /e /i /q >nul 2>&1
) else (
    echo ⚠️ scripts 目錄不存在，跳過
)

if exist "*.md" (
    copy "*.md" "%PACKAGE_DIR%\docs\" >nul 2>&1
) else (
    echo ⚠️ 文檔文件不存在，跳過
)

REM 創建 Windows 啟動腳本
echo 🪟 創建 Windows 啟動腳本...
(
echo @echo off
echo chcp 65001 ^>nul
echo title Restaurant Analyzer
echo.
echo echo 🚀 正在啟動餐廳分析器...
echo.
echo REM 獲取腳本所在目錄
echo set "APP_DIR=%%~dp0"
echo set "APP_DIR=%%APP_DIR:~0,-1%%"
echo.
echo echo 📍 應用程式目錄: %%APP_DIR%%
echo.
echo REM 檢查 Java 是否安裝
echo java -version ^>nul 2^>^&1
echo if %%errorlevel%% neq 0 ^(
echo     echo ❌ 錯誤: 未找到 Java
echo     echo 請安裝 Java 21 或更高版本
echo     echo 下載地址: https://www.oracle.com/java/technologies/downloads/
echo     pause
echo     exit /b 1
echo ^)
echo.
echo REM 檢查 Python 是否安裝
echo python --version ^>nul 2^>^&1
echo if %%errorlevel%% equ 0 ^(
echo     echo ✅ 檢測到 Python
echo     
echo     REM 檢查虛擬環境
echo     if not exist "%%APP_DIR%%\.venv" ^(
echo         echo 🐍 創建 Python 虛擬環境...
echo         python -m venv "%%APP_DIR%%\.venv"
echo         call "%%APP_DIR%%\.venv\Scripts\activate.bat"
echo         if exist "%%APP_DIR%%\requirements.txt" ^(
echo             echo 📦 安裝 Python 依賴...
echo             pip install -r "%%APP_DIR%%\requirements.txt"
echo         ^)
echo     ^) else ^(
echo         echo ✅ 使用現有 Python 虛擬環境
echo     ^)
echo ^) else ^(
echo     echo ⚠️ 警告: 未檢測到 Python，數據收集功能可能無法使用
echo ^)
echo.
echo REM 設定 Java 啟動參數
echo set "JAVA_OPTS=-Xmx2g -Xms512m"
echo set "JAVA_OPTS=%%JAVA_OPTS%% --add-modules java.net.http,java.prefs,javafx.controls,javafx.fxml,javafx.swing,javafx.graphics,javafx.base"
echo set "JAVA_OPTS=%%JAVA_OPTS%% --add-modules ALL-MODULE-PATH"
echo set "JAVA_OPTS=%%JAVA_OPTS%% --add-opens java.base/java.lang=ALL-UNNAMED"
echo set "JAVA_OPTS=%%JAVA_OPTS%% --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED"
echo set "JAVA_OPTS=%%JAVA_OPTS%% --add-exports javafx.swing/javafx.embed.swing=ALL-UNNAMED"
echo.
echo REM 啟動應用程式
echo echo ▶️ 啟動餐廳分析器...
echo cd /d "%%APP_DIR%%"
echo java %%JAVA_OPTS%% -jar "lib\Restaurant Analyzer-1.0.0.jar"
echo.
echo echo 👋 餐廳分析器已關閉
echo pause
) > "%PACKAGE_DIR%\RestaurantAnalyzer.bat"

REM 創建 Mac/Linux 啟動腳本（便於跨平台分發）
echo 🖥️ 創建 Mac/Linux 啟動腳本...
(
echo #!/bin/bash
echo.
echo # Restaurant Analyzer 啟動腳本
echo # Get the directory where this script is located
echo SCRIPT_DIR="$$(cd "$$(dirname "$${BASH_SOURCE[0]}"^)" ^&^& pwd^)"
echo APP_DIR="$$SCRIPT_DIR"
echo.
echo echo "🚀 正在啟動餐廳分析器..."
echo echo "📍 應用程式目錄: $$APP_DIR"
echo.
echo # 檢查 Java 是否安裝
echo if ! command -v java ^&^> /dev/null; then
echo     echo "❌ 錯誤: 未找到 Java"
echo     echo "請安裝 Java 21 或更高版本"
echo     echo "下載地址: https://www.oracle.com/java/technologies/downloads/"
echo     read -p "按任意鍵退出..."
echo     exit 1
echo fi
echo.
echo # 檢查 Java 版本
echo JAVA_VERSION=$$(java -version 2^>^&1 ^| head -n1 ^| awk -F '"' '{print $$2}' ^| cut -d'.' -f1^)
echo if [ "$$JAVA_VERSION" -lt 21 ]; then
echo     echo "⚠️ 警告: 檢測到 Java $$JAVA_VERSION，建議使用 Java 21 或更高版本"
echo fi
echo.
echo # 檢查 Python 是否安裝 (用於數據收集器^)
echo if command -v python3 ^&^> /dev/null; then
echo     echo "✅ 檢測到 Python 3"
echo     # 檢查是否有虛擬環境
echo     if [ ! -d "$$APP_DIR/.venv" ]; then
echo         echo "🐍 創建 Python 虛擬環境..."
echo         python3 -m venv "$$APP_DIR/.venv"
echo         source "$$APP_DIR/.venv/bin/activate"
echo         if [ -f "$$APP_DIR/requirements.txt" ]; then
echo             echo "📦 安裝 Python 依賴..."
echo             pip install -r "$$APP_DIR/requirements.txt"
echo         fi
echo     else
echo         echo "✅ 使用現有 Python 虛擬環境"
echo     fi
echo else
echo     echo "⚠️ 警告: 未檢測到 Python 3，數據收集功能可能無法使用"
echo fi
echo.
echo # 設定 Java 啟動參數
echo JAVA_OPTS="-Xmx2g -Xms512m"
echo JAVA_OPTS="$$JAVA_OPTS --add-modules java.net.http,java.prefs,javafx.controls,javafx.fxml,javafx.swing,javafx.graphics,javafx.base"
echo JAVA_OPTS="$$JAVA_OPTS --add-modules ALL-MODULE-PATH"
echo JAVA_OPTS="$$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
echo JAVA_OPTS="$$JAVA_OPTS --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED"
echo JAVA_OPTS="$$JAVA_OPTS --add-exports javafx.swing/javafx.embed.swing=ALL-UNNAMED"
echo.
echo # macOS 特定設定
echo if [[ "$$OSTYPE" == "darwin"* ]]; then
echo     JAVA_OPTS="$$JAVA_OPTS -Dapple.awt.application.name=Restaurant\ Analyzer"
echo     JAVA_OPTS="$$JAVA_OPTS -Xdock:name=Restaurant\ Analyzer"
echo fi
echo.
echo # 啟動應用程式
echo echo "▶️ 啟動餐廳分析器..."
echo cd "$$APP_DIR"
echo java $$JAVA_OPTS -jar "lib/Restaurant Analyzer-1.0.0.jar"
echo.
echo echo "👋 餐廳分析器已關閉"
) > "%PACKAGE_DIR%\RestaurantAnalyzer.sh"

REM 創建安裝說明
echo 📖 創建安裝說明...
(
echo 餐廳分析器 Restaurant Analyzer v1.0.0
echo =====================================
echo.
echo 🎯 系統需求 System Requirements:
echo - Java 21 或更高版本 (Java 21 or higher^)
echo - Python 3.8+ (可選，用於數據收集功能 Optional, for data collection features^)
echo - 至少 2GB RAM (At least 2GB RAM^)
echo - 300MB 磁碟空間 (300MB disk space^)
echo.
echo 🚀 快速開始 Quick Start:
echo.
echo Windows 用戶:
echo 1. 雙擊 RestaurantAnalyzer.bat
echo 2. 首次運行會自動設置 Python 環境（如果已安裝 Python）
echo.
echo Mac/Linux 用戶:
echo 1. 打開終端機，進入應用程式目錄
echo 2. 執行: ./RestaurantAnalyzer.sh
echo 3. 首次運行會自動設置 Python 環境（如果已安裝 Python）
echo.
echo 📦 目錄結構 Directory Structure:
echo ├── lib/                    # Java JAR 文件
echo ├── data-collector/         # Python 數據收集器
echo ├── scripts/               # 工具腳本
echo ├── docs/                  # 文檔
echo ├── RestaurantAnalyzer.sh  # Mac/Linux 啟動腳本
echo ├── RestaurantAnalyzer.bat # Windows 啟動腳本
echo └── requirements.txt       # Python 依賴
echo.
echo 🔧 故障排除 Troubleshooting:
echo.
echo 1. Java 相關問題:
echo    - 確保安裝了 Java 21+
echo    - 下載地址: https://www.oracle.com/java/technologies/downloads/
echo.
echo 2. Python 相關問題:
echo    - 確保安裝了 Python 3.8+
echo    - 下載地址: https://www.python.org/downloads/
echo.
echo 3. 權限問題 (Mac/Linux^):
echo    - 執行: chmod +x RestaurantAnalyzer.sh
echo.
echo 4. 編碼問題:
echo    - 確保系統支援 UTF-8 編碼
echo.
echo 📞 技術支援 Technical Support:
echo 如有問題，請查看 docs/ 目錄中的詳細文檔。
echo For issues, please check detailed documentation in docs/ directory.
) > "%PACKAGE_DIR%\README.txt"

REM 創建解除安裝腳本
echo 🗑️ 創建解除安裝腳本...
(
echo @echo off
echo echo 🗑️ 解除安裝餐廳分析器...
echo set /p "confirm=確定要刪除所有文件嗎？(y/N^): "
echo if /i "%%confirm%%"=="y" ^(
echo     cd ..
echo     rmdir /s /q "%%~dp0"
echo     echo ✅ 解除安裝完成
echo     pause
echo ^) else ^(
echo     echo ❌ 解除安裝已取消
echo     pause
echo ^)
) > "%PACKAGE_DIR%\uninstall.bat"

(
echo #!/bin/bash
echo echo "🗑️ 解除安裝餐廳分析器..."
echo read -p "確定要刪除所有文件嗎？(y/N^): " -n 1 -r
echo echo
echo if [[ $$REPLY =~ ^^[Yy]$$ ]]; then
echo     cd ..
echo     rm -rf "$$(basename "$$PWD"^)"
echo     echo "✅ 解除安裝完成"
echo else
echo     echo "❌ 解除安裝已取消"
echo fi
) > "%PACKAGE_DIR%\uninstall.sh"

REM 創建壓縮包
echo 📦 創建發布包...
cd "%DIST_DIR%"

REM 檢查是否有 7-Zip 或 WinRAR
where 7z >nul 2>&1
if %errorlevel% equ 0 (
    echo 使用 7-Zip 創建壓縮包...
    7z a -tzip "RestaurantAnalyzer-%VERSION%-crossplatform.zip" "RestaurantAnalyzer-%VERSION%"
) else (
    where winrar >nul 2>&1
    if %errorlevel% equ 0 (
        echo 使用 WinRAR 創建壓縮包...
        winrar a -afzip "RestaurantAnalyzer-%VERSION%-crossplatform.zip" "RestaurantAnalyzer-%VERSION%"
    ) else (
        echo ⚠️ 未找到 7-Zip 或 WinRAR，需要手動創建壓縮包
        echo 請將 RestaurantAnalyzer-%VERSION% 目錄壓縮為 ZIP 格式
    )
)

cd ..

echo ✅ 打包完成！
echo 📦 發布包位置:
echo    🌐 跨平台: %DIST_DIR%\RestaurantAnalyzer-%VERSION%-crossplatform.zip
echo    📁 原始目錄: %PACKAGE_DIR%

echo.
echo 🎉 打包成功！現在您可以將發布包分發給用戶。
echo 📋 用戶只需解壓縮並運行對應的啟動腳本即可。

pause 