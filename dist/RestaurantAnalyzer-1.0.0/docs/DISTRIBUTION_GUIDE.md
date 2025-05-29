# 🚀 餐廳分析器分發指南
# Restaurant Analyzer Distribution Guide

## 📦 可用下載格式

### 🌐 跨平台版本（推薦）
**檔案：** `RestaurantAnalyzer-1.0.0-crossplatform.zip`
**適用系統：** Windows 10+, macOS 10.15+, Linux
**檔案大小：** ~16MB

#### 安裝步驟：
1. 下載 ZIP 檔案
2. 解壓縮到任意資料夾
3. 根據你的作業系統執行：
   - **Windows：** 雙擊 `RestaurantAnalyzer.bat`
   - **Mac/Linux：** 在終端執行 `./RestaurantAnalyzer.sh`

### 🍎 Mac 專用版本
**檔案：** `RestaurantAnalyzer-1.0.0-mac.dmg`
**適用系統：** macOS 10.15+
**檔案大小：** ~16MB

#### 安裝步驟：
1. 下載 DMG 檔案
2. 雙擊開啟 DMG
3. 將「餐廳分析器」拖拽到「Applications」資料夾
4. 從 Launchpad 或 Applications 資料夾啟動

## 🎯 系統需求

### 必要條件
- **Java 21 或更新版本**
  - 下載地址：https://www.oracle.com/java/technologies/downloads/
  - 或使用 OpenJDK：https://adoptium.net/
- **至少 2GB RAM**
- **300MB 可用磁碟空間**

### 可選組件
- **Python 3.8+** (用於進階數據收集功能)
  - 下載地址：https://www.python.org/downloads/

## 🔧 故障排除

### Java 相關問題
**問題：** "找不到 Java" 或 "Java 版本過舊"
**解決方案：**
1. 安裝 Java 21+：https://www.oracle.com/java/technologies/downloads/
2. 確保 Java 在系統 PATH 中
3. 重新啟動應用程式

### 權限問題 (Mac/Linux)
**問題：** "Permission denied" 或無法執行腳本
**解決方案：**
```bash
chmod +x RestaurantAnalyzer.sh
./RestaurantAnalyzer.sh
```

### Windows 編碼問題
**問題：** 顯示亂碼或中文不正常
**解決方案：**
1. 以管理員身份開啟 PowerShell
2. 執行：`chcp 65001`
3. 重新運行 `RestaurantAnalyzer.bat`

### 記憶體不足
**問題：** 應用程式啟動緩慢或當機
**解決方案：**
- 關閉其他大型應用程式
- 確保至少有 2GB 可用 RAM

## 📱 分發建議

### 對於一般用戶
- 使用 **跨平台 ZIP 包**
- 提供詳細的安裝影片教學
- 準備 Java 安裝指南

### 對於 Mac 用戶
- 推薦使用 **Mac DMG 包**
- 更原生的 Mac 體驗
- 可能需要在「安全性與隱私權」中允許應用程式

### 對於企業用戶
- 提供批次部署腳本
- 考慮使用 Docker 容器化部署
- 提供技術支援文件

## 🌍 網路分發平台

### GitHub Releases（免費）
1. 建立 GitHub Repository
2. 上傳分發包到 Releases
3. 用戶可直接下載

### 其他平台選項
- **Google Drive** - 簡單分享
- **DropBox** - 檔案同步
- **自架網站** - 完全控制
- **Microsoft Store** - Windows 用戶
- **Mac App Store** - Mac 用戶（需開發者帳號）

## 📊 版本管理

### 版本號格式
- `1.0.0` - 穩定版
- `1.0.0-beta` - 測試版
- `1.0.0-rc1` - 發布候選版

### 更新策略
1. 自動檢查更新（未來功能）
2. 手動下載新版本
3. 覆蓋安裝或並行安裝

## 🆘 技術支援

### 回報問題
1. 查看應用程式日誌
2. 記錄錯誤訊息
3. 提供系統資訊
4. 聯繫技術支援

### 聯繫方式
- GitHub Issues：https://github.com/yourusername/restaurant-analyzer
- Email：your.email@example.com
- 文檔：查看 docs/ 資料夾

---

**最後更新：** 2024年12月
**版本：** 1.0.0 