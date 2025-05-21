#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Firebase 到 Algolia 資料同步工具

此程式將 Firebase Firestore 中的餐廳資料同步到 Algolia 搜索引擎，
便於前端應用程式實現高效的餐廳搜索功能。

作者：James Su
版本：1.0.0
日期：2023-05-15
"""

import firebase_admin
from firebase_admin import credentials, firestore
import algoliasearch
import os

# ---------- 全局常量 ----------
# 根據bigproject結構更新路徑
FIREBASE_CREDENTIALS_PATH = os.path.join(os.path.dirname(__file__), 
                                        "../src/main/resources/firebase/Firebase_Admin_SDK.json")
FIREBASE_COLLECTION = "restaurants"

# Algolia 設定
ALGOLIA_APP_ID = "V269PWJYC3"
ALGOLIA_ADMIN_API_KEY = "865dca6455aab8c44b0cc47a1c438c57"
ALGOLIA_INDEX_NAME = "restaurants"

def initialize_firebase():
    """初始化 Firebase 連接"""
    cred = credentials.Certificate(FIREBASE_CREDENTIALS_PATH)
    firebase_admin.initialize_app(cred)
    return firestore.client()

def initialize_algolia():
    """初始化 Algolia 連接"""
    client = algoliasearch.client.Client(ALGOLIA_APP_ID, ALGOLIA_ADMIN_API_KEY)
    return client.init_index(ALGOLIA_INDEX_NAME)

def sync_firestore_to_algolia(db, index):
    """
    從 Firebase Firestore 讀取餐廳資料，並上傳到 Algolia 搜索引擎
    
    Args:
        db: Firestore 資料庫客戶端
        index: Algolia 索引客戶端
        
    Returns:
        int: 成功上傳的記錄數量
    """
    # 獲取所有餐廳文檔
    docs = db.collection(FIREBASE_COLLECTION).stream()

    # 準備要上傳的資料
    algolia_objects = []

    # 處理每個餐廳文檔
    for doc in docs:
        data = doc.to_dict()
        data["objectID"] = doc.id  # 設置 Algolia 物件 ID
        algolia_objects.append(data)

    # 上傳資料到 Algolia
    if algolia_objects:
        index.save_objects(algolia_objects)
        print(f"成功上傳 {len(algolia_objects)} 筆餐廳資料到 Algolia！")
        return len(algolia_objects)
    else:
        print("沒有找到任何餐廳資料")
        return 0

def main():
    """主程式入口點"""
    print("開始同步 Firebase 餐廳資料到 Algolia...")
    
    try:
        # 初始化連接
        db = initialize_firebase()
        index = initialize_algolia()
        
        # 執行同步
        record_count = sync_firestore_to_algolia(db, index)
        
        print(f"同步完成！共處理 {record_count} 筆記錄")
    except Exception as e:
        print(f"同步過程中發生錯誤: {e}")
        return 1
    
    return 0

# ---------- 主程式 ----------
if __name__ == "__main__":
    exit_code = main()
    exit(exit_code) 