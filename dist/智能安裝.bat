@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

REM Restaurant Analyzer - Windows 零安裝自動化包裝腳本
REM 自動下載 Java Runtime 並創建自包含包

set VERSION=1.0.0
set PACKAGE_DIR=dist\RestaurantAnalyzer-ZeroInstall-Windows-%VERSION%
set JAVA_VERSION=21

echo 🚀 開始創建 Windows 零安裝包 (無需預先安裝 Java)

REM 創建輸出目錄
if exist "%PACKAGE_DIR%" rmdir /s /q "%PACKAGE_DIR%"
mkdir "%PACKAGE_DIR%"

REM 檢測 Windows 架構
set ARCH=%PROCESSOR_ARCHITECTURE%
if "%ARCH%"=="AMD64" (
    set JAVA_URL=https://download.oracle.com/java/21/latest/jdk-21_windows-x64_bin.zip
    set JAVA_FILENAME=jdk-21_windows-x64.zip
) else if "%ARCH%"=="ARM64" (
    set JAVA_URL=https://download.oracle.com/java/21/latest/jdk-21_windows-aarch64_bin.zip
    set JAVA_FILENAME=jdk-21_windows-aarch64.zip
) else (
    echo ❌ 不支援的 Windows 架構: %ARCH%
    pause
    exit /b 1
)

echo 🔍 檢測到系統: Windows %ARCH%
echo 📥 下載 Java Runtime...
echo    URL: %JAVA_URL%

REM 下載 Java (使用 PowerShell)
powershell -Command "Invoke-WebRequest -Uri '%JAVA_URL%' -OutFile '%TEMP%\%JAVA_FILENAME%'"

if %errorlevel% neq 0 (
    echo ❌ Java 下載失敗
    pause
    exit /b 1
)

REM 解壓 Java 到包目錄
echo 📦 解壓 Java Runtime...
mkdir "%PACKAGE_DIR%\java"
powershell -Command "Expand-Archive -Path '%TEMP%\%JAVA_FILENAME%' -DestinationPath '%PACKAGE_DIR%\java' -Force"

REM 移動 Java 檔案到正確位置（去除嵌套目錄）
for /d %%i in ("%PACKAGE_DIR%\java\jdk-*") do (
    robocopy "%%i" "%PACKAGE_DIR%\java" /E /MOVE >nul
)

REM 清理下載的檔案
del "%TEMP%\%JAVA_FILENAME%"

REM 構建應用程式
echo 🔨 構建應用程式...
call gradlew.bat createFatJar

if %errorlevel% neq 0 (
    echo ❌ 應用程式構建失敗
    pause
    exit /b 1
)

REM 複製 JAR 檔案
echo 📋 複製應用程式檔案...
mkdir "%PACKAGE_DIR%\app"

REM 找到並複製 standalone JAR
for %%f in (build\libs\*-standalone-*-all.jar) do (
    copy "%%f" "%PACKAGE_DIR%\app\Restaurant-Analyzer.jar"
)

REM 複製源代碼和資源
xcopy "src" "%PACKAGE_DIR%\app\src\" /E /I /Q
xcopy "data-collector" "%PACKAGE_DIR%\app\data-collector\" /E /I /Q
copy "build.gradle" "%PACKAGE_DIR%\app\"
copy "settings.gradle" "%PACKAGE_DIR%\app\"
copy "gradlew" "%PACKAGE_DIR%\app\"
copy "gradlew.bat" "%PACKAGE_DIR%\app\"
xcopy "gradle" "%PACKAGE_DIR%\app\gradle\" /E /I /Q
copy "requirements.txt" "%PACKAGE_DIR%\app\"
copy "應用程式背景.png" "%PACKAGE_DIR%\app\" 2>nul || echo ⚠️ 背景圖片不存在，跳過

REM 創建 Windows 啟動腳本
echo 🪟 創建 Windows 啟動腳本...
(
echo @echo off
echo chcp 65001 ^>nul
echo title Restaurant Analyzer
echo.
echo REM Restaurant Analyzer - 智能啟動腳本
echo REM 自動檢測系統 Java，無 Java 或版本過舊則使用內建 Java
echo.
echo set "SCRIPT_DIR=%%~dp0"
echo set "SCRIPT_DIR=%%SCRIPT_DIR:~0,-1%%"
echo set "APP_DIR=%%SCRIPT_DIR%%\app"
echo set "BUNDLED_JAVA_DIR=%%SCRIPT_DIR%%\java"
echo set "USE_BUNDLED_JAVA=false"
echo.
echo echo 🚀 正在啟動餐廳分析器...
echo echo 📍 應用程式目錄: %%APP_DIR%%
echo.
echo REM 檢測系統 Java 版本
echo echo 🔍 檢測系統 Java...
echo java -version ^>nul 2^>^&1
echo if %%errorlevel%% equ 0 ^(
echo     echo ✅ 檢測到系統 Java，正在檢查版本...
echo     
echo     REM 獲取 Java 版本並檢查是否 ^>= 21
echo     for /f "tokens=3" %%%%a in ^('java -version 2^>^&1 ^| findstr /i "version"'^) do set JAVA_VERSION=%%%%a
echo     set JAVA_VERSION=%%JAVA_VERSION:"=%%
echo     
echo     REM 提取主版本號
echo     for /f "tokens=1 delims=." %%%%a in ^("%%JAVA_VERSION%%"^) do set MAJOR_VERSION=%%%%a
echo     
echo     REM Java 9+ 版本格式 ^(如 21.0.1^)
echo     if %%MAJOR_VERSION%% geq 21 ^(
echo         echo ✅ 系統 Java 版本 %%JAVA_VERSION%% 符合要求 ^(需要 21+^)
echo         set "JAVA_CMD=java"
echo     ^) else ^(
echo         echo ⚠️ 系統 Java 版本 %%JAVA_VERSION%% 過舊 ^(需要 21+^)，使用內建 Java
echo         set "USE_BUNDLED_JAVA=true"
echo     ^)
echo ^) else ^(
echo     echo ⚠️ 未檢測到系統 Java，使用內建 Java
echo     set "USE_BUNDLED_JAVA=true"
echo ^)
echo.
echo REM 設定 Java 路徑
echo if "%%USE_BUNDLED_JAVA%%"=="true" ^(
echo     if not exist "%%BUNDLED_JAVA_DIR%%\bin\java.exe" ^(
echo         echo ❌ 錯誤: 找不到內建 Java Runtime
echo         echo 請重新下載完整的 Restaurant Analyzer 零安裝包
echo         pause
echo         exit /b 1
echo     ^)
echo     set "JAVA_CMD=%%BUNDLED_JAVA_DIR%%\bin\java.exe"
echo     echo ☕ 使用內建 Java: %%BUNDLED_JAVA_DIR%%
echo ^) else ^(
echo     echo ☕ 使用系統 Java
echo ^)
echo.
echo REM 顯示實際使用的 Java 版本
echo echo ✅ 使用的 Java 版本:
echo "%%JAVA_CMD%%" -version
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
echo.
echo REM 嘗試直接運行 JAR，如果失敗則使用 Gradle
echo "%%JAVA_CMD%%" %%JAVA_OPTS%% -jar "Restaurant-Analyzer.jar" 2^>nul
echo if %%errorlevel%% neq 0 ^(
echo     echo 🔄 JAR 檔案啟動失敗，使用 Gradle 啟動...
echo     if "%%USE_BUNDLED_JAVA%%"=="true" ^(
echo         set "JAVA_HOME=%%BUNDLED_JAVA_DIR%%"
echo         set "PATH=%%BUNDLED_JAVA_DIR%%\bin;%%PATH%%"
echo     ^) else ^(
echo         REM 系統 Java 應該已經在 PATH 中
echo         echo 使用系統 Java 執行 Gradle...
echo     ^)
echo     call gradlew.bat run --no-daemon
echo ^)
echo.
echo echo 👋 餐廳分析器已關閉
echo pause
) > "%PACKAGE_DIR%\RestaurantAnalyzer.bat"

REM 創建 README
echo 📖 創建說明文件...
(
echo 🍽️ Restaurant Analyzer v%VERSION% - Windows 智能零安裝版本
echo ===============================================================
echo.
echo 📋 這是一個智能自包含版本！會優先使用您系統的 Java，沒有或版本過舊才使用內建 Java
echo.
echo 🚀 快速開始:
echo.
echo Windows 用戶:
echo 1. 雙擊 RestaurantAnalyzer.bat 即可啟動
echo 2. 首次啟動會自動檢測 Java 版本和設置環境（約 1-2 分鐘）
echo.
echo 🎯 智能 Java 檢測:
echo ✅ 自動檢測系統 Java 版本
echo ✅ Java 21+ 系統版本: 使用系統 Java（節省空間）
echo ✅ 無 Java 或版本 ^< 21: 使用內建 Java Runtime
echo ✅ 無需手動安裝或配置任何環境
echo.
echo 🎯 已包含組件:
echo ✅ Java %JAVA_VERSION% Runtime （備用內建）
echo ✅ Restaurant Analyzer 應用程式
echo ✅ 所有必要的依賴庫
echo ✅ AI 分析引擎 ^(Ollama - 自動下載^)
echo ✅ Python 數據收集器 ^(如系統有 Python^)
echo.
echo 🔧 系統需求:
echo - Windows 10/11 ^(任何版本^)
echo - 至少 2GB RAM
echo - 150-500MB 磁碟空間 ^(根據是否使用內建 Java^)
echo - 網路連接 ^(首次啟動下載 AI 模型^)
echo.
echo 📂 目錄結構:
echo ├── java/                         # 內建 Java Runtime ^(備用^)
echo ├── app/                          # Restaurant Analyzer 應用程式
echo │   ├── Restaurant-Analyzer.jar          # 主程式
echo │   ├── src/                            # 源代碼
echo │   ├── data-collector/                 # Python 數據收集器
echo │   └── requirements.txt                # Python 依賴
echo ├── RestaurantAnalyzer.bat        # 智能啟動腳本
echo └── README.txt                    # 本文件
echo.
echo ⚡ 優勢:
echo 🎯 智能 Java 管理 - 自動選擇最佳 Java 版本
echo 🚀 一鍵啟動 - 雙擊即用，無需配置
echo 📦 自包含 - 所有依賴已預先配置
echo 🔄 自動更新 - AI 模型和依賴自動管理
echo 💾 空間效率 - 優先使用系統 Java 節省空間
echo.
echo 🆘 疑難排解:
echo.
echo Q: 啟動時出現 Java 版本錯誤？
echo A: 腳本會自動處理，如系統 Java 版本過舊會自動切換到內建 Java
echo.
echo Q: 首次啟動很慢？
echo A: 正常！首次需要下載 AI 模型（約 2.6GB），後續啟動會很快
echo.
echo Q: 沒有網路連接？
echo A: 首次啟動需要網路下載 AI 模型，之後可離線使用基本功能
echo.
echo 📧 技術支援: 
echo 如有問題請查看終端輸出訊息，通常包含詳細的錯誤說明
echo.
echo 🎉 享受您的餐廳分析體驗！
) > "%PACKAGE_DIR%\README.txt"

REM 創建壓縮包
echo 📦 創建最終壓縮包...
powershell -Command "Compress-Archive -Path '%PACKAGE_DIR%' -DestinationPath '%PACKAGE_DIR%.zip' -Force"

REM 計算大小
for %%f in ("%PACKAGE_DIR%.zip") do set ZIP_SIZE=%%~zf
set /a ZIP_SIZE_MB=!ZIP_SIZE!/1048576

echo.
echo ✅ Windows 零安裝包創建完成！
echo.
echo 📦 檔案:
echo    📁 目錄: %PACKAGE_DIR%
echo    📦 壓縮包: %PACKAGE_DIR%.zip
echo.
echo 📊 大小:
echo    📦 壓縮包: !ZIP_SIZE_MB! MB
echo.
echo 🚀 使用方法:
echo    1. 解壓縮: %PACKAGE_DIR%.zip
echo    2. 進入目錄並雙擊: RestaurantAnalyzer.bat
echo.
echo ✨ 特色: 無需安裝 Java，下載即可運行！
echo.
pause 