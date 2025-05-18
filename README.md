# 餐廳市場分析系統 (Store Strategist)

一個用於餐廳市場分析和競爭情報收集的Java應用程式。

## 功能

- 收集並分析餐廳評論數據
- 比較競爭對手的表現
- 視覺化評分和評論資訊
- AI輔助分析建議
  - 自動評論摘要生成
  - 餐廳改進建議與路線圖
  - 面向情感分析
- 自訂主題和界面設定

## 技術架構

- Java + JavaFX
- Google Places API
- JSON數據處理
- Python數據收集工具
- Ollama AI 整合 (LLM 本地分析)

## 系統需求

- Java 11或更高版本
- Gradle建置工具
- 網路連接（用於API功能）
- 可選：Ollama本地部署用於AI功能

## 安裝與使用

1. 複製專案
```
git clone https://github.com/ntoujavaproject/store-strategist.git
```

2. 建置專案
```
./gradlew build
```

3. 執行應用程式
```
./gradlew run
```

## AI功能設置

若要完整啟用AI分析功能，需安裝並運行Ollama：

1. 從 [Ollama官網](https://ollama.ai/) 下載並安裝
2. 拉取所需模型：`ollama pull gemma:1b` 或 `ollama pull gemma3:1b`
3. 確保Ollama服務運行於 http://localhost:11434

## 授權

此專案僅供教育用途，非商業使用。
