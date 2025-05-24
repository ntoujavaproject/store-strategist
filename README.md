# 餐廳市場分析系統 (Store Strategist)

一個用於餐廳市場分析和競爭情報收集的Java應用程式。

## 功能

- 收集並分析餐廳評論數據
- 比較競爭對手的表現
- 視覺化評分和評論資訊
- **智能餐廳搜尋系統**
  - 當搜尋不到餐廳時，自動詢問是否要從 Google Maps 收集資料
  - 自動上傳新餐廳資料到 Firebase 資料庫
  - 支援精選評論收集（含在地嚮導權重系統）
- 先進AI分析功能
  - 自動評論摘要生成
  - 餐廳改進建議與路線圖
  - 面向情感分析
  - 整合Ollama大型語言模型（自動安裝）
  - 智能對話與分析

## 技術架構

- Java + JavaFX
- Google Places API
- JSON數據處理
- Python數據收集工具
- Ollama大型語言模型整合（無需手動安裝）
- Firebase Firestore 資料庫
- Algolia 搜尋引擎

## 系統需求

- Java 11或更高版本
- Python 3.8+ (用於資料收集功能)
- Gradle建置工具
- 網路連接（用於API功能和初次下載AI模型）

## 安裝與使用

1. 複製專案
```bash
git clone https://github.com/ntoujavaproject/store-strategist.git
```

2. 安裝 Python 依賴
```bash
pip install -r requirements.txt
```

3. 建置專案
```bash
./gradlew build
```

4. 執行應用程式
```bash
./gradlew run
```

## 智能搜尋功能說明

### 自動餐廳收集系統

當您在搜尋欄中輸入餐廳名稱，但在資料庫中找不到時，系統會：

1. **顯示確認對話框** - 詢問是否要自動收集該餐廳的資料
2. **自動資料收集** - 從 Google Maps 抓取餐廳資訊和評論
3. **上傳到 Firebase** - 將收集到的資料自動上傳到雲端資料庫
4. **立即可搜尋** - 收集完成後即可正常搜尋該餐廳

### 使用流程

```
搜尋餐廳 → 找不到 → 詢問是否收集 → 自動收集 → 上傳到資料庫 → 重新搜尋成功
```

### 手動收集餐廳資料

如果您想要手動收集特定餐廳的資料，可以使用以下指令：

```bash
# 根據餐廳名稱收集
python data-collector/search_res_by_name_upload_firebase.py "餐廳名稱"

# 收集區域內所有餐廳
python data-collector/search_res_in_area_upload_firebase.py
```

## AI功能說明

本系統整合了Ollama大型語言模型，**無需用戶手動安裝**：

- 應用程式首次啟動會**自動下載並安裝Ollama**（根據您的操作系統選擇適合的版本）
- 系統會**自動下載Gemma 3 1B模型**（約1.5-2GB，僅首次使用時需要）
- 所有AI功能在本地運行，完全保護您的數據隱私

AI功能包括：

- **智能分析**：自動分析餐廳評論，提取關鍵洞見
- **改進建議**：基於評論生成具體可行的業務改進建議
- **互動式對話**：用戶可通過聊天界面向AI提問，獲取專業建議

若首次啟動時沒有網絡連接或下載失敗，系統會自動使用內置的備用分析功能，不影響核心功能使用。

## 餐廳搜索系統

此專案包含了一個強大的餐廳搜索系統，使用 Algolia 搜索引擎實現高效、模糊搜索功能。

### 主要功能

- **智能搜尋** - 支援模糊搜索和拼寫錯誤容錯
- **自動完成** - 輸入時即時顯示搜尋建議
- **資料同步**：自動將 Firebase 中的餐廳資料同步到 Algolia 搜索引擎
- **降級搜索策略**：當精確搜索無結果時，自動嘗試替代搜索方式
- **自動收集**：找不到餐廳時自動從 Google Maps 收集並上傳

### 使用方法

#### 同步資料到搜尋引擎

```bash
# 在專案根目錄執行
python scripts/Firebase2AlgoliaSync.py
```

#### 精選評論收集

```bash
# 收集特定餐廳的精選評論（含在地嚮導權重）
python data-collector/featured_collector.py --id 餐廳ID --name 餐廳名稱

# 或使用搜尋方式
python data-collector/featured_collector.py --search 餐廳名稱
```

## 專案特色

### 🔍 智能搜尋
- 即時搜尋建議
- 模糊搜索容錯
- 自動餐廳收集

### 📊 精選評論系統
- 多維度評分（權威性35%、質量35%、新近性20%、評分10%）
- 在地嚮導額外加分機制
- 智能評論篩選

### 🤖 AI 分析
- 本地運行保護隱私
- 智能對話系統
- 自動改進建議

### ☁️ 雲端整合
- Firebase 資料同步
- Algolia 高效搜尋
- 自動資料備份

## 開發團隊

此專案由 NTOU Java 專案團隊開發，專注於餐廳市場分析和商業智能應用。

