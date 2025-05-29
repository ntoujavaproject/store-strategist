#!/bin/bash

# 餐廳分析器打包腳本 (Mac/Linux)
# Restaurant Analyzer Packaging Script for Mac/Linux

echo "🚀 開始打包餐廳分析器應用程式..."
echo "📦 Restaurant Analyzer Packaging Started..."

# 設定變數
APP_NAME="Restaurant Analyzer"
VERSION="1.0.0"
BUILD_DIR="build"
DIST_DIR="dist"
PACKAGE_DIR="$DIST_DIR/RestaurantAnalyzer-$VERSION"

# 清理舊的構建文件
echo "🧹 清理舊的構建文件..."
rm -rf $BUILD_DIR
rm -rf $DIST_DIR
mkdir -p $PACKAGE_DIR

# 編譯 Java 應用程式
echo "☕ 編譯 Java 應用程式..."
./gradlew clean build --no-daemon
if [ $? -ne 0 ]; then
    echo "❌ Java 編譯失敗"
    exit 1
fi

# 創建可執行 JAR
echo "📦 創建包含所有依賴的 Fat JAR..."
./gradlew createFatJar --no-daemon
if [ $? -ne 0 ]; then
    echo "❌ Fat JAR 創建失敗"
    exit 1
fi

# 創建打包目錄結構
echo "📁 創建打包目錄結構..."
mkdir -p "$PACKAGE_DIR/bin"
mkdir -p "$PACKAGE_DIR/lib"
mkdir -p "$PACKAGE_DIR/data-collector"
mkdir -p "$PACKAGE_DIR/scripts"
mkdir -p "$PACKAGE_DIR/docs"

# 複製 Fat JAR 文件
echo "📋 複製應用程式文件..."
cp build/libs/*-all.jar "$PACKAGE_DIR/lib/RestaurantAnalyzer-$VERSION.jar"

# 複製 Python 數據收集器
echo "🐍 複製 Python 數據收集器..."
cp -r data-collector/* "$PACKAGE_DIR/data-collector/" 2>/dev/null || echo "⚠️  data-collector 目錄不存在，跳過"
cp requirements.txt "$PACKAGE_DIR/" 2>/dev/null || echo "⚠️  requirements.txt 不存在，跳過"

# 複製腳本和資源
echo "📜 複製腳本和資源..."
cp scripts/* "$PACKAGE_DIR/scripts/" 2>/dev/null || echo "⚠️  scripts 目錄不存在，跳過"
cp *.md "$PACKAGE_DIR/docs/" 2>/dev/null || echo "⚠️  文檔文件不存在，跳過"

# 複製 Gradle wrapper 和 build 文件
echo "📋 複製 Gradle 構建文件..."
cp build.gradle "$PACKAGE_DIR/"
cp settings.gradle "$PACKAGE_DIR/"
cp gradlew "$PACKAGE_DIR/"
cp gradlew.bat "$PACKAGE_DIR/"
cp -r gradle "$PACKAGE_DIR/"

# 複製源代碼目錄
echo "📋 複製源代碼..."
cp -r src "$PACKAGE_DIR/"

# 複製背景圖片
echo "🖼️  複製背景圖片..."
cp "應用程式背景.png" "$PACKAGE_DIR/" 2>/dev/null || echo "⚠️  背景圖片不存在，跳過"

# 創建 macOS .app bundle
echo "🍎 創建 macOS 應用程式包..."
APP_BUNDLE="$PACKAGE_DIR/Restaurant Analyzer.app"
mkdir -p "$APP_BUNDLE/Contents/MacOS"
mkdir -p "$APP_BUNDLE/Contents/Resources"
mkdir -p "$APP_BUNDLE/Contents/Java"

# 複製應用程式資源到 .app bundle
cp -r src "$APP_BUNDLE/Contents/Java/"
cp build.gradle "$APP_BUNDLE/Contents/Java/"
cp settings.gradle "$APP_BUNDLE/Contents/Java/"
cp gradlew "$APP_BUNDLE/Contents/Java/"
cp gradlew.bat "$APP_BUNDLE/Contents/Java/"
cp -r gradle "$APP_BUNDLE/Contents/Java/"
cp -r data-collector "$APP_BUNDLE/Contents/Java/"
cp requirements.txt "$APP_BUNDLE/Contents/Java/"
cp "應用程式背景.png" "$APP_BUNDLE/Contents/Java/" 2>/dev/null || echo "⚠️  背景圖片不存在，跳過"

# 複製應用程式圖標（如果存在）
if [ -f "src/main/resources/icons/restaurant_icon.icns" ]; then
    cp "src/main/resources/icons/restaurant_icon.icns" "$APP_BUNDLE/Contents/Resources/app_icon.icns"
fi

# 創建 Info.plist
cat > "$APP_BUNDLE/Contents/Info.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>Restaurant Analyzer</string>
    <key>CFBundleDisplayName</key>
    <string>Restaurant Analyzer</string>
    <key>CFBundleIdentifier</key>
    <string>com.restaurantanalyzer.app</string>
    <key>CFBundleVersion</key>
    <string>$VERSION</string>
    <key>CFBundleShortVersionString</key>
    <string>$VERSION</string>
    <key>CFBundleExecutable</key>
    <string>RestaurantAnalyzer</string>
    <key>CFBundleIconFile</key>
    <string>app_icon.icns</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleSignature</key>
    <string>????</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.15</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSSupportsAutomaticGraphicsSwitching</key>
    <true/>
</dict>
</plist>
EOF

# 創建主要可執行檔案
cat > "$APP_BUNDLE/Contents/MacOS/RestaurantAnalyzer" << 'EOF'
#!/bin/bash

# 獲取應用程式束的路徑
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../Java" && pwd)"

echo "🚀 正在啟動餐廳分析器..."
echo "📍 應用程式目錄: $APP_DIR"

# 檢查 Java 是否安裝
if ! command -v java &> /dev/null; then
    osascript -e 'display dialog "未找到 Java\n\n請安裝 Java 21 或更高版本\n下載地址: https://www.oracle.com/java/technologies/downloads/" with title "Restaurant Analyzer" buttons {"確定"} default button "確定"'
    exit 1
fi

# 檢查 Java 版本
JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    osascript -e "display dialog \"檢測到 Java $JAVA_VERSION，建議使用 Java 21 或更高版本\" with title \"Restaurant Analyzer\" buttons {\"確定\"} default button \"確定\""
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

# 切換到 Java 目錄並啟動應用程式
cd "$APP_DIR"
exec ./gradlew run --no-daemon
EOF

# 讓可執行檔案有執行權限
chmod +x "$APP_BUNDLE/Contents/MacOS/RestaurantAnalyzer"

# 創建啟動腳本 (Mac/Linux)
echo "🖥️  創建 Mac/Linux 啟動腳本..."
cat > "$PACKAGE_DIR/RestaurantAnalyzer.sh" << 'EOF'
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

# macOS 特定設定
if [[ "$OSTYPE" == "darwin"* ]]; then
    JAVA_OPTS="$JAVA_OPTS -Dapple.awt.application.name=Restaurant\ Analyzer"
    JAVA_OPTS="$JAVA_OPTS -Xdock:name=Restaurant\ Analyzer"
fi

# 啟動應用程式
echo "▶️  啟動餐廳分析器..."
cd "$APP_DIR"
./gradlew run --no-daemon

echo "👋 餐廳分析器已關閉"
EOF

chmod +x "$PACKAGE_DIR/RestaurantAnalyzer.sh"

# 創建 Windows 啟動腳本
echo "🪟 創建 Windows 啟動腳本..."
cat > "$PACKAGE_DIR/RestaurantAnalyzer.bat" << 'EOF'
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
EOF

# 創建安裝說明
echo "📖 創建安裝說明..."
cat > "$PACKAGE_DIR/README.txt" << 'EOF'
餐廳分析器 Restaurant Analyzer v1.0.0
=====================================

🎯 系統需求 System Requirements:
- Java 21 或更高版本 (Java 21 or higher)
- Python 3.8+ (可選，用於數據收集功能 Optional, for data collection features)
- 至少 2GB RAM (At least 2GB RAM)
- 300MB 磁碟空間 (300MB disk space)

🚀 快速開始 Quick Start:

Windows 用戶:
1. 雙擊 RestaurantAnalyzer.bat
2. 首次運行會自動設置 Python 環境（如果已安裝 Python）

Mac/Linux 用戶:
1. 打開終端機，進入應用程式目錄
2. 執行: ./RestaurantAnalyzer.sh
3. 首次運行會自動設置 Python 環境（如果已安裝 Python）

📦 目錄結構 Directory Structure:
├── lib/                    # Java JAR 文件
├── data-collector/         # Python 數據收集器
├── scripts/               # 工具腳本
├── docs/                  # 文檔
├── RestaurantAnalyzer.sh  # Mac/Linux 啟動腳本
├── RestaurantAnalyzer.bat # Windows 啟動腳本
└── requirements.txt       # Python 依賴

🔧 故障排除 Troubleshooting:

1. Java 相關問題:
   - 確保安裝了 Java 21+
   - 下載地址: https://www.oracle.com/java/technologies/downloads/

2. Python 相關問題:
   - 確保安裝了 Python 3.8+
   - 下載地址: https://www.python.org/downloads/

3. 權限問題 (Mac/Linux):
   - 執行: chmod +x RestaurantAnalyzer.sh

4. 編碼問題:
   - 確保系統支援 UTF-8 編碼

📞 技術支援 Technical Support:
如有問題，請查看 docs/ 目錄中的詳細文檔。
For issues, please check detailed documentation in docs/ directory.

EOF

# 創建解除安裝腳本
echo "🗑️  創建解除安裝腳本..."
cat > "$PACKAGE_DIR/uninstall.sh" << 'EOF'
#!/bin/bash
echo "🗑️  解除安裝餐廳分析器..."
read -p "確定要刪除所有文件嗎？(y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    cd ..
    rm -rf "$(basename "$PWD")"
    echo "✅ 解除安裝完成"
else
    echo "❌ 解除安裝已取消"
fi
EOF

cat > "$PACKAGE_DIR/uninstall.bat" << 'EOF'
@echo off
echo 🗑️ 解除安裝餐廳分析器...
set /p "confirm=確定要刪除所有文件嗎？(y/N): "
if /i "%confirm%"=="y" (
    cd ..
    rmdir /s /q "%~dp0"
    echo ✅ 解除安裝完成
    pause
) else (
    echo ❌ 解除安裝已取消
    pause
)
EOF

chmod +x "$PACKAGE_DIR/uninstall.sh"

# 創建壓縮包
echo "📦 創建發布包..."
cd $DIST_DIR

# 創建 ZIP 包 (跨平台)
zip -r "RestaurantAnalyzer-$VERSION-crossplatform.zip" "RestaurantAnalyzer-$VERSION"

# 創建 Mac DMG 包
echo "💿 創建 Mac DMG 包..."
if command -v hdiutil &> /dev/null; then
    # 創建臨時DMG目錄
    DMG_TEMP_DIR="$DIST_DIR/dmg_temp"
    mkdir -p "$DMG_TEMP_DIR"
    
    # 複製 .app bundle 到 DMG 目錄
    if [ -d "$APP_BUNDLE" ]; then
        cp -r "$APP_BUNDLE" "$DMG_TEMP_DIR/"
        echo "✅ 已複製 .app bundle 到 DMG"
    else
        echo "❌ 錯誤: 找不到 .app bundle: $APP_BUNDLE"
        exit 1
    fi
    
    # 創建 Applications 目錄的符號連結
    ln -s /Applications "$DMG_TEMP_DIR/Applications"
    
    # 創建背景和樣式（可選）
    mkdir -p "$DMG_TEMP_DIR/.background"
    if [ -f "應用程式背景.png" ]; then
        cp "應用程式背景.png" "$DMG_TEMP_DIR/.background/"
    fi
    
    # 創建 README
    cat > "$DMG_TEMP_DIR/README.txt" << EOF
Restaurant Analyzer - 餐廳分析器

安裝方式：
1. 將 "Restaurant Analyzer.app" 拖拽到 "Applications" 目錄
2. 在 Launchpad 或 Applications 目錄中找到並啟動應用程式

系統需求：
- macOS 10.15 或更高版本
- Java 21 或更高版本
- Python 3 (可選，用於數據收集功能)

如需幫助，請查看應用程式內的文檔。
EOF
    
    # 創建DMG
    hdiutil create -volname "Restaurant Analyzer" \
                   -srcfolder "$DMG_TEMP_DIR" \
                   -ov -format UDZO \
                   "$DIST_DIR/RestaurantAnalyzer-$VERSION-mac.dmg"
    
    # 清理臨時目錄
    rm -rf "$DMG_TEMP_DIR"
    
    echo "✅ Mac DMG 創建完成: $DIST_DIR/RestaurantAnalyzer-$VERSION-mac.dmg"
else
    echo "⚠️  hdiutil 不可用，跳過 DMG 包創建"
fi

cd ..

echo "✅ 打包完成！"
echo "📦 發布包位置:"
echo "   🌐 跨平台: dist/RestaurantAnalyzer-$VERSION-crossplatform.zip"
echo "   🍎 Mac DMG: dist/RestaurantAnalyzer-$VERSION-mac.dmg"
echo "   📁 原始目錄: dist/RestaurantAnalyzer-$VERSION"
echo ""
echo "🎉 打包成功！"
echo "📋 Mac 用戶使用方式："
echo "   1. 雙擊打開 DMG 檔案"
echo "   2. 將 'Restaurant Analyzer.app' 拖拽到 Applications 目錄"
echo "   3. 在 Launchpad 中啟動應用程式"
echo ""
echo "📋 其他平台用戶："
echo "   1. 解壓縮跨平台 ZIP 檔案"
echo "   2. 運行對應的啟動腳本" 