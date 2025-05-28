#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
檢查餐廳是否已存在於 Firebase 中
用於在收集餐廳資料前先確認是否需要重複收集
"""

import sys
import requests

# Firebase 設定
FIREBASE_PROJECT_ID = "java2025-91d74"

def check_restaurant_exists_in_firebase(restaurant_name):
    """檢查餐廳是否存在於 Firebase"""
    try:
        # 使用 Firebase REST API 搜尋餐廳
        url = f"https://firestore.googleapis.com/v1/projects/{FIREBASE_PROJECT_ID}/databases/(default)/documents/restaurants"
        
        # 分頁獲取所有餐廳資料
        all_docs = []
        page_token = None
        max_pages = 10  # 限制最大頁數避免無限循環
        
        for page in range(max_pages):
            params = {'pageSize': 100}
            if page_token:
                params['pageToken'] = page_token
                
            response = requests.get(url, params=params, timeout=10)
            
            if response.status_code != 200:
                print(f"ERROR:Firebase API錯誤 - {response.status_code}")
                return False
                
            data = response.json()
            
            if 'documents' in data:
                all_docs.extend(data['documents'])
                
            # 檢查是否還有下一頁
            if 'nextPageToken' not in data:
                break
            page_token = data['nextPageToken']
        
        if not all_docs:
            print("ERROR:Firebase中沒有找到任何餐廳資料")
            return False
            
        print(f"INFO:從Firebase獲取了 {len(all_docs)} 家餐廳資料")
        
        # 搜尋匹配的餐廳 - 使用多種匹配方式
        for doc in all_docs:
            fields = doc.get('fields', {})
            name = fields.get('name', {}).get('stringValue', '')
            
            # 多種匹配方式
            matches = [
                restaurant_name == name,  # 完全匹配
                restaurant_name in name,  # 部分匹配
                name in restaurant_name,  # 反向部分匹配
                # 移除空格後比較
                restaurant_name.replace(' ', '') == name.replace(' ', ''),
                # 移除常見標點符號後比較
                restaurant_name.replace('-', '').replace('－', '') in name.replace('-', '').replace('－', ''),
                # 移除常見詞彙後比較
                restaurant_name.replace('餐廳', '').replace('小吃', '').replace('店', '') in name.replace('餐廳', '').replace('小吃', '').replace('店', '')
            ]
            
            if any(matches):
                print(f"INFO:在Firebase中找到匹配餐廳")
                print(f"INFO:搜尋詞：{restaurant_name}")
                print(f"INFO:找到的餐廳：{name}")
                print(f"INFO:地址：{fields.get('address', {}).get('stringValue', 'Unknown')}")
                print(f"INFO:文檔ID：{doc['name'].split('/')[-1]}")
                return True
                
        print(f"INFO:在Firebase中找不到餐廳：{restaurant_name}")
        return False
        
    except Exception as e:
        print(f"ERROR:檢查Firebase時發生錯誤 - {str(e)}")
        return False

def main():
    """主程式"""
    if len(sys.argv) < 2:
        print("ERROR:Missing restaurant name argument")
        sys.exit(1)
    
    restaurant_name = sys.argv[1]
    
    # 檢查餐廳是否存在
    exists = check_restaurant_exists_in_firebase(restaurant_name)
    
    # 輸出結果
    print(f"EXISTS:{exists}")
    
    # 設定退出碼
    sys.exit(0 if exists else 1)

if __name__ == "__main__":
    main() 