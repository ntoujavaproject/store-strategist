#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
同步遺漏的餐廳到 Algolia 搜尋引擎

此腳本會：
1. 獲取 Firebase 中所有餐廳的列表
2. 檢查每家餐廳是否存在於 Algolia 搜尋引擎中
3. 將不存在的餐廳同步到 Algolia

使用方式：
python sync_missing_restaurants.py
"""

import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'data-collector'))

import requests
from algoliasearch.search_client import SearchClient
from config import project_id

# Algolia 設定
ALGOLIA_APP_ID = "V81B79H3KW"
ALGOLIA_API_KEY = "26825e7d99094b8abf5b76b7c4abf9e8"
ALGOLIA_INDEX_NAME = "restaurants"

def get_all_restaurants_from_firebase():
    """獲取 Firebase 中所有餐廳的資料"""
    try:
        url = f"https://firestore.googleapis.com/v1/projects/{project_id}/databases/(default)/documents/restaurants"
        params = {
            "pageSize": 1000  # 每次最多獲取 1000 筆
        }
        
        all_restaurants = []
        page_token = None
        
        while True:
            if page_token:
                params["pageToken"] = page_token
            
            response = requests.get(url, params=params, timeout=30)
            response.raise_for_status()
            data = response.json()
            
            if "documents" not in data:
                break
            
            # 提取餐廳資料
            for doc in data["documents"]:
                fields = doc.get("fields", {})
                restaurant_name = fields.get("name", {}).get("stringValue", "")
                restaurant_address = fields.get("address", {}).get("stringValue", "")
                restaurant_id = fields.get("id", {}).get("stringValue", "")
                
                if restaurant_name:
                    all_restaurants.append({
                        "name": restaurant_name,
                        "address": restaurant_address,
                        "id": restaurant_id
                    })
            
            # 檢查是否有下一頁
            page_token = data.get("nextPageToken")
            if not page_token:
                break
        
        print(f"✅ 從 Firebase 獲取到 {len(all_restaurants)} 家餐廳")
        return all_restaurants
        
    except Exception as e:
        print(f"❌ 獲取 Firebase 餐廳列表失敗：{e}")
        return []

def check_restaurant_in_algolia(client, restaurant_name):
    """檢查餐廳是否存在於 Algolia 中"""
    try:
        index = client.init_index(ALGOLIA_INDEX_NAME)
        results = index.search(restaurant_name, {
            "hitsPerPage": 1,
            "attributesToRetrieve": ["name"],
            "typoTolerance": False  # 精確匹配
        })
        
        # 檢查是否有完全匹配的結果
        for hit in results.get("hits", []):
            if hit.get("name", "").strip() == restaurant_name.strip():
                return True
        
        return False
        
    except Exception as e:
        print(f"⚠️ 檢查 Algolia 時發生錯誤：{e}")
        return False

def add_restaurant_to_algolia(client, restaurant):
    """將餐廳添加到 Algolia"""
    try:
        index = client.init_index(ALGOLIA_INDEX_NAME)
        
        record = {
            "objectID": restaurant["name"],
            "name": restaurant["name"],
            "address": restaurant["address"],
            "id": restaurant["id"]
        }
        
        index.save_object(record)
        return True
        
    except Exception as e:
        print(f"❌ 添加到 Algolia 失敗：{e}")
        return False

def sync_missing_restaurants():
    """主要同步函數"""
    print("🔍 開始檢查並同步遺漏的餐廳...")
    
    # 1. 獲取 Firebase 中的所有餐廳
    firebase_restaurants = get_all_restaurants_from_firebase()
    if not firebase_restaurants:
        print("❌ 無法獲取 Firebase 餐廳列表，停止同步")
        return
    
    # 2. 初始化 Algolia 客戶端
    try:
        client = SearchClient.create(ALGOLIA_APP_ID, ALGOLIA_API_KEY)
        print("✅ Algolia 客戶端初始化成功")
    except Exception as e:
        print(f"❌ Algolia 客戶端初始化失敗：{e}")
        return
    
    # 3. 檢查每家餐廳並同步遺漏的
    missing_count = 0
    synced_count = 0
    
    for i, restaurant in enumerate(firebase_restaurants, 1):
        restaurant_name = restaurant["name"]
        print(f"🔍 檢查 ({i}/{len(firebase_restaurants)}): {restaurant_name}")
        
        # 檢查是否存在於 Algolia
        exists_in_algolia = check_restaurant_in_algolia(client, restaurant_name)
        
        if not exists_in_algolia:
            print(f"❌ 在 Algolia 中找不到：{restaurant_name}")
            missing_count += 1
            
            # 嘗試添加到 Algolia
            if add_restaurant_to_algolia(client, restaurant):
                print(f"✅ 成功同步：{restaurant_name}")
                synced_count += 1
            else:
                print(f"❌ 同步失敗：{restaurant_name}")
        else:
            print(f"✅ 已存在於 Algolia：{restaurant_name}")
    
    # 4. 總結報告
    print("\n" + "="*50)
    print("📊 同步結果總結：")
    print(f"📋 Firebase 總餐廳數：{len(firebase_restaurants)}")
    print(f"❌ 遺漏餐廳數：{missing_count}")
    print(f"✅ 成功同步數：{synced_count}")
    print(f"❌ 同步失敗數：{missing_count - synced_count}")
    print("="*50)
    
    if synced_count > 0:
        print(f"🎉 已將 {synced_count} 家餐廳同步到 Algolia！")
    else:
        print("✨ 所有餐廳都已同步，無需操作")

if __name__ == "__main__":
    try:
        sync_missing_restaurants()
    except KeyboardInterrupt:
        print("\n⏹️ 用戶中斷操作")
    except Exception as e:
        print(f"💥 程式執行時發生錯誤：{e}") 