#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
快速添加餐廳到Algolia
用於在收集餐廳資料後立即同步到搜尋引擎
"""

import algoliasearch
import sys

# Algolia 設定
ALGOLIA_APP_ID = "V269PWJYC3"
ALGOLIA_ADMIN_API_KEY = "865dca6455aab8c44b0cc47a1c438c57"
ALGOLIA_INDEX_NAME = "restaurants"

def quick_add_restaurant(restaurant_name, restaurant_address=None, restaurant_id=None):
    """快速添加餐廳到Algolia"""
    try:
        # 初始化Algolia客戶端
        client = algoliasearch.client.Client(ALGOLIA_APP_ID, ALGOLIA_ADMIN_API_KEY)
        index = client.init_index(ALGOLIA_INDEX_NAME)
        
        # 生成ObjectID（如果沒有提供ID）
        object_id = restaurant_id if restaurant_id else restaurant_name.replace(' ', '_')
        
        # 餐廳資料
        restaurant_data = {
            "objectID": object_id,
            "name": restaurant_name,
            "formatted_address": restaurant_address or "地址未知",
            "address": restaurant_address or "地址未知",
            "id": restaurant_id or object_id,
            "business_status": "OPERATIONAL",
            "types": ["restaurant", "food", "point_of_interest", "establishment"]
        }
        
        print(f"正在添加餐廳到Algolia：{restaurant_name}")
        if restaurant_address:
            print(f"地址：{restaurant_address}")
        
        # 上傳到Algolia
        response = index.save_object(restaurant_data)
        
        print(f"✅ 成功添加到Algolia！")
        print(f"ObjectID: {object_id}")
        
        # 等待索引更新
        index.wait_task(response['taskID'])
        print("索引更新完成！")
        
        # 測試搜尋
        search_results = index.search(restaurant_name)
        if search_results['nbHits'] > 0:
            print(f"🎉 搜尋測試成功！現在可以搜尋到「{restaurant_name}」了")
            return True
        else:
            print("⚠️ 搜尋測試失敗，但餐廳已添加")
            return True
        
    except Exception as e:
        print(f"❌ 添加到Algolia時發生錯誤：{e}")
        return False

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("使用方式：python quick_add_algolia.py <餐廳名稱> [地址] [ID]")
        sys.exit(1)
    
    restaurant_name = sys.argv[1]
    restaurant_address = sys.argv[2] if len(sys.argv) > 2 else None
    restaurant_id = sys.argv[3] if len(sys.argv) > 3 else None
    
    success = quick_add_restaurant(restaurant_name, restaurant_address, restaurant_id)
    
    if success:
        print(f"\n🎯 建議：現在可以重新搜尋「{restaurant_name}」")
    else:
        print(f"\n❌ 同步失敗，請檢查錯誤訊息") 