#!/bin/bash

# Restaurant Analyzer - macOS DMG 打包腳本
# 使用已建置的 JAR 檔案創建 macOS 應用程式包

set -e

VERSION="1.0.0"
APP_NAME="Restaurant Analyzer"
DMG_NAME="RestaurantAnalyzer-Working"
BUILD_DIR="build/macos"
APP_DIR="$BUILD_DIR/$APP_NAME.app"
DMG_DIR="build/dmg"

echo "🍎 開始創建 macOS DMG 安裝包..."
echo "📦 版本: $VERSION"

# 清理舊的建置檔案
if [ -d "$BUILD_DIR" ]; then
    rm -rf "$BUILD_DIR"
fi
if [ -d "$DMG_DIR" ]; then
    rm -rf "$DMG_DIR"
fi

# 創建目錄結構
mkdir -p "$APP_DIR/Contents/MacOS"
mkdir -p "$APP_DIR/Contents/Resources"
mkdir -p "$APP_DIR/Contents/Java"
mkdir -p "$DMG_DIR"

echo "📁 創建應用程式包結構..."

# 複製 JAR 檔案和相關檔案到應用程式包
echo "📋 複製應用程式檔案..."
cp "build/libs/Restaurant Analyzer-standalone-$VERSION-all.jar" "$APP_DIR/Contents/Java/Restaurant-Analyzer.jar"

# 複製源代碼和資源（供開發使用）
cp -r src "$APP_DIR/Contents/Java/"
cp -r data-collector "$APP_DIR/Contents/Java/"
cp build.gradle "$APP_DIR/Contents/Java/"
cp settings.gradle "$APP_DIR/Contents/Java/"
cp gradlew "$APP_DIR/Contents/Java/"
cp gradlew.bat "$APP_DIR/Contents/Java/"
cp -r gradle "$APP_DIR/Contents/Java/"
cp requirements.txt "$APP_DIR/Contents/Java/"
if [ -f "應用程式背景.png" ]; then
    cp "應用程式背景.png" "$APP_DIR/Contents/Java/"
fi

# 創建 Info.plist
echo "📝 創建 Info.plist..."
cat > "$APP_DIR/Contents/Info.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>en</string>
    <key>CFBundleDisplayName</key>
    <string>$APP_NAME</string>
    <key>CFBundleExecutable</key>
    <string>RestaurantAnalyzer</string>
    <key>CFBundleIconFile</key>
    <string>icon.icns</string>
    <key>CFBundleIdentifier</key>
    <string>com.restaurantanalyzer.app</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>$APP_NAME</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>$VERSION</string>
    <key>CFBundleVersion</key>
    <string>$VERSION</string>
    <key>LSApplicationCategoryType</key>
    <string>public.app-category.productivity</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.14</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSHumanReadableCopyright</key>
    <string>Copyright © 2024 Restaurant Analyzer Team</string>
    <key>NSPrincipalClass</key>
    <string>NSApplication</string>
    <key>NSRequiresAquaSystemAppearance</key>
    <false/>
</dict>
</plist>
EOF

# 創建啟動腳本
echo "🚀 創建啟動腳本..."
cat > "$APP_DIR/Contents/MacOS/RestaurantAnalyzer" << 'EOF'
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
    osascript -e "display dialog \"檢測到 Java $JAVA_VERSION，建議使用 Java 21 或更高版本\n\n應用程式仍會嘗試啟動，但可能會遇到問題。\" with title \"Restaurant Analyzer\" buttons {\"確定\"} default button \"確定\""
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

# 設定 Java 參數
JAVA_OPTS="-Xmx2g -Xms512m"
JAVA_OPTS="$JAVA_OPTS --add-modules java.net.http,java.prefs,javafx.controls,javafx.fxml,javafx.swing,javafx.graphics,javafx.base"
JAVA_OPTS="$JAVA_OPTS --add-modules ALL-MODULE-PATH"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-exports javafx.swing/javafx.embed.swing=ALL-UNNAMED"

# 切換到應用程式目錄並啟動
cd "$APP_DIR"

# 嘗試直接運行 JAR，如果失敗則使用 Gradle
echo "▶️ 啟動餐廳分析器..."
java $JAVA_OPTS -jar "Restaurant-Analyzer.jar" 2>/dev/null || {
    echo "🔄 JAR 檔案啟動失敗，使用 Gradle 啟動..."
    ./gradlew run --no-daemon
}
EOF

# 讓啟動腳本可執行
chmod +x "$APP_DIR/Contents/MacOS/RestaurantAnalyzer"

# 創建一個簡單的圖示檔案（空檔案，使用系統預設圖示）
touch "$APP_DIR/Contents/Resources/icon.icns"

echo "📦 創建 DMG 檔案..."

# 複製應用程式到 DMG 目錄
cp -r "$APP_DIR" "$DMG_DIR/"

# 在 DMG 目錄中創建 Applications 連結
ln -sf /Applications "$DMG_DIR/Applications"

# 創建 DMG 檔案
DMG_PATH="dist/$DMG_NAME.dmg"
if [ -f "$DMG_PATH" ]; then
    rm "$DMG_PATH"
fi

hdiutil create -volname "$APP_NAME" -srcfolder "$DMG_DIR" -ov -format UDZO "$DMG_PATH"

# 計算檔案大小
DMG_SIZE=$(du -m "$DMG_PATH" | cut -f1)

echo ""
echo "✅ macOS DMG 安裝包創建完成！"
echo ""
echo "📦 檔案:"
echo "   📄 $DMG_PATH"
echo ""
echo "📊 大小:"
echo "   📦 $DMG_SIZE MB"
echo ""
echo "🚀 使用方法:"
echo "   1. 雙擊開啟 DMG 檔案"
echo "   2. 拖拽 '$APP_NAME.app' 到 Applications 資料夾"
echo "   3. 從 Launchpad 或 Applications 啟動應用程式"
echo ""
echo "✨ 特色: 包含完整的應用程式和所有依賴，包括源代碼用於開發"
echo "⚠️  注意: 需要安裝 Java 21+ 才能運行"
echo "" 