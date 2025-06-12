#!/bin/bash

# Restaurant Analyzer - macOS DMG 打包腳本 (全自動版)
# 創建完全自動化的 macOS 應用程式包，具備 Ollama 自動檢測、安裝與啟動功能

# 嚴格模式：任何錯誤立即終止腳本
set -e

# 應用程式版本和名稱設置
VERSION="1.1.5"
APP_NAME="Restaurant Analyzer"
DMG_NAME="RestaurantAnalyzer-Working"
BUILD_DIR="build/macos"
APP_DIR="$BUILD_DIR/$APP_NAME.app"
DMG_DIR="build/dmg"
RESOURCES_DIR="$APP_DIR/Contents/Resources"
JAVA_DIR="$APP_DIR/Contents/Java"

# 顯示彩色標題和版本信息
echo "╭────────────────────────────────────────────────╮"
echo "│ 🍎 餐廳分析器 - macOS 應用程式打包腳本 (全自動版) │"
echo "│ 📦 版本: $VERSION                              │" 
echo "╰────────────────────────────────────────────────╯"

# 1. 準備階段：清理舊的建置檔案
echo ""
echo "🧹 清理舊的建置檔案..."

if [ -d "$BUILD_DIR" ]; then
    echo "   - 移除舊的建置目錄 ($BUILD_DIR)"
    rm -rf "$BUILD_DIR"
fi

if [ -d "$DMG_DIR" ]; then
    echo "   - 移除舊的 DMG 目錄 ($DMG_DIR)"
    rm -rf "$DMG_DIR"
fi

# 2. 創建應用程式目錄結構
echo ""
echo "📁 創建應用程式包結構..."
mkdir -p "$APP_DIR/Contents/MacOS"
mkdir -p "$RESOURCES_DIR"
mkdir -p "$JAVA_DIR"
mkdir -p "$DMG_DIR"
mkdir -p "dist"

# 3. 複製應用程式檔案
echo ""
echo "📋 複製應用程式檔案..."

# 主要 JAR 檔案
if [ -f "build/libs/bigproject.jar" ]; then
    echo "   - 複製主要 JAR 檔案"
    cp "build/libs/bigproject.jar" "$JAVA_DIR/Restaurant-Analyzer.jar"
else
    echo "❌ 錯誤：主要 JAR 檔案不存在！請先執行 './gradlew build'"
    exit 1
fi

# 源代碼和開發資源
echo "   - 複製源代碼和開發資源"
cp -r src "$JAVA_DIR/"
cp -r data-collector "$JAVA_DIR/"
cp build.gradle "$JAVA_DIR/"
cp settings.gradle "$JAVA_DIR/"
cp gradlew "$JAVA_DIR/"
cp gradlew.bat "$JAVA_DIR/"
cp -r gradle "$JAVA_DIR/"
cp requirements.txt "$JAVA_DIR/"

# 背景圖片和其他資源
if [ -f "應用程式背景.png" ]; then
    echo "   - 複製應用程式背景圖片"
    cp "應用程式背景.png" "$JAVA_DIR/"
fi

# 複製文件和說明
echo "   - 複製文檔和說明文件"
cp README.md "$JAVA_DIR/" 2>/dev/null || echo "   - (跳過不存在的 README.md)"
cp QUICK_START.md "$JAVA_DIR/" 2>/dev/null || echo "   - (跳過不存在的 QUICK_START.md)"
cp 程式架構說明.md "$JAVA_DIR/" 2>/dev/null || echo "   - (跳過不存在的 程式架構說明.md)"

# 創建資料目錄
mkdir -p "$JAVA_DIR/chat_history"

# 4. 創建 Info.plist
echo ""
echo "📝 創建 Info.plist..."
cat > "$APP_DIR/Contents/Info.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>zh-TW</string>
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
    <!-- 添加完整網路權限，確保 Ollama 通信不受限制 -->
    <key>com.apple.security.network.client</key>
    <true/>
    <key>com.apple.security.network.server</key>
    <true/>
    <key>NSLocalNetworkUsageDescription</key>
    <string>餐廳分析器需要使用本地網路連接到 AI 服務</string>
</dict>
</plist>
EOF

# 5. 創建自動化啟動腳本
echo ""
echo "🚀 創建全自動啟動腳本..."
cat > "$APP_DIR/Contents/MacOS/RestaurantAnalyzer" << 'EOF'
#!/bin/bash

# 餐廳分析器全自動啟動腳本
# 自動檢測環境、安裝並啟動 Ollama，無需使用者手動操作

# 獲取應用程式目錄路徑
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../Java" && pwd)"
LOGS_DIR="$HOME/Library/Logs/RestaurantAnalyzer"
# 建立日誌目錄
mkdir -p "$LOGS_DIR"
LOG_FILE="$LOGS_DIR/app_$(date +%Y%m%d_%H%M%S).log"

# 同時輸出到控制台和日誌檔案
exec > >(tee -a "$LOG_FILE") 2>&1

echo ""
echo "╭───────────────────────────────────────────╮"
echo "│ 🍽️  餐廳分析器 - 自動啟動程序            │"
echo "╰───────────────────────────────────────────╯"
echo ""
echo "📅 啟動時間: $(date)"
echo "📍 應用程式目錄: $APP_DIR"
echo "📝 日誌檔案: $LOG_FILE"
echo ""

# 檢查 Java 安裝
echo "🔍 檢查 Java 環境..."
if ! command -v java &> /dev/null; then
    echo "❌ 未找到 Java！"
    osascript -e 'display dialog "未找到 Java\n\n請安裝 Java 21 或更高版本\n下載地址: https://www.oracle.com/java/technologies/downloads/\n\n安裝完成後請重新啟動餐廳分析器" with title "餐廳分析器" buttons {"確定"} default button "確定" with icon stop'
    exit 1
fi

# 檢查 Java 版本
JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
echo "✅ 檢測到 Java 版本: $JAVA_VERSION"

if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "⚠️ Java 版本過低 (建議 Java 21 或更高)"
    osascript -e "display dialog \"檢測到 Java $JAVA_VERSION，建議使用 Java 21 或更高版本\n\n應用程式仍會嘗試啟動，但可能會遇到問題。\" with title \"餐廳分析器\" buttons {\"繼續嘗試啟動\", \"取消\"} default button \"繼續嘗試啟動\" with icon caution" || exit 1
fi

# 檢查 Python 環境
echo ""
echo "🔍 檢查 Python 環境..."
if command -v python3 &> /dev/null; then
    PYTHON_VERSION=$(python3 --version 2>&1 | cut -d' ' -f2)
    echo "✅ 檢測到 Python: $PYTHON_VERSION"
    # 檢查是否有虛擬環境
    if [ ! -d "$APP_DIR/.venv" ]; then
        echo "🐍 創建 Python 虛擬環境..."
        python3 -m venv "$APP_DIR/.venv"
        source "$APP_DIR/.venv/bin/activate"
        if [ -f "$APP_DIR/requirements.txt" ]; then
            echo "📦 安裝 Python 依賴..."
            pip install --quiet -r "$APP_DIR/requirements.txt"
        fi
    else
        echo "✅ 使用現有 Python 虛擬環境"
        source "$APP_DIR/.venv/bin/activate"
    fi
else
    echo "⚠️ 未檢測到 Python 3，數據收集功能將受限"
fi

# 自動檢測和安裝 Ollama
echo ""
echo "🔍 自動檢測和管理 Ollama AI 服務..."
OLLAMA_INSTALL_PATH="$HOME/.ollama/bin/ollama"
OLLAMA_INSTALLED=false
OLLAMA_RUNNING=false
OLLAMA_MODEL_READY=false

# 檢查 Ollama 是否已安裝
if [ -f "$OLLAMA_INSTALL_PATH" ]; then
    echo "✅ 檢測到 Ollama 已安裝: $OLLAMA_INSTALL_PATH"
    OLLAMA_INSTALLED=true
else
    # 自動下載安裝 Ollama
    echo "📥 Ollama 未安裝，正在自動下載安裝..."
    
    # 顯示進度對話框
    osascript -e 'display dialog "正在下載並安裝 AI 服務組件...\n\n這只需要進行一次，請稍候..." with title "餐廳分析器" buttons {"請稍候..."} default button "請稍候..." giving up after 3 with icon note' &
    DIALOG_PID=$!
    
    TEMP_DIR=$(mktemp -d)
    echo "   - 下載 Ollama 中..."
    curl -L -s -o "$TEMP_DIR/ollama.tgz" "https://github.com/ollama/ollama/releases/latest/download/ollama-darwin.tgz"
    
    echo "   - 解壓 Ollama 中..."
    mkdir -p "$HOME/.ollama/bin"
    tar -xzf "$TEMP_DIR/ollama.tgz" -C "$TEMP_DIR"
    mv "$TEMP_DIR/ollama" "$HOME/.ollama/bin/"
    chmod +x "$HOME/.ollama/bin/ollama"
    
    # 清理臨時目錄
    rm -rf "$TEMP_DIR"
    
    if [ -f "$OLLAMA_INSTALL_PATH" ]; then
        echo "✅ Ollama 安裝成功"
        OLLAMA_INSTALLED=true
        
        # 關閉進度對話框
        kill $DIALOG_PID 2>/dev/null || true
    else
        echo "❌ Ollama 安裝失敗"
        
        # 關閉進度對話框
        kill $DIALOG_PID 2>/dev/null || true
        
        osascript -e 'display dialog "無法安裝 AI 服務組件\n\n應用程式將繼續啟動，但 AI 功能可能無法使用。" with title "餐廳分析器" buttons {"確定"} default button "確定" with icon caution'
    fi
fi

# 如果 Ollama 已安裝，確保它在運行
if [ "$OLLAMA_INSTALLED" = true ]; then
    # 檢查 Ollama 服務是否已在運行
    if pgrep -x "ollama" > /dev/null; then
        echo "✅ Ollama 服務已在運行中"
        OLLAMA_RUNNING=true
    else
        # 自動啟動 Ollama 服務
        echo "🚀 自動啟動 Ollama 服務..."
        
        # 顯示啟動中對話框
        osascript -e 'display dialog "正在啟動 AI 服務...\n\n首次啟動可能需要幾秒鐘，請稍候..." with title "餐廳分析器" buttons {"請稍候..."} default button "請稍候..." giving up after 3 with icon note' &
        DIALOG_PID=$!
        
        # 啟動 Ollama 服務
        "$OLLAMA_INSTALL_PATH" serve > /dev/null 2>&1 &
        OLLAMA_SERVER_PID=$!
        
        # 確保 Ollama 進程在應用程式退出時終止
        trap 'pkill -P $OLLAMA_SERVER_PID 2>/dev/null || true; kill $OLLAMA_SERVER_PID 2>/dev/null || true' EXIT
        
        # 等待 Ollama 服務啟動
        echo "⏳ 等待 Ollama 服務啟動..."
        for i in {1..30}; do
            if curl -s -I "http://localhost:11434/api/version" &> /dev/null; then
                echo "✅ Ollama 服務啟動成功"
                OLLAMA_RUNNING=true
                
                # 關閉啟動中對話框
                kill $DIALOG_PID 2>/dev/null || true
                break
            fi
            echo -n "."
            sleep 1
        done
        
        # 如果無法啟動 Ollama，顯示提示
        if [ "$OLLAMA_RUNNING" = false ]; then
            # 關閉啟動中對話框
            kill $DIALOG_PID 2>/dev/null || true
            
            echo "❌ 無法啟動 Ollama 服務"
            osascript -e 'display dialog "無法啟動 AI 服務\n\n應用程式將繼續啟動，但 AI 功能可能無法使用。" with title "餐廳分析器" buttons {"確定"} default button "確定" with icon caution'
        fi
    fi

    # 如果 Ollama 服務運行中，檢查並下載模型
    if [ "$OLLAMA_RUNNING" = true ]; then
        echo "🔍 檢查 AI 模型是否已下載..."
        
        if "$OLLAMA_INSTALL_PATH" list | grep -q "gemma3:4b"; then
            echo "✅ AI 模型 (gemma3:4b) 已就緒"
            OLLAMA_MODEL_READY=true
        else
            # 自動下載模型
            echo "📥 下載 AI 模型 (gemma3:4b)..."
            
            # 顯示下載中對話框
            osascript -e 'display dialog "正在下載 AI 模型 (gemma3:4b, 約 2.7GB)...\n\n這只需要進行一次，請耐心等候...\n\n下載過程中應用程式會保持運行" with title "餐廳分析器" buttons {"請稍候..."} default button "請稍候..." giving up after 5 with icon note' &
            DIALOG_PID=$!
            
            # 後台下載模型
            "$OLLAMA_INSTALL_PATH" pull gemma3:4b > /dev/null 2>&1 &
            DOWNLOAD_PID=$!
            
            # 檢查下載進度
            echo "⏳ 模型下載已在後台啟動，應用程式將繼續啟動"
            echo "📊 首次運行時 AI 功能可能需要等待模型下載完成才能使用"
            
            # 關閉下載中對話框
            sleep 5
            kill $DIALOG_PID 2>/dev/null || true
            
            # 設置 OLLAMA_MODEL_READY 為 false，因為模型正在下載中
            OLLAMA_MODEL_READY=false
        fi
    fi
fi

# 配置 Java 啟動參數
echo ""
echo "⚙️ 配置 Java 參數..."
JAVA_OPTS="-Xmx2g -Xms512m"
JAVA_OPTS="$JAVA_OPTS --add-modules java.net.http,java.prefs,javafx.controls,javafx.fxml,javafx.swing,javafx.graphics,javafx.base"
JAVA_OPTS="$JAVA_OPTS --add-modules ALL-MODULE-PATH"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-exports javafx.swing/javafx.embed.swing=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS -Djava.library.path=$APP_DIR"
# 傳遞 Ollama 狀態給應用程式
JAVA_OPTS="$JAVA_OPTS -Dollama.installed=$OLLAMA_INSTALLED"
JAVA_OPTS="$JAVA_OPTS -Dollama.running=$OLLAMA_RUNNING"
JAVA_OPTS="$JAVA_OPTS -Dollama.model.ready=$OLLAMA_MODEL_READY"
JAVA_OPTS="$JAVA_OPTS -Dollama.path=$OLLAMA_INSTALL_PATH"

# 切換到應用程式目錄
cd "$APP_DIR"

# 啟動應用程式
echo ""
echo "🚀 正在啟動餐廳分析器..."
echo "⏳ 請稍候..."
echo ""

# 嘗試直接運行 JAR，如果失敗則使用 Gradle
java $JAVA_OPTS -jar "Restaurant-Analyzer.jar" 2>/dev/null || {
    echo "🔄 嘗試使用 Gradle 啟動..."
    ./gradlew run --no-daemon
}
EOF

# 讓啟動腳本可執行
chmod +x "$APP_DIR/Contents/MacOS/RestaurantAnalyzer"

# 6. 創建應用程式圖示
echo ""
echo "🎨 創建應用程式圖示..."
touch "$APP_DIR/Contents/Resources/icon.icns"

# 7. 創建 README 和啟動說明
echo ""
echo "📝 創建啟動說明..."
cat > "$DMG_DIR/README.txt" << 'EOF'
=== 餐廳分析器 - 安裝與啟動說明 ===

【安裝說明】
1. 將 "Restaurant Analyzer" 拖曳至 "Applications" 資料夾
2. 從 Launchpad 或 Applications 資料夾啟動應用程式

【特色功能】
- 完全自動化的 AI 服務：無需手動啟動 Ollama
- 首次執行會自動下載並安裝所需的 AI 組件
- 自動分析餐廳評論、排名與特色
- 智能對話功能，協助餐廳營運決策

【首次使用注意事項】
- 首次啟動時，系統可能會顯示"未識別的開發者"警告
  解決方法：在 Finder 中右鍵點擊應用程式，選擇"開啟"
- 首次執行時，AI 模型下載可能需要幾分鐘（約 2.7GB）
  應用程式會在背景完成下載並自動啟用 AI 功能

【系統需求】
- macOS 10.14 或更新版本
- Java 21 或更高版本
- 4GB 以上可用記憶體
- 3GB 以上硬碟空間（用於 AI 模型）

【如遇問題】
如果應用程式無法正常啟動或 AI 功能無回應，請查閱日誌檔案：
~/Library/Logs/RestaurantAnalyzer/app_*.log

祝您使用愉快！
EOF

# 8. 創建 DMG 檔案
echo ""
echo "💿 創建 DMG 檔案..."

# 複製應用程式到 DMG 目錄
cp -r "$APP_DIR" "$DMG_DIR/"

# 在 DMG 目錄中創建 Applications 連結
ln -sf /Applications "$DMG_DIR/Applications"

# 創建 DMG 檔案
DMG_PATH="dist/$DMG_NAME.dmg"
if [ -f "$DMG_PATH" ]; then
    echo "   - 移除舊的 DMG 檔案"
    rm "$DMG_PATH"
fi

echo "   - 創建新的 DMG 檔案"
hdiutil create -volname "$APP_NAME" -srcfolder "$DMG_DIR" -ov -format UDZO "$DMG_PATH"

# 計算檔案大小
DMG_SIZE=$(du -m "$DMG_PATH" | cut -f1)

# 9. 結果報告
echo ""
echo "╭────────────────────────────────────────────────╮"
echo "│ ✅ macOS DMG 安裝包創建完成！                 │"
echo "╰────────────────────────────────────────────────╯"
echo ""
echo "📦 輸出檔案:"
echo "   📄 $DMG_PATH ($DMG_SIZE MB)"
echo ""
echo "🚀 安裝說明:"
echo "   1. 雙擊開啟 DMG 檔案"
echo "   2. 拖拽 '$APP_NAME.app' 到 Applications 資料夾"
echo "   3. 從 Launchpad 或 Applications 啟動應用程式"
echo ""
echo "✨ 主要特色:"
echo "   • 全自動 Ollama 檢測、安裝與啟動功能"
echo "   • 自動下載 AI 模型 (gemma3:4b)"
echo "   • 智能錯誤處理和詳細日誌記錄"
echo "   • 提升使用者首次體驗的互動提示"
echo ""
echo "⚠️  注意事項:"
echo "   • 需要 Java 21+ 才能運行"
echo "   • 首次使用時可能需要在系統偏好設定中允許應用程式運行"
echo "   • 首次執行時 AI 模型下載可能需要幾分鐘"
echo "" 