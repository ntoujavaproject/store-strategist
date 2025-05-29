#!/bin/bash

echo "🎯 創建真正適合一般使用者的DMG（基於可運行版本）..."

# 設定變數
APP_NAME="Restaurant Analyzer"
APP_BUNDLE="dist/RestaurantAnalyzer-1.0.0/Restaurant Analyzer.app"
DMG_NAME="RestaurantAnalyzer-Working"
TEMP_DIR="dist/dmg_working"
BACKGROUND_IMG="應用程式背景.png"

# 檢查應用程式是否存在
echo "🔍 檢查應用程式檔案..."
if [ ! -d "$APP_BUNDLE" ]; then
    echo "❌ 錯誤: 找不到應用程式: $APP_BUNDLE"
    exit 1
fi
echo "✅ 應用程式檔案確認存在"

# 清理舊檔案
echo "🧹 清理舊檔案..."
# 確保沒有掛載的 DMG
hdiutil detach "/Volumes/$APP_NAME" 2>/dev/null || true
sleep 2

rm -rf "$TEMP_DIR"
rm -f "dist/${DMG_NAME}.dmg"
rm -f "dist/${DMG_NAME}-rw.dmg"

# 創建目錄結構
echo "📁 創建目錄結構..."
mkdir -p "$TEMP_DIR"

# 複製應用程式
echo "📦 複製應用程式..."
cp -R "$APP_BUNDLE" "$TEMP_DIR/"

# 創建Applications連結
echo "🔗 創建 Applications 連結..."
ln -s /Applications "$TEMP_DIR/Applications"

# 複製背景圖片（如果存在）
if [ -f "$BACKGROUND_IMG" ]; then
    echo "🖼️  複製背景圖片..."
    cp "$BACKGROUND_IMG" "$TEMP_DIR/.background.png"
else
    echo "⚠️  背景圖片不存在，將使用預設背景"
fi

# 驗證暫存目錄內容
echo "🔍 驗證暫存目錄內容："
ls -la "$TEMP_DIR/"
echo ""

# 創建較大尺寸的可讀寫DMG
echo "💿 創建可讀寫 DMG..."
hdiutil create -volname "$APP_NAME" \
               -srcfolder "$TEMP_DIR" \
               -ov -format UDRW \
               -size 120m \
               "dist/${DMG_NAME}-rw.dmg"

if [ $? -ne 0 ]; then
    echo "❌ DMG 創建失敗"
    exit 1
fi

# 掛載DMG
echo "🔗 掛載 DMG..."
hdiutil attach "dist/${DMG_NAME}-rw.dmg"

# 等待掛載完成
sleep 5

# 驗證 DMG 內容
echo "🔍 驗證 DMG 內容："
ls -la "/Volumes/$APP_NAME/"
echo ""

# 確保兩個重要檔案都存在
if [ ! -d "/Volumes/$APP_NAME/Restaurant Analyzer.app" ]; then
    echo "❌ Restaurant Analyzer.app 不在 DMG 中！"
    exit 1
fi

if [ ! -L "/Volumes/$APP_NAME/Applications" ]; then
    echo "❌ Applications 連結不在 DMG 中！"
    exit 1
fi

echo "✅ 兩個檔案都確認存在於 DMG 中"

# 設定DMG視圖和背景
echo "🎨 設定 DMG 視圖和背景..."
osascript << EOF
tell application "Finder"
    tell disk "$APP_NAME"
        open
        delay 2
        
        -- 設定基本視圖
        set current view of container window to icon view
        set toolbar visible of container window to false
        set statusbar visible of container window to false
        
        -- 設定視窗大小和位置
        set the bounds of container window to {100, 100, 650, 450}
        
        -- 設定圖示視圖選項
        set theViewOptions to the icon view options of container window
        set arrangement of theViewOptions to not arranged
        set icon size of theViewOptions to 96
        set shows item info of theViewOptions to false
        set shows icon preview of theViewOptions to true
        
        -- 設定背景圖片（如果存在）
        try
            set background picture of theViewOptions to file ".background.png"
        on error
            -- 如果背景設定失敗，繼續執行
        end try
        
        -- 等待Finder準備好
        delay 5
        
        -- 設定項目位置（多次嘗試）
        repeat 3 times
            try
                set position of item "Restaurant Analyzer.app" of container window to {170, 220}
                set position of item "Applications" of container window to {450, 220}
                exit repeat
            on error errMsg
                delay 2
            end try
        end repeat
        
        -- 強制更新
        update without registering applications
        delay 3
        
        -- 再次更新確保設定生效
        update without registering applications
        delay 2
        
        close
    end tell
end tell
EOF

echo "⏳ 等待視圖設定完成..."
sleep 8

# 強制同步
sync
sleep 2

# 卸載DMG
echo "📤 卸載 DMG..."
hdiutil detach "/Volumes/$APP_NAME"

# 等待卸載完成
sleep 3

# 轉換為只讀壓縮DMG
echo "🗜️  轉換為最終 DMG..."
hdiutil convert "dist/${DMG_NAME}-rw.dmg" \
               -format UDZO \
               -imagekey zlib-level=9 \
               -o "dist/${DMG_NAME}.dmg"

if [ $? -eq 0 ]; then
    # 清理
    rm -f "dist/${DMG_NAME}-rw.dmg"
    rm -rf "$TEMP_DIR"
    
    echo "✅ 使用者友好的DMG已創建：dist/${DMG_NAME}.dmg"
    echo "📦 這個DMG應該能正確顯示兩個檔案！"
    echo ""
    echo "📊 檔案大小："
    ls -lah "dist/${DMG_NAME}.dmg"
    echo ""
    
    # 立即測試
    echo "🧪 立即測試新創建的 DMG..."
    hdiutil attach "dist/${DMG_NAME}.dmg" -readonly
    
    sleep 3
    
    if [ -d "/Volumes/$APP_NAME" ]; then
        echo "📋 最終 DMG 內容："
        ls -la "/Volumes/$APP_NAME/"
        
        if [ -d "/Volumes/$APP_NAME/Restaurant Analyzer.app" ] && [ -L "/Volumes/$APP_NAME/Applications" ]; then
            echo "✅ 成功！兩個檔案都在最終 DMG 中"
            open "/Volumes/$APP_NAME/"
            echo "🔍 Finder 視窗已開啟，請檢查兩個項目是否都顯示"
        else
            echo "❌ 最終驗證失敗"
        fi
    fi
else
    echo "❌ DMG 轉換失敗"
    exit 1
fi 