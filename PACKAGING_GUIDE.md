# 餐廳分析器打包指南 
# Restaurant Analyzer Packaging Guide

## 📦 快速打包 Quick Packaging

### Mac/Linux 用戶
```bash
chmod +x package.sh
./package.sh
```

### Windows 用戶
```cmd
package.bat
```

## 🎯 打包內容 Package Contents

打包後會創建以下結構：
```
RestaurantAnalyzer-1.0.0/
├── lib/                          # Java JAR 文件
│   └── Restaurant Analyzer-1.0.0.jar
├── data-collector/               # Python 數據收集器
├── scripts/                      # 工具腳本
├── docs/                         # 文檔文件
├── RestaurantAnalyzer.sh         # Mac/Linux 啟動腳本
├── RestaurantAnalyzer.bat        # Windows 啟動腳本
├── requirements.txt              # Python 依賴
├── README.txt                    # 用戶說明
├── uninstall.sh                  # Mac/Linux 解除安裝
└── uninstall.bat                 # Windows 解除安裝
```

## 🚀 分發包 Distribution Packages

打包完成後會生成：

1. **跨平台 ZIP 包**：`RestaurantAnalyzer-1.0.0-crossplatform.zip`
   - 適用於 Windows、Mac、Linux
   - 用戶解壓後直接運行對應腳本

2. **Mac DMG 包**（僅 Mac 環境）：`RestaurantAnalyzer-1.0.0-mac.dmg`
   - 專為 Mac 用戶設計的安裝包

## 📋 系統需求 System Requirements

### 用戶端需求：
- **Java 21+** (必需)
- **Python 3.8+** (可選，用於數據收集功能)
- **2GB RAM** (建議)
- **300MB 磁碟空間**

### 開發端需求：
- Java 21+ JDK
- Gradle 8.x
- Git (可選)

## 🛠️ 自訂打包 Custom Packaging

### 修改版本號
編輯打包腳本中的版本號：
```bash
VERSION="1.0.0"  # 修改為您的版本號
```

### 添加額外文件
在打包腳本中的文件複製部分添加：
```bash
# 複製其他文件
cp your_file.txt "$PACKAGE_DIR/"
```

### 修改應用程式名稱
在 `build.gradle` 中修改：
```gradle
ext {
    appName = "Your App Name"
    appVersion = "1.0.0"
}
```

## 🔧 故障排除 Troubleshooting

### 常見問題：

1. **Gradle 編譯失敗**
   ```bash
   # 清理並重新編譯
   ./gradlew clean
   ./gradlew build
   ```

2. **權限問題 (Mac/Linux)**
   ```bash
   chmod +x package.sh
   chmod +x gradlew
   ```

3. **編碼問題 (Windows)**
   - 確保 PowerShell 或 cmd 支援 UTF-8
   - 使用 `chcp 65001` 設定編碼

4. **Java 版本問題**
   ```bash
   # 檢查 Java 版本
   java -version
   javac -version
   ```

## 📤 分發建議 Distribution Recommendations

### 對於一般用戶：
- 提供 **ZIP 包** + **詳細安裝說明**
- 包含 Java 安裝鏈接
- 提供影片教學（如有需要）

### 對於技術用戶：
- 提供 **原始碼** + **編譯說明**
- GitHub Releases 頁面
- Docker 映像（可選）

### 對於企業用戶：
- 提供 **MSI 安裝包** (Windows)
- 提供 **PKG 安裝包** (Mac)
- 提供 **DEB/RPM 包** (Linux)

## 🏗️ 進階打包選項 Advanced Packaging

### 使用 jpackage (Java 21+)
```bash
# 創建原生安裝包
jpackage --input lib \
         --name "Restaurant Analyzer" \
         --main-jar "Restaurant Analyzer-1.0.0.jar" \
         --type msi  # Windows
         --type dmg  # Mac
         --type deb  # Linux
```

### 使用 Docker
```dockerfile
FROM openjdk:21-jre-slim
COPY dist/RestaurantAnalyzer-1.0.0 /app
WORKDIR /app
CMD ["java", "-jar", "lib/Restaurant Analyzer-1.0.0.jar"]
```

## 📊 版本管理 Version Management

### 語義化版本 (Semantic Versioning)
- `1.0.0` - 主要版本.次要版本.修補版本
- `1.0.0-beta` - 測試版本
- `1.0.0-rc1` - 發布候選版本

### 發布流程
1. 更新版本號
2. 更新 CHANGELOG.md
3. 執行完整測試
4. 執行打包腳本
5. 創建 Git 標籤
6. 上傳到發布平台

## 📞 支援 Support

如有打包相關問題：
1. 檢查 `build.gradle` 配置
2. 確認所有依賴都已正確安裝
3. 查看打包腳本的錯誤訊息
4. 檢查 Java 和 Python 環境

---

**注意**：首次打包可能需要下載依賴，請確保網路連線正常。 