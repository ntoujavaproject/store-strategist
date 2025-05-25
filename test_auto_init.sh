#!/bin/bash

echo "🧪 AI 自動初始化功能測試腳本"
echo "================================"

# 檢查當前 Ollama 狀態
echo "📋 檢查當前 AI 組件狀態..."

if [ -d "$HOME/.ollama" ]; then
    echo "✅ Ollama 已安裝在: $HOME/.ollama"
    
    if [ -f "$HOME/.ollama/bin/ollama" ]; then
        echo "✅ Ollama 可執行檔案存在"
        
        # 檢查模型
        if $HOME/.ollama/bin/ollama list | grep -q "gemma3:4b"; then
            echo "✅ gemma3:4b 模型已下載"
            echo ""
            echo "🎯 測試情境: 已安裝環境"
            echo "   預期結果: 程式啟動後不會顯示進度對話框"
            echo "   控制台應顯示: 'AI 功能已就緒'"
        else
            echo "❌ gemma3:4b 模型未下載"
            echo ""
            echo "🎯 測試情境: 部分安裝環境"
            echo "   預期結果: 程式啟動後會顯示進度對話框下載模型"
        fi
    else
        echo "❌ Ollama 可執行檔案不存在"
    fi
else
    echo "❌ Ollama 未安裝"
    echo ""
    echo "🎯 測試情境: 全新安裝環境"
    echo "   預期結果: 程式啟動後會顯示進度對話框下載 Ollama 和模型"
fi

echo ""
echo "🚀 啟動程式進行測試..."
echo "   請觀察程式啟動約 3 秒後是否自動顯示 AI 初始化對話框"
echo ""

# 啟動程式
./gradlew run

echo ""
echo "📝 測試完成！"
echo ""
echo "如果要測試全新安裝情境，請執行："
echo "   rm -rf ~/.ollama"
echo "   ./test_auto_init.sh" 