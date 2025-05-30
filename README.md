# 🍽️ 餐廳分析器 - Restaurant Analyzer

[![Java](https://img.shields.io/badge/Java-21+-blue.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg)](https://openjfx.io/)
[![Python](https://img.shields.io/badge/Python-3.8+-green.svg)](https://www.python.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一個先進的餐廳市場分析應用程式，整合 AI 分析、智能搜尋和視覺化數據展示。專為餐飲業經營者和市場分析師設計。

## 🚀 核心特色

- **🤖 AI 智能分析** - 整合 Ollama AI 引擎，提供餐廳經營建議
- **🔍 智能搜尋系統** - 自動名稱匹配、重複檢測、用戶確認機制
- **📊 實時數據收集** - Google Places API + Firebase 雲端數據庫
- **💬 AI 聊天助手** - 即時回答餐廳經營相關問題
- **📱 現代化 UI** - JavaFX 打造的美觀用戶界面
- **🔧 零安裝配置** - 智能檢測系統環境，自動配置所需組件

## 📦 快速開始

### 🎯 使用者下載

查看 [**QUICK_START.md**](QUICK_START.md) 獲取詳細使用指南。

**Windows 用戶（推薦）：**
```bash
# 下載 dist/ 目錄中的兩個檔案
# 1. 智能安裝.bat (10KB)
# 2. 餐廳分析器-智能安裝版.zip (250MB)

# 執行安裝
雙擊 智能安裝.bat
```

**Mac 用戶：**
```bash
# 下載並安裝 DMG 檔案
open RestaurantAnalyzer-Working.dmg
```

### 🛠️ 開發者設置

```bash
# 克隆專案
git clone https://github.com/yourusername/restaurant-analyzer.git
cd restaurant-analyzer

# 使用 Gradle 運行
./gradlew run

# 或者使用 IDE (IntelliJ IDEA / Eclipse)
# 導入為 Gradle 專案
```

## 🏗️ 技術架構

### 核心技術棧
- **前端：** JavaFX 21 + CSS 主題
- **後端：** Java 21 + Gradle 建置系統
- **AI 引擎：** Ollama (本地 AI 模型)
- **數據庫：** Firebase Firestore
- **數據收集：** Python + Google Places API
- **搜尋服務：** Algolia Search

### 系統架構圖
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   JavaFX UI     │    │   Data Collector │    │   AI Assistant  │
│                 │    │   (Python)       │    │   (Ollama)      │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Core Application                            │
│                     (Java Backend)                             │
└─────────────────────────────────────────────────────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Firebase      │    │   Google Places  │    │   Local Storage │
│   Database      │    │   API            │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### 詳細架構說明
查看 [**程式架構說明.md**](程式架構說明.md) 了解每個模組的詳細功能。

## 📊 主要功能

### 🔍 智能搜尋
- 自動名稱匹配和補全
- 重複餐廳檢測
- 智能搜尋建議
- 多平台數據整合

### 📈 數據分析
- 即時評分統計
- 競爭對手比較
- 消費水平分析
- 趨勢預測

### 🤖 AI 助手
- 餐廳經營建議
- 市場分析報告
- 智能問答系統
- 個性化推薦

### 🗺️ 地圖整合
- Google Maps 顯示
- 周邊競爭分析
- 商圈評估
- 位置優劣分析

## 🔧 開發指南

### 建置專案
```bash
# 編譯專案
./gradlew build

# 運行測試
./gradlew test

# 創建發布包
./gradlew createDistribution
```

### 代碼結構
```
src/main/java/bigproject/
├── compare.java              # 主界面控制器
├── AppLauncher.java          # 應用程式啟動器
├── ui/                       # 用戶界面組件
├── data/                     # 數據管理模組
├── ai/                       # AI 功能模組
└── search/                   # 搜尋功能模組
```

## 🤝 貢獻指南

1. Fork 專案
2. 創建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

## 📄 許可證

本專案採用 MIT 許可證 - 查看 [LICENSE](LICENSE) 檔案了解詳情。

## 🌟 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=yourusername/restaurant-analyzer&type=Date)](https://star-history.com/#yourusername/restaurant-analyzer&Date)

## 📧 聯絡我們

- 作者：[Your Name](https://github.com/yourusername)
- Email：your.email@example.com
- 專案連結：[https://github.com/yourusername/restaurant-analyzer](https://github.com/yourusername/restaurant-analyzer)

---

⭐ 如果這個專案對你有幫助，請給一個 Star！

