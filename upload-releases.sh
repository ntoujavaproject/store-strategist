#!/bin/bash

# 餐廳分析器發布腳本
# Restaurant Analyzer Release Script

echo "🚀 準備發布餐廳分析器..."

VERSION="1.0.0"
DIST_DIR="dist"
RELEASE_NOTES="release-notes.md"

# 檢查分發包是否存在
if [ ! -f "$DIST_DIR/RestaurantAnalyzer-$VERSION-crossplatform.zip" ]; then
    echo "❌ 找不到跨平台分發包，請先執行 ./package.sh"
    exit 1
fi

if [ ! -f "$DIST_DIR/RestaurantAnalyzer-$VERSION-mac.dmg" ]; then
    echo "❌ 找不到 Mac DMG 包，請先執行 ./package.sh"
    exit 1
fi

# 創建發布說明
echo "📝 創建發布說明..."
cat > "$RELEASE_NOTES" << EOF
# 餐廳分析器 v$VERSION 發布說明

## 🎉 新功能
- ✨ 全新的餐廳評論分析功能
- 🤖 整合 AI 聊天助手
- 📊 進階評分視覺化
- 🌍 跨平台支援

## 📦 下載選項

### 🌐 跨平台版本（推薦）
**檔案：** RestaurantAnalyzer-$VERSION-crossplatform.zip
- 適用於 Windows 10+, macOS 10.15+, Linux
- 包含自動環境設置腳本
- 檔案大小：~16MB

### 🍎 Mac 專用版本
**檔案：** RestaurantAnalyzer-$VERSION-mac.dmg
- 專為 macOS 最佳化
- 原生安裝體驗
- 檔案大小：~16MB

## 🎯 系統需求
- Java 21+ (必需)
- Python 3.8+ (可選，用於數據收集)
- 2GB RAM (建議)
- 300MB 磁碟空間

## 🔧 快速開始
1. 下載對應你系統的版本
2. 解壓縮或安裝
3. 執行啟動腳本
4. 首次啟動會自動設置環境

## 📞 技術支援
- 📖 查看 README.txt 獲取詳細說明
- 🐛 回報問題：[GitHub Issues](https://github.com/yourusername/restaurant-analyzer/issues)
- 📧 聯繫：your.email@example.com

---
發布日期：$(date '+%Y-%m-%d')
EOF

echo "✅ 發布說明已創建：$RELEASE_NOTES"

# 顯示檔案資訊
echo ""
echo "📦 發布包資訊："
echo "├── 跨平台版本："
ls -lh "$DIST_DIR/RestaurantAnalyzer-$VERSION-crossplatform.zip"
echo "├── Mac 版本："
ls -lh "$DIST_DIR/RestaurantAnalyzer-$VERSION-mac.dmg"
echo "└── 發布目錄："
ls -lh "$DIST_DIR/RestaurantAnalyzer-$VERSION/"

echo ""
echo "🌐 分發平台選項："
echo ""
echo "1. 📱 GitHub Releases (推薦)："
echo "   - 前往你的 GitHub Repository"
echo "   - 點擊 'Releases' → 'Create a new release'"
echo "   - 上傳以上兩個檔案"
echo "   - 複製 $RELEASE_NOTES 的內容作為發布說明"
echo ""
echo "2. 📂 Google Drive/DropBox："
echo "   - 上傳檔案到雲端硬碟"
echo "   - 設置公開分享連結"
echo "   - 在網站或社群媒體分享連結"
echo ""
echo "3. 🌍 自架網站："
echo "   - 上傳檔案到你的網站"
echo "   - 創建下載頁面"
echo "   - 提供直接下載連結"
echo ""
echo "4. 💿 實體分發："
echo "   - 燒錄到 USB 隨身碟"
echo "   - 製作安裝光碟"
echo "   - 本地網路分享"

echo ""
echo "🎯 建議的分發策略："
echo "✅ 主要：GitHub Releases（免費、專業、有版本控制）"
echo "✅ 備用：Google Drive（簡單、快速分享）"
echo "✅ 行銷：社群媒體宣傳"
echo "✅ 文檔：提供詳細的安裝教學"

echo ""
echo "📋 分發檢查清單："
echo "□ 上傳跨平台 ZIP 包"
echo "□ 上傳 Mac DMG 包"
echo "□ 撰寫清楚的發布說明"
echo "□ 測試下載連結"
echo "□ 準備技術支援文檔"
echo "□ 建立回饋收集機制"

echo ""
echo "🎉 準備完成！現在可以開始分發你的軟體了！" 