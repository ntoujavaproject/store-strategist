#!/bin/bash

# Restaurant Analyzer 啟動腳本
# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$SCRIPT_DIR"

echo "🚀 正在啟動餐廳分析器..."
echo "📍 應用程式目錄: $APP_DIR"

# 檢查 Java 是否安裝
if ! command -v java &> /dev/null; then
    echo "❌ 錯誤: 未找到 Java"
    echo "請安裝 Java 21 或更高版本"
    echo "下載地址: https://www.oracle.com/java/technologies/downloads/"
    read -p "按任意鍵退出..."
    exit 1
fi

# 檢查 Java 版本
JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "⚠️  警告: 檢測到 Java $JAVA_VERSION，建議使用 Java 21 或更高版本"
fi

# 檢查 Python 是否安裝 (用於數據收集器)
if command -v python3 &> /dev/null; then
    echo "✅ 檢測到 Python 3"
    # 檢查是否有虛擬環境
    if [ ! -d "$APP_DIR/.venv" ]; then
        echo "🐍 創建 Python 虛擬環境..."
        python3 -m venv "$APP_DIR/.venv"
        source "$APP_DIR/.venv/bin/activate"
        if [ -f "$APP_DIR/requirements.txt" ]; then
            echo "📦 安裝 Python 依賴..."
            pip install -r "$APP_DIR/requirements.txt"
        fi
    else
        echo "✅ 使用現有 Python 虛擬環境"
    fi
else
    echo "⚠️  警告: 未檢測到 Python 3，數據收集功能可能無法使用"
fi

# 設定 Java 啟動參數
JAVA_OPTS="-Xmx2g -Xms512m"
JAVA_OPTS="$JAVA_OPTS --add-modules java.net.http,java.prefs,javafx.controls,javafx.fxml,javafx.swing,javafx.graphics,javafx.base"
JAVA_OPTS="$JAVA_OPTS --add-modules ALL-MODULE-PATH"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-exports javafx.swing/javafx.embed.swing=ALL-UNNAMED"

# macOS 特定設定
if [[ "$OSTYPE" == "darwin"* ]]; then
    JAVA_OPTS="$JAVA_OPTS -Dapple.awt.application.name=Restaurant\ Analyzer"
    JAVA_OPTS="$JAVA_OPTS -Xdock:name=Restaurant\ Analyzer"
fi

# 啟動應用程式
echo "▶️  啟動餐廳分析器..."
cd "$APP_DIR"
java $JAVA_OPTS -jar "lib/Restaurant Analyzer-1.0.0.jar"

echo "👋 餐廳分析器已關閉"
