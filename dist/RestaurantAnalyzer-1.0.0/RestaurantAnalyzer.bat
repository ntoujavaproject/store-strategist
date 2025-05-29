@echo off
chcp 65001 >nul
title Restaurant Analyzer

echo 🚀 正在啟動餐廳分析器...

REM 獲取腳本所在目錄
set "APP_DIR=%~dp0"
set "APP_DIR=%APP_DIR:~0,-1%"

echo 📍 應用程式目錄: %APP_DIR%

REM 檢查 Java 是否安裝
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 錯誤: 未找到 Java
    echo 請安裝 Java 21 或更高版本
    echo 下載地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

REM 檢查 Python 是否安裝
python --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ 檢測到 Python
    
    REM 檢查虛擬環境
    if not exist "%APP_DIR%\.venv" (
        echo 🐍 創建 Python 虛擬環境...
        python -m venv "%APP_DIR%\.venv"
        call "%APP_DIR%\.venv\Scripts\activate.bat"
        if exist "%APP_DIR%\requirements.txt" (
            echo 📦 安裝 Python 依賴...
            pip install -r "%APP_DIR%\requirements.txt"
        )
    ) else (
        echo ✅ 使用現有 Python 虛擬環境
    )
) else (
    echo ⚠️ 警告: 未檢測到 Python，數據收集功能可能無法使用
)

REM 設定 Java 啟動參數
set "JAVA_OPTS=-Xmx2g -Xms512m"

REM 啟動應用程式
echo ▶️ 啟動餐廳分析器...
cd /d "%APP_DIR%"
gradlew.bat run --no-daemon

echo 👋 餐廳分析器已關閉
pause
