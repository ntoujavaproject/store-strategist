#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
自動同步剛上傳的餐廳到Algolia
解決餐廳資料在Firebase但Algolia搜尋不到的問題
"""

import algoliasearch
import requests
import sys

# Algolia 設定
ALGOLIA_APP_ID = "V269PWJYC3"
ALGOLIA_ADMIN_API_KEY = "865dca6455aab8c44b0cc47a1c438c57"
ALGOLIA_INDEX_NAME = "restaurants"

# Firebase 設定（使用HTTP API，不需要認證檔案）
FIREBASE_PROJECT_ID = "java2025-91d74"

def get_restaurant_from_firebase(restaurant_name):
    """從Firebase獲取餐廳資料"""
    try:
        # 使用Firebase REST API搜尋餐廳
        url = f"https://firestore.googleapis.com/v1/projects/{FIREBASE_PROJECT_ID}/databases/(default)/documents/restaurants"
        
        # 分頁獲取所有餐廳資料
        all_docs = []
        page_token = None
        max_pages = 10  # 限制最大頁數避免無限循環
        
        for page in range(max_pages):
            params = {'pageSize': 100}
            if page_token:
                params['pageToken'] = page_token
                
            response = requests.get(url, params=params)
            
            if response.status_code != 200:
                print(f"Firebase API錯誤：{response.status_code}")
                break
                
            data = response.json()
            
            if 'documents' in data:
                all_docs.extend(data['documents'])
                
            # 檢查是否還有下一頁
            if 'nextPageToken' not in data:
                break
            page_token = data['nextPageToken']
        
        if not all_docs:
            print("Firebase中沒有找到任何餐廳資料")
            return None
            
        print(f"從Firebase獲取了 {len(all_docs)} 家餐廳資料")
        
        # 搜尋匹配的餐廳 - 使用更寬鬆的匹配邏輯
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
                restaurant_name.replace('-', '').replace('－', '') in name.replace('-', '').replace('－', '')
            ]
            
            if any(matches):
                # 提取餐廳資料
                restaurant_data = {
                    'objectID': doc['name'].split('/')[-1],  # 文檔ID
                    'name': fields.get('name', {}).get('stringValue', ''),
                    'address': fields.get('address', {}).get('stringValue', ''),
                    'formatted_address': fields.get('address', {}).get('stringValue', ''),
                    'id': fields.get('id', {}).get('stringValue', ''),
                    'business_status': 'OPERATIONAL',
                    'types': ['restaurant', 'food', 'point_of_interest', 'establishment']
                }
                
                print(f"在Firebase中找到餐廳：{restaurant_data['name']}")
                print(f"地址：{restaurant_data['address']}")
                print(f"文檔ID：{restaurant_data['objectID']}")
                
                return restaurant_data
                
        print(f"在Firebase中找不到餐廳：{restaurant_name}")
        print("嘗試的匹配方式包括：完全匹配、部分匹配、忽略空格和標點符號")
        return None
        
    except Exception as e:
        print(f"從Firebase獲取資料時發生錯誤：{e}")
        return None

def add_restaurant_to_algolia(restaurant_data):
    """將餐廳資料添加到Algolia"""
    try:
        # 初始化Algolia客戶端
        client = algoliasearch.client.Client(ALGOLIA_APP_ID, ALGOLIA_ADMIN_API_KEY)
        index = client.init_index(ALGOLIA_INDEX_NAME)
        
        print(f"正在添加餐廳到Algolia：{restaurant_data['name']}")
        
        # 上傳到Algolia
        response = index.save_object(restaurant_data)
        
        print(f"成功添加到Algolia！")
        print(f"ObjectID: {restaurant_data['objectID']}")
        
        # 等待索引更新
        index.wait_task(response['taskID'])
        print("索引更新完成！")
        
        return True
        
    except Exception as e:
        print(f"添加到Algolia時發生錯誤：{e}")
        return False

def test_algolia_search(restaurant_name):
    """測試Algolia搜尋功能"""
    try:
        client = algoliasearch.client.Client(ALGOLIA_APP_ID, ALGOLIA_ADMIN_API_KEY)
        index = client.init_index(ALGOLIA_INDEX_NAME)
        
        print(f"正在測試搜尋：{restaurant_name}")
        
        # 測試搜尋
        search_results = index.search(restaurant_name)
        
        print(f"搜尋結果數量：{search_results['nbHits']}")
        
        if search_results['hits']:
            for hit in search_results['hits']:
                print(f"找到餐廳：{hit['name']}")
                print(f"地址：{hit.get('formatted_address', hit.get('address', 'Unknown'))}")
            return True
        else:
            print("沒有找到搜尋結果")
            return False
            
    except Exception as e:
        print(f"測試搜尋時發生錯誤：{e}")
        return False

def auto_sync_restaurant(restaurant_name):
    """自動同步餐廳流程"""
    print(f"=== 自動同步餐廳：{restaurant_name} ===")
    
    # 1. 從Firebase獲取餐廳資料
    restaurant_data = get_restaurant_from_firebase(restaurant_name)
    
    if not restaurant_data:
        print("無法從Firebase獲取餐廳資料，同步失敗")
        return False
    
    # 2. 添加到Algolia
    success = add_restaurant_to_algolia(restaurant_data)
    
    if not success:
        print("添加到Algolia失敗")
        return False
    
    # 3. 測試搜尋
    search_success = test_algolia_search(restaurant_name)
    
    if search_success:
        print(f"✅ 同步成功！現在可以搜尋到「{restaurant_name}」了")
        return True
    else:
        print(f"❌ 同步可能有問題，搜尋測試失敗")
        return False

if __name__ == "__main__":
    restaurant_name = "好豆味冰沙豆花"
    if len(sys.argv) > 1:
        restaurant_name = ' '.join(sys.argv[1:])
    
    auto_sync_restaurant(restaurant_name) 