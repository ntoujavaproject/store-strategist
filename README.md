# 🍽️ 餐廳分析器 - Restaurant Analyzer

一個基於 JavaFX 的餐廳市場分析應用程式，具備智能搜尋、資料收集和視覺化分析功能。

## 📦 下載安裝

### 🚀 快速下載（推薦）

**現成版本下載：** 前往 `dist/` 資料夾，選擇適合你系統的版本：

#### 🌐 跨平台版本（Windows、Mac、Linux）
- **檔案：** `RestaurantAnalyzer-1.0.0-crossplatform.zip`
- **適用：** Windows 10+, macOS 10.15+, Linux
- **大小：** ~16MB

**安裝步驟：**
1. 下載 ZIP 檔案
2. 解壓縮到任意資料夾
3. 根據你的系統執行：
   - **Windows：** 雙擊 `RestaurantAnalyzer.bat`
   - **Mac/Linux：** 終端執行 `./RestaurantAnalyzer.sh`

#### 🍎 Mac 專用版本
- **檔案：** `RestaurantAnalyzer-1.0.0-mac.dmg`
- **適用：** macOS 10.15+
- **大小：** ~16MB

**安裝步驟：**
1. 下載 DMG 檔案
2. 雙擊開啟 DMG
3. 拖拽到 Applications 資料夾

### 🎯 系統需求
- **Java 21+** (必需) - [下載地址](https://www.oracle.com/java/technologies/downloads/)
- **Python 3.8+** (可選，用於數據收集)
- **2GB RAM** (建議)
- **300MB 磁碟空間**

### 🆘 安裝問題？
查看 [DISTRIBUTION_GUIDE.md](DISTRIBUTION_GUIDE.md) 獲取詳細安裝指南和故障排除方法。

---

## ✨ 主要功能

### 🔍 智能搜尋系統
- **自動名稱匹配**：當搜尋部分餐廳名稱時，系統會自動檢查並顯示完整餐廳名稱供確認
- **重複餐廳檢測**：自動避免重複添加已存在的餐廳到資料庫
- **用戶確認機制**：當搜尋「八方雲集」找到「八方雲集 新竹金山店」時，會詢問用戶確認是否為所需餐廳
- **409 錯誤處理**：智能處理餐廳已存在的情況，提供友善的用戶提示

### 📊 視覺化分析
- **即時評論分析**：顯示餐廳評論趨勢和評分分布
- **多維度評分系統**：食物、服務、環境、價格等各項評分
- **近期評論篩選**：可查看最近 7 天、30 天的評論動態
- **月報功能**：生成美味評分月度趨勢圖表

### 🤖 AI 智能助手
- **經營建議**：基於評論數據提供餐廳經營建議
- **互動式聊天**：可與 AI 討論餐廳分析結果
- **多種分析模式**：支援競爭分析、趨勢預測等

### 🎨 用戶體驗
- **整合式操作界面**：無彈跳視窗干擾，所有操作都在主界面完成
- **響應式設計**：支援不同螢幕尺寸自動調整布局
- **深色主題**：現代化的視覺設計
- **多分頁管理**：可同時分析多家餐廳

## 🛠️ 開發者安裝

### 系統需求
- Java 21 或以上版本
- Python 3.8+ （用於資料收集）
- Gradle 8.x

### 從原始碼安裝

1. **克隆專案**
   ```bash
   git clone https://github.com/ntoujavaproject/restaurant-analyzer.git
   cd restaurant-analyzer
   ```

2. **安裝 Python 依賴**
   ```bash
   pip install -r requirements.txt
   ```

3. **建置並執行**
   ```bash
   ./gradlew run
   ```

4. **打包分發版本**
   ```bash
   # Mac/Linux
   ./package.sh
   
   # Windows
   package.bat
   ```

## 📱 使用指南

### 智能搜尋功能

#### 情境 1：部分名稱搜尋
1. 在搜尋欄輸入「八方雲集」
2. 系統自動搜尋並找到「八方雲集 新竹金山店」
3. 系統顯示確認頁面：「在資料庫中找不到『八方雲集』，但在 Google Maps 中找到：『八方雲集 新竹金山店』」
4. 點擊「確認收集資料」添加到資料庫，或選擇「在地圖中開啟」查看更多資訊

#### 情境 2：餐廳已存在
1. 搜尋已存在的餐廳
2. 系統檢測到 409 衝突（餐廳已存在）
3. 顯示「餐廳已存在」提示，避免重複添加
4. 自動導向現有餐廳資料

#### 情境 3：精確匹配
1. 輸入完整餐廳名稱
2. 系統檢測到高相似度匹配（>= 80%）
3. 自動進行資料收集，無需額外確認

### 分析功能

#### 評論分析
- **時間篩選**：使用右側面板的時間選擇器查看不同時期的評論
- **評分分布**：查看食物、服務、環境、價格等各項評分
- **評論內容**：瀏覽客戶的詳細評論和建議

#### AI 分析
1. 點擊「經營建議」查看 AI 生成的經營策略
2. 使用 AI 聊天功能深入討論分析結果
3. 查看月報了解評分趨勢

## 🛠️ 技術架構

### 前端架構
- **JavaFX 21**：現代化桌面應用框架
- **響應式設計**：自適應不同螢幕尺寸
- **模組化設計**：UIManager、DataManager、AIChat 等分離式架構

### 後端整合
- **Python 資料收集**：使用 Google Maps API 收集餐廳資料
- **Firebase 整合**：雲端資料庫存儲和同步
- **多執行緒處理**：非阻塞式資料收集和上傳

### 資料流程
```
用戶搜尋 → 名稱匹配檢查 → 相似度計算 → 用戶確認 → 資料收集 → Firebase 上傳 → 顯示結果
```

## 🔧 開發設定

### 開發環境配置
```bash
# 設定 Python 虛擬環境
python -m venv .venv
source .venv/bin/activate  # Linux/Mac
# 或
.venv\Scripts\activate     # Windows

# 安裝開發依賴
pip install -r requirements.txt
```

### 建置設定
```bash
# 編譯專案
./gradlew build

# 執行測試
./gradlew test

# 清理建置
./gradlew clean
```

### 程式碼結構
```
src/main/java/bigproject/
├── compare.java              # 主應用程式
├── UIManager.java           # UI 管理器
├── DataManager.java         # 資料管理器
├── AIChat.java             # AI 聊天功能
├── RightPanel.java         # 右側面板
├── SearchBar.java          # 搜尋欄
└── PreferencesManager.java # 設定管理器

data-collector/
├── search_res_by_name_upload_firebase.py  # 單餐廳收集
├── search_res_in_area_upload_firebase.py  # 區域餐廳收集
├── utils.py                               # 工具函數
└── restaurant.py                          # 餐廳資料模型
```

## 🤝 貢獻指南

歡迎提交 Issue 和 Pull Request！

### 開發流程
1. Fork 專案
2. 創建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交變更 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

## 📄 授權

本專案採用 MIT 授權條款 - 詳見 [LICENSE](LICENSE) 檔案

## 🙏 致謝

- Google Maps API for 餐廳資料
- Firebase for 雲端資料庫服務
- JavaFX 社群 for UI 框架支援

---

**Store Strategist** - 讓餐廳經營更智能 🍽️📈

