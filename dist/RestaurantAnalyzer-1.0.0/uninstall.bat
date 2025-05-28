@echo off
echo 🗑️ 解除安裝餐廳分析器...
set /p "confirm=確定要刪除所有文件嗎？(y/N): "
if /i "%confirm%"=="y" (
    cd ..
    rmdir /s /q "%~dp0"
    echo ✅ 解除安裝完成
    pause
) else (
    echo ❌ 解除安裝已取消
    pause
)
