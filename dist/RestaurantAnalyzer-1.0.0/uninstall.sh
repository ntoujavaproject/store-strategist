#!/bin/bash
echo "🗑️  解除安裝餐廳分析器..."
read -p "確定要刪除所有文件嗎？(y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    cd ..
    rm -rf "$(basename "$PWD")"
    echo "✅ 解除安裝完成"
else
    echo "❌ 解除安裝已取消"
fi
