#!/usr/bin/env python3
# -*- coding: utf-8 -*-
'''
此程式用於根據餐廳名稱搜尋並上傳資料到 Firestore

執行流程：
1. 從命令行參數或使用者輸入獲取餐廳名稱
2. 根據餐廳名稱搜尋餐廳ID
3. 使用ID獲取餐廳詳細資訊
4. 抓取該餐廳的所有評論
5. 將餐廳資料和評論上傳到 Firestore

錯誤處理：
- 找不到餐廳ID時會拋出ValueError
- 無法獲取餐廳資訊時會拋出ValueError
- 其他未預期的錯誤會被捕捉並顯示錯誤信息

命令行使用方式：
python search_res_by_name_upload_firebase.py [餐廳名稱]
'''

import sys
from utils import (
    get_restaurant_id_by_name,
    get_restaurant_info_by_id,
)
from restaurant import Restaurant

def collect_and_upload_restaurant(restaurant_name):
    """
    收集餐廳資料並上傳到 Firebase
    
    Args:
        restaurant_name (str): 餐廳名稱
        
    Returns:
        dict: 包含成功狀態和餐廳資訊的字典
    """
    try:
        print(f"正在搜尋餐廳：{restaurant_name}")
        restaurant_id = get_restaurant_id_by_name(restaurant_name)
        if not restaurant_id:
            raise ValueError("無法找到該餐廳的 ID，請確認餐廳名稱是否正確。")

        print(f"找到餐廳 ID：{restaurant_id}")
        restaurant_name_actual, restaurant_address = get_restaurant_info_by_id(restaurant_id)
        if not restaurant_name_actual or not restaurant_address:
            raise ValueError("無法取得餐廳資訊，請確認餐廳 ID 是否正確。")

        print(f"餐廳資訊：{restaurant_name_actual} - {restaurant_address}")
        print("正在收集評論資料...")
        
        restaurant = Restaurant(restaurant_id, restaurant_name_actual, restaurant_address)
        restaurant.get_reviews()
        
        print("正在上傳到 Firestore...")
        restaurant.upload_to_firestore()
        
        print("餐廳資訊已成功上傳至 Firestore。")
        
        # 自動同步到 Algolia
        print("正在自動同步到 Algolia 搜尋引擎...")
        try:
            import subprocess
            import os
            
            # 執行 auto_sync_restaurant.py
            script_dir = os.path.dirname(os.path.abspath(__file__))
            sync_script = os.path.join(script_dir, '..', 'scripts', 'auto_sync_restaurant.py')
            
            result = subprocess.run([
                sys.executable, sync_script, restaurant_name_actual
            ], capture_output=True, text=True, timeout=30)
            
            if result.returncode == 0:
                print("✅ 已自動同步到 Algolia！現在可以搜尋到這家餐廳了。")
            else:
                print("⚠️ Algolia 同步失敗，但 Firebase 上傳成功")
                print(f"錯誤訊息：{result.stderr}")
                
        except Exception as sync_error:
            print(f"⚠️ 自動同步時發生錯誤：{sync_error}")
            print("Firebase 上傳成功，但可能需要手動同步到搜尋引擎")
        
        return {
            'success': True,
            'restaurant_id': restaurant_id,
            'restaurant_name': restaurant_name_actual,
            'restaurant_address': restaurant_address,
            'reviews_count': len(restaurant.reviews) if hasattr(restaurant, 'reviews') else 0
        }

    except ValueError as ve:
        error_msg = f"錯誤: {ve}"
        print(error_msg)
        return {'success': False, 'error': str(ve)}
    except Exception as e:
        error_msg = f"發生未預期的錯誤: {e}"
        print(error_msg)
        return {'success': False, 'error': str(e)}

def main():
    """主程式"""
    try:
        # 檢查是否有命令行參數
        if len(sys.argv) > 1:
            # 使用命令行參數
            restaurant_name = ' '.join(sys.argv[1:])  # 支援包含空格的餐廳名稱
        else:
            # 從使用者輸入獲取
            restaurant_name = input("請輸入餐廳名稱: ")
        
        if not restaurant_name.strip():
            print("錯誤：餐廳名稱不能為空")
            sys.exit(1)
        
        # 收集並上傳資料
        result = collect_and_upload_restaurant(restaurant_name.strip())
        
        # 根據結果設定退出碼
        if result['success']:
            print("\n=== 上傳成功 ===")
            print(f"餐廳名稱：{result['restaurant_name']}")
            print(f"餐廳地址：{result['restaurant_address']}")
            print(f"評論數量：{result['reviews_count']}")
            sys.exit(0)  # 成功
        else:
            print(f"\n=== 上傳失敗 ===")
            print(f"錯誤原因：{result['error']}")
            sys.exit(1)  # 失敗
            
    except KeyboardInterrupt:
        print("\n程式被用戶中斷")
        sys.exit(1)
    except Exception as e:
        print(f"程式執行時發生未預期錯誤：{e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
