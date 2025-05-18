#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import json
import time
import datetime
import argparse
import requests
from pathlib import Path

class ReviewTracker:
    """Google Maps 餐廳評論追蹤器"""
    
    def __init__(self, api_key=None):
        """初始化追蹤器
        
        Args:
            api_key: Google Maps API 金鑰，如果未提供會嘗試從環境變數獲取
        """
        # 嘗試從環境變數獲取 API 金鑰
        self.api_key = api_key or os.environ.get("GOOGLE_MAPS_API_KEY")
        if not self.api_key:
            print("警告: 未提供 Google Maps API 金鑰，請設置 GOOGLE_MAPS_API_KEY 環境變數或直接傳入")
        
        # 設置基本檔案路徑
        self.data_dir = Path("reviews_data")
        self.data_dir.mkdir(exist_ok=True)
        
        # 餐廳資訊的預設檔案
        self.restaurants_file = self.data_dir / "tracked_restaurants.json"
        
        # 如果檔案不存在，建立基本結構
        if not self.restaurants_file.exists():
            self._initialize_restaurants_file()
    
    def _initialize_restaurants_file(self):
        """初始化追蹤餐廳的檔案"""
        initial_data = {
            "restaurants": [
                {
                    "name": "海大燒臘",
                    "place_id": "",  # 需要通過搜尋 API 獲取
                    "address": "基隆市中正區北寧路2號",
                    "last_update": "",
                    "review_count": 0
                }
            ]
        }
        
        with open(self.restaurants_file, "w", encoding="utf-8") as f:
            json.dump(initial_data, f, ensure_ascii=False, indent=2)
        
        print(f"已建立追蹤餐廳檔案: {self.restaurants_file}")
    
    def find_place_id(self, restaurant_name, address=None):
        """使用 Google Maps API 尋找餐廳的 place_id
        
        Args:
            restaurant_name: 餐廳名稱
            address: 餐廳地址 (可選)
            
        Returns:
            place_id 字串或 None
        """
        if not self.api_key:
            print("錯誤: 需要 API 金鑰才能查詢 place_id")
            return None
            
        # 構建查詢文字
        query = restaurant_name
        if address:
            query = f"{restaurant_name} {address}"
            
        # 使用 Find Place API 查詢
        url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json"
        params = {
            "input": query,
            "inputtype": "textquery",
            "fields": "place_id,name,formatted_address,geometry",
            "language": "zh-TW",
            "key": self.api_key
        }
        
        try:
            response = requests.get(url, params=params)
            data = response.json()
            
            if data["status"] == "OK" and data["candidates"]:
                place_id = data["candidates"][0]["place_id"]
                print(f"已找到 {restaurant_name} 的 place_id: {place_id}")
                return place_id
            else:
                print(f"無法找到 {restaurant_name} 的 place_id. 狀態: {data['status']}")
                return None
                
        except Exception as e:
            print(f"查詢 place_id 時發生錯誤: {e}")
            return None
    
    def get_restaurant_reviews(self, place_id, max_reviews=20):
        """獲取餐廳的評論
        
        Args:
            place_id: Google Maps 餐廳的 place_id
            max_reviews: 要獲取的最大評論數量
            
        Returns:
            評論清單或 None
        """
        if not self.api_key:
            print("錯誤: 需要 API 金鑰才能獲取評論")
            return None
            
        url = "https://maps.googleapis.com/maps/api/place/details/json"
        params = {
            "place_id": place_id,
            "fields": "name,rating,reviews,user_ratings_total",
            "language": "zh-TW",
            "reviews_sort": "newest",  # 取得最新評論
            "key": self.api_key
        }
        
        try:
            response = requests.get(url, params=params)
            data = response.json()
            
            if data["status"] == "OK":
                result = data["result"]
                reviews = result.get("reviews", [])
                total_ratings = result.get("user_ratings_total", 0)
                rating = result.get("rating", 0)
                
                print(f"餐廳: {result['name']}")
                print(f"總評分數: {total_ratings}")
                print(f"平均評分: {rating}")
                print(f"已獲取 {len(reviews)} 則評論")
                
                return {
                    "name": result["name"],
                    "total_ratings": total_ratings,
                    "rating": rating,
                    "reviews": reviews[:max_reviews]
                }
            else:
                print(f"獲取評論失敗. 狀態: {data['status']}")
                return None
                
        except Exception as e:
            print(f"獲取評論時發生錯誤: {e}")
            return None
    
    def update_restaurant_data(self, restaurant_name=None):
        """更新指定餐廳的評論資料
        
        Args:
            restaurant_name: 餐廳名稱，如果為 None 則更新所有追蹤的餐廳
            
        Returns:
            新評論的數量
        """
        if not self.api_key:
            print("錯誤: 需要 API 金鑰才能更新資料")
            return 0
            
        # 載入追蹤的餐廳
        tracked_data = self._load_tracked_restaurants()
        
        # 過濾要更新的餐廳
        restaurants_to_update = []
        if restaurant_name:
            restaurants_to_update = [r for r in tracked_data["restaurants"] if r["name"] == restaurant_name]
            if not restaurants_to_update:
                print(f"未找到名為 '{restaurant_name}' 的追蹤餐廳")
                return 0
        else:
            restaurants_to_update = tracked_data["restaurants"]
        
        total_new_reviews = 0
        
        for restaurant in restaurants_to_update:
            name = restaurant["name"]
            place_id = restaurant["place_id"]
            
            # 如果沒有 place_id，嘗試查詢
            if not place_id:
                place_id = self.find_place_id(name, restaurant.get("address"))
                if place_id:
                    restaurant["place_id"] = place_id
                else:
                    print(f"無法更新 {name} 的資料: 缺少 place_id")
                    continue
            
            # 獲取最新評論
            data = self.get_restaurant_reviews(place_id, max_reviews=50)
            if not data:
                continue
                
            # 檢查以前的評論資料
            restaurant_data_file = self.data_dir / f"{name.replace(' ', '_')}_reviews.json"
            previous_data = None
            new_reviews = []
            
            if restaurant_data_file.exists():
                try:
                    with open(restaurant_data_file, "r", encoding="utf-8") as f:
                        previous_data = json.load(f)
                    
                    # 尋找新評論
                    previous_review_ids = set()
                    if "reviews" in previous_data:
                        for review in previous_data["reviews"]:
                            if "time" in review and "author_name" in review:
                                review_id = f"{review['author_name']}_{review['time']}"
                                previous_review_ids.add(review_id)
                    
                    for review in data["reviews"]:
                        review_id = f"{review['author_name']}_{review['time']}"
                        if review_id not in previous_review_ids:
                            new_reviews.append(review)
                    
                    print(f"找到 {len(new_reviews)} 則新評論")
                    total_new_reviews += len(new_reviews)
                    
                    # 合併評論資料
                    if new_reviews:
                        if "reviews" in previous_data:
                            all_reviews = new_reviews + previous_data["reviews"]
                            data["reviews"] = all_reviews[:100]  # 限制存儲的評論數
                        else:
                            data["reviews"] = new_reviews
                    
                except Exception as e:
                    print(f"讀取先前資料時發生錯誤: {e}")
            else:
                # 沒有先前資料，所有評論都是新的
                new_reviews = data["reviews"]
                total_new_reviews += len(new_reviews)
                print(f"首次獲取，{len(new_reviews)} 則新評論")
            
            # 更新檔案
            with open(restaurant_data_file, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            
            # 更新追蹤資料
            restaurant["last_update"] = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            restaurant["review_count"] = data["total_ratings"]
            
            # 顯示新評論
            if new_reviews:
                print("\n新評論:")
                for i, review in enumerate(new_reviews, 1):
                    print(f"\n--- 評論 {i} ---")
                    print(f"評分: {'⭐' * int(review['rating'])}")
                    print(f"時間: {datetime.datetime.fromtimestamp(review['time']).strftime('%Y-%m-%d')}")
                    print(f"作者: {review['author_name']}")
                    print(f"內容: {review['text']}")
        
        # 儲存更新的追蹤資料
        with open(self.restaurants_file, "w", encoding="utf-8") as f:
            json.dump(tracked_data, f, ensure_ascii=False, indent=2)
        
        return total_new_reviews
    
    def add_restaurant(self, name, address=None):
        """添加新餐廳到追蹤列表
        
        Args:
            name: 餐廳名稱
            address: 餐廳地址 (可選)
            
        Returns:
            布林值表示是否成功
        """
        tracked_data = self._load_tracked_restaurants()
        
        # 檢查是否已經追蹤
        for restaurant in tracked_data["restaurants"]:
            if restaurant["name"] == name:
                print(f"已經在追蹤 '{name}'")
                return False
        
        # 獲取 place_id
        place_id = self.find_place_id(name, address)
        
        # 添加新餐廳
        new_restaurant = {
            "name": name,
            "place_id": place_id or "",
            "address": address or "",
            "last_update": "",
            "review_count": 0
        }
        
        tracked_data["restaurants"].append(new_restaurant)
        
        # 儲存更新的追蹤資料
        with open(self.restaurants_file, "w", encoding="utf-8") as f:
            json.dump(tracked_data, f, ensure_ascii=False, indent=2)
        
        print(f"已添加 '{name}' 到追蹤列表")
        return True
    
    def list_tracked_restaurants(self):
        """列出所有追蹤的餐廳"""
        tracked_data = self._load_tracked_restaurants()
        
        print("\n目前追蹤的餐廳:")
        print("=" * 50)
        
        for i, restaurant in enumerate(tracked_data["restaurants"], 1):
            print(f"{i}. {restaurant['name']}")
            print(f"   地址: {restaurant['address'] or '未指定'}")
            print(f"   Place ID: {restaurant['place_id'] or '未獲取'}")
            print(f"   最後更新: {restaurant['last_update'] or '從未'}")
            print(f"   評論數量: {restaurant['review_count']}")
            print("=" * 50)
    
    def _load_tracked_restaurants(self):
        """載入追蹤的餐廳資料"""
        if not self.restaurants_file.exists():
            self._initialize_restaurants_file()
            
        with open(self.restaurants_file, "r", encoding="utf-8") as f:
            return json.load(f)
    
    def export_reviews_for_analysis(self, restaurant_name, output_format="json"):
        """匯出指定餐廳的評論，以便分析
        
        Args:
            restaurant_name: 餐廳名稱
            output_format: 輸出格式 (目前只支援 json)
            
        Returns:
            輸出檔案的路徑或 None
        """
        restaurant_data_file = self.data_dir / f"{restaurant_name.replace(' ', '_')}_reviews.json"
        
        if not restaurant_data_file.exists():
            print(f"找不到 '{restaurant_name}' 的資料檔案")
            return None
            
        try:
            with open(restaurant_data_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                
            # 創建更適合分析的格式
            analysis_data = []
            for review in data.get("reviews", []):
                analysis_data.append({
                    "author": review.get("author_name", ""),
                    "rating": review.get("rating", 0),
                    "date": datetime.datetime.fromtimestamp(review.get("time", 0)).strftime("%Y-%m-%d"),
                    "comment": review.get("text", ""),
                    "language": review.get("language", "")
                })
            
            # 儲存為 JSON 檔案
            output_file = Path(f"{restaurant_name.replace(' ', '_')}_analysis.json")
            with open(output_file, "w", encoding="utf-8") as f:
                json.dump(analysis_data, f, ensure_ascii=False, indent=2)
                
            print(f"已匯出 {len(analysis_data)} 則評論到 {output_file}")
            return output_file
                
        except Exception as e:
            print(f"匯出評論時發生錯誤: {e}")
            return None


def main():
    """主函數"""
    parser = argparse.ArgumentParser(description="Google Maps 餐廳評論追蹤工具")
    parser.add_argument("-k", "--api-key", help="Google Maps API 金鑰")
    
    subparsers = parser.add_subparsers(dest="command", help="命令")
    
    # 新增餐廳命令
    add_parser = subparsers.add_parser("add", help="添加餐廳到追蹤列表")
    add_parser.add_argument("name", help="餐廳名稱")
    add_parser.add_argument("-a", "--address", help="餐廳地址")
    
    # 更新評論命令
    update_parser = subparsers.add_parser("update", help="更新評論資料")
    update_parser.add_argument("-r", "--restaurant", help="餐廳名稱 (若未指定則更新所有餐廳)")
    
    # 列出餐廳命令
    subparsers.add_parser("list", help="列出所有追蹤的餐廳")
    
    # 匯出評論命令
    export_parser = subparsers.add_parser("export", help="匯出評論以供分析")
    export_parser.add_argument("restaurant", help="餐廳名稱")
    
    args = parser.parse_args()
    
    # 建立追蹤器
    tracker = ReviewTracker(api_key=args.api_key)
    
    # 處理命令
    if args.command == "add":
        tracker.add_restaurant(args.name, args.address)
    elif args.command == "update":
        new_reviews = tracker.update_restaurant_data(args.restaurant)
        print(f"\n總共發現 {new_reviews} 則新評論")
    elif args.command == "list":
        tracker.list_tracked_restaurants()
    elif args.command == "export":
        tracker.export_reviews_for_analysis(args.restaurant)
    else:
        # 顯示使用說明
        parser.print_help()


if __name__ == "__main__":
    main() 