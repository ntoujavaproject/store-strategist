#!/bin/bash

echo "=== Restaurant Analyzer 應用程式打包工具 ==="
echo "此腳本將自動轉換圖標並創建應用程式包"

# 檢查操作系統
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "檢測到 macOS 系統"
    IS_MAC=true
else
    echo "檢測到非 macOS 系統"
    IS_MAC=false
fi

# 設置路徑
ICONS_DIR="src/main/resources/icons"
PNG_ICON="$ICONS_DIR/restaurant_icon.png"
ICNS_ICON="$ICONS_DIR/restaurant_icon.icns"

# 檢查PNG圖標是否存在
if [ ! -f "$PNG_ICON" ]; then
    echo "錯誤: 找不到PNG圖標文件 $PNG_ICON"
    exit 1
fi

# 在macOS上將PNG轉換為ICNS
if [ "$IS_MAC" = true ]; then
    echo "正在將PNG圖標轉換為ICNS格式..."
    
    # 創建臨時圖標集
    mkdir -p MyIcon.iconset
    
    # 生成各種尺寸的圖標
    sips -z 16 16 $PNG_ICON --out MyIcon.iconset/icon_16x16.png
    sips -z 32 32 $PNG_ICON --out MyIcon.iconset/icon_16x16@2x.png
    sips -z 32 32 $PNG_ICON --out MyIcon.iconset/icon_32x32.png
    sips -z 64 64 $PNG_ICON --out MyIcon.iconset/icon_32x32@2x.png
    sips -z 128 128 $PNG_ICON --out MyIcon.iconset/icon_128x128.png
    sips -z 256 256 $PNG_ICON --out MyIcon.iconset/icon_128x128@2x.png
    sips -z 256 256 $PNG_ICON --out MyIcon.iconset/icon_256x256.png
    sips -z 512 512 $PNG_ICON --out MyIcon.iconset/icon_256x256@2x.png
    sips -z 512 512 $PNG_ICON --out MyIcon.iconset/icon_512x512.png
    sips -z 1024 1024 $PNG_ICON --out MyIcon.iconset/icon_512x512@2x.png
    
    # 轉換為ICNS
    iconutil -c icns MyIcon.iconset -o $ICNS_ICON
    
    # 清理臨時文件
    rm -rf MyIcon.iconset
    
    if [ -f "$ICNS_ICON" ]; then
        echo "成功創建ICNS圖標: $ICNS_ICON"
    else
        echo "警告: 創建ICNS圖標失敗，將使用原始PNG圖標"
    fi
else
    echo "非macOS系統，跳過ICNS轉換"
fi

# 運行Gradle打包任務
echo "開始創建應用程式包..."
if [ "$IS_MAC" = true ]; then
    # 運行macOS打包任務
    ./gradlew createMacApp
    echo "macOS應用程式包創建完成，請在build/jpackage目錄中查看"
else
    # 運行Windows打包任務
    ./gradlew createWindowsApp
    echo "Windows應用程式包創建完成，請在build/jpackage目錄中查看"
fi

echo "打包過程完成" 