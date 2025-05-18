from concurrent.futures import ThreadPoolExecutor as tpe
from datetime import datetime
import json
import re
import time

from bs4 import BeautifulSoup as bs
import emoji
import math
import requests


center_lat = 25.1494729
center_lon = 121.7641153
search_radius = 10
project_id = "java2025-91d74"
headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                       "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36"
        }

class Restaurant:
    def __init__(self, id, name, address):
        self.id = id
        self.name = name
        self.address = address
        self.reviews: list[Review] = []
        self.comment_url = "https://www.google.com.tw/maps/rpc/listugcposts"

    def get_reviews(self, page_count=2000, sorted_by=2):
        '''
        sorted_by 參數對應：
        1 - 最相關 (Most Relevant)
        2 - 最新 (Newest)
        3 - 評分最高 (Highest Rating)
        4 - 評分最低 (Lowest Rating)
        
        每個 page 會有10筆資料，除非評論數未達10筆

        '''
        next_token = ""
        comment_list = []
        for page in range(1, page_count+1):
            #print(f"第 {page} 頁開始抓取")
            
            params = {
                "authuser": "0",
                "hl": "zh-TW",
                "gl": "tw",
                "pb": (
                    f"!1m6!1s{self.id}!6m4!4m1!1e1!4m1!1e3!2m2!1i10!2s"
                    f"{next_token}"
                    f"!5m2!1s0OBwZ4OnGsrM1e8PxIjW6AI!7e81!8m5!1b1!2b1!3b1!5b1!7b1!11m0!13m1!1e{sorted_by}"
                )
            }

            response = requests.get(self.comment_url, params=params, headers=headers)
            data = json.loads(emoji.demojize(response.text[4:]))
            #print(f"第 {page} 抓取結束")
            try:
                next_token = data[1]
            except IndexError:
                print(f"Unexpected data structure: {data} {self.id} {page} {self.name}")
                break
            comment_list.extend(data[2])
            if not next_token:
                #print(f"所有評論以抓取完成，總共抓取 {len(comment_list)} 則評論")
                break
            time.sleep(0.1)

        # 提取需要的資料
        for comment_data in comment_list:
            try:
                comment_date = comment_data[0][2][2][0][1][21][6][-1]
                comment_date = datetime(comment_date[0], comment_date[1], comment_date[2], comment_date[3]).strftime('%Y/%m/%d %H:%M:%S')
            except:
                comment_date = None

            try:
                comment_text = comment_data[0][2][-1][0][0]
            except:
                comment_text = None

            try:
                service_type = comment_data[0][2][6][0][2][0][0][0][0]
            except:
                service_type = None

            try:
                meal_type = comment_data[0][2][6][1][2][0][0][0][0]
            except:
                meal_type = None

            try:
                spend = comment_data[0][2][6][2][2][0][0][0][0]
            except:
                spend = None

            try:
                food_score = comment_data[0][2][6][3][11][0]
            except:
                food_score = None
            try:
                service_score = comment_data[0][2][6][4][11][0]
            except:
                service_score = None
            try:
                atmosphere_score = comment_data[0][2][6][5][11][0]
            except:
                atmosphere_score = None

            try:
                photo_url = comment_data[0][2][2][0][1][6][0]
            except:
                photo_url = None

            comment_info = {
                "評論者": comment_data[0][1][4][5][0],
                "服務內容": service_type,
                "餐點類型": meal_type,
                "平均每人消費": spend,
                "餐點": food_score,
                "服務": service_score,
                "氣氛": atmosphere_score,
                "照片數": comment_data[0][1][4][5][6],
                "照片": photo_url,
                "評論數": comment_data[0][1][4][5][5],
                "評論者id": comment_data[0][0],
                "評論者狀態": comment_data[0][1][4][5][10][0],
                "留言時間": comment_data[0][1][6],
                "留言日期": comment_date,
                "評論": comment_text,
                "評論分數": comment_data[0][2][0][0]

            }
            self.reviews.append(Review(
                reviewer_id=comment_info["評論者id"],
                review_id=comment_info["評論者id"],
                comment=comment_info["評論"],
                star_rating=comment_info["評論分數"],
                comment_date=comment_info["留言日期"],
                photo_url=comment_info["照片"],
                service_type=comment_info["服務內容"],
                meal_type=comment_info["餐點類型"],
                spend=comment_info["平均每人消費"],
                food_score=comment_info["餐點"],
                service_score=comment_info["服務"],
                atmosphere_score=comment_info["氣氛"]
            ))
        return

    def upload_to_firestore(self):
        retries = 7
        for attempt in range(retries):
            try:
                self.upload_res()
                time.sleep(0.5)
                self.upload_review()
                break
            except Exception as e:
                if attempt < retries - 1:
                    print(f"重試中，嘗試次數：{attempt + 1}/{retries}")
                    time.sleep(2 ** attempt)
                else:
                    print(f"上傳失敗：{e}")

    def upload_res(self):
        retries = 7
        for attempt in range(retries):
            try:
                url = f"https://firestore.googleapis.com/v1/projects/{project_id}/databases/(default)/documents/restaurants?documentId={self.id}"

                headers = {
                    "Content-Type": "application/json; charset=UTF-8"
                }

                json_data = {
                    "fields": {
                        "name": {"stringValue": self.name},
                        "address": {"stringValue": self.address},
                        "id": {"stringValue": self.id}
                    }
                }

                response = requests.post(url, headers=headers, json=json_data, timeout=10)
                if response.status_code == 400:
                    raise requests.RequestException("HTTP 400 Bad Request")
                response.raise_for_status()

                print(f"新增餐廳 HTTP 狀態碼：{response.status_code}")
                break
            except Exception as e:
                if attempt < retries - 1:
                    time.sleep(2 ** attempt)
                else:
                    print(f"發生錯誤：{e}")

    def upload_review(self):
        try:
            url = f"https://firestore.googleapis.com/v1/projects/{project_id}/databases/(default)/documents:commit"

            headers = {
                "Content-Type": "application/json; charset=UTF-8"
            }

            def ensure_string(value):
                if value is None:
                    return {"nullValue": None}
                return {"stringValue": str(value)}

            writes = []
            for review in self.reviews:
                writes.append({
                    "update": {
                        "name": f"projects/{project_id}/databases/(default)/documents/restaurants/{self.id}/reviews/{review.review_id}",
                        "fields": {
                            "reviewer_id": ensure_string(review.reviewer_id),
                            "review_id": ensure_string(review.review_id),
                            "comment": ensure_string(review.comment),
                            "star_rating": ensure_string(review.star_rating),
                            "comment_date": ensure_string(review.comment_date),
                            "photo_url": ensure_string(review.photo_url),
                            "service_type": ensure_string(review.service_type),
                            "meal_type": ensure_string(review.meal_type),
                            "spend": ensure_string(review.spend),
                            "food_score": ensure_string(review.food_score),
                            "service_score": ensure_string(review.service_score),
                            "atmosphere_score": ensure_string(review.atmosphere_score)
                        }
                    }
                })

            json_data = {"writes": writes}

            response = requests.post(url, headers=headers, json=json_data, timeout=10)
            response.raise_for_status()

            print(f"更新評論 HTTP 狀態碼：{response.status_code}")
        except Exception as e:
            print(f"發生錯誤：{e}")


class Review:
    def __init__(self, reviewer_id, review_id, comment, star_rating, comment_date, photo_url=None, service_type=None, meal_type=None, spend=None, food_score=None, service_score=None, atmosphere_score=None):
        self.reviewer_id = reviewer_id
        self.review_id = review_id
        self.comment = comment
        self.star_rating = star_rating
        self.comment_date = comment_date
        self.photo_url = photo_url
        self.service_type = service_type
        self.meal_type = meal_type
        self.spend = spend
        self.food_score = food_score
        self.service_score = service_score
        self.atmosphere_score = atmosphere_score


def search_restaurants_id_in_radius(lat, lon, radius=50):
    """
    使用Google Maps API搜尋特定範圍內的餐廳ID
    """
    url = "https://www.google.com.tw/maps/search/@" + str(lat) + "," + str(lon) + ",15z/data=!3m1!4b1!4m2!2m1!6e5"
    response = requests.get(url, headers=headers)
    
    # 使用正則表達式提取所有商店ID
    pattern = r'null,null,null,\["(\w+)",null'
    store_ids = set(re.findall(pattern, response.text))
    
    # 使用另一個模式尋找其他可能的ID
    pattern2 = r'\["(\w+)","\d+'
    store_ids.update(re.findall(pattern2, response.text))
    
    return list(store_ids)


def search_restaurants_id_in_area(center_lat, center_lon, search_radius, grid_radius=50):
    """
    使用網格方式在較大區域內搜尋餐廳ID
    """
    km_per_degree_lat = 111.32  # 每緯度大約距離(公里)
    km_per_degree_lon = 110.57 * math.cos(math.radians(center_lat))  # 在當前緯度下每經度大約距離(公里)
    
    lat_range = search_radius / km_per_degree_lat
    lon_range = search_radius / km_per_degree_lon
    
    grid_count = int(search_radius / grid_radius) or 1
    
    all_restaurant_ids = set()

    def fetch_ids(i, j):
        grid_lat = center_lat + (i / grid_count) * lat_range
        grid_lon = center_lon + (j / grid_count) * lon_range
        restaurant_ids = search_restaurants_id_in_radius(grid_lat, grid_lon, grid_radius)
    return restaurant_ids
    
    with tpe(max_workers=4) as executor:
        tasks = []
        for i in range(-grid_count, grid_count + 1):
            for j in range(-grid_count, grid_count + 1):
                tasks.append(executor.submit(fetch_ids, i, j))
        
        for task in tasks:
            all_restaurant_ids.update(task.result())
    
    return list(all_restaurant_ids)


def get_restaurant_info(restaurant_id):
    """
    根據餐廳ID獲取餐廳詳細資訊
    """
    url = f"https://www.google.com.tw/maps/place/?q=place_id:{restaurant_id}"
    response = requests.get(url, headers=headers)
    soup = bs(response.text, 'html.parser')
    
    # 提取餐廳名稱
    try:
        name_tag = soup.find('meta', {'property': 'og:title'})
        restaurant_name = name_tag['content'] if name_tag else None
    except:
        restaurant_name = None
    
    # 提取餐廳地址
    try:
        address_tag = soup.find('meta', {'property': 'og:description'})
        restaurant_address = address_tag['content'] if address_tag else None
    except:
        restaurant_address = None
    
    return {
        'id': restaurant_id,
        'name': restaurant_name,
        'address': restaurant_address
    }


def get_restaurants_in_area(center_lat, center_lon, search_radius):
    """
    獲取區域內所有餐廳的詳細資訊
    """
    restaurant_ids = search_restaurants_id_in_area(center_lat, center_lon, search_radius)

    def fetch_name(restaurant_id):
            try:
                return get_restaurant_info(restaurant_id)
            except Exception as e:
            print(f"獲取餐廳 {restaurant_id} 資訊時出錯: {e}")
            return None
    
    restaurants = []
    with tpe(max_workers=4) as executor:
        results = executor.map(fetch_name, restaurant_ids)
        
        for result in results:
            if result and result['name'] and result['address']:
                restaurant = Restaurant(
                    id=result['id'],
                    name=result['name'],
                    address=result['address']
                )
                restaurants.append(restaurant)

    return restaurants


def add_res_by_name():
    names = ["海大燒臘", "爾灣口味噴泉", "海那邊小食堂"]


def main():
    print("1. 搜尋區域內餐廳")
    print("2. 手動輸入餐廳名稱")
    
    choice = input("請選擇操作 (1/2): ")
    
    if choice == "1":
        lat = float(input("輸入中心點緯度 (預設: 25.1494729): ") or "25.1494729")
        lon = float(input("輸入中心點經度 (預設: 121.7641153): ") or "121.7641153")
        radius = float(input("輸入搜尋半徑 (公里) (預設: 5): ") or "5")
        
        print(f"正在搜尋以 ({lat}, {lon}) 為中心，半徑 {radius} 公里內的餐廳...")
        restaurants = get_restaurants_in_area(lat, lon, radius)
        
        print(f"找到 {len(restaurants)} 家餐廳:")
        for i, restaurant in enumerate(restaurants, 1):
            print(f"{i}. {restaurant.name} - {restaurant.address}")
        
        selected_indices = input("請選擇要分析的餐廳編號 (用逗號分隔，全選請輸入 'all'): ")
        
        if selected_indices.lower() == 'all':
            selected_restaurants = restaurants
        else:
            indices = [int(idx) - 1 for idx in selected_indices.split(',')]
            selected_restaurants = [restaurants[idx] for idx in indices if 0 <= idx < len(restaurants)]
        
    elif choice == "2":
        selected_restaurants = []
        while True:
            restaurant_name = input("輸入餐廳名稱 (輸入空白結束): ")
            if not restaurant_name:
                break
                
            restaurant_id = input("輸入餐廳 ID (可選): ")
            if not restaurant_id:
                print("搜尋餐廳ID...")
                # 執行搜尋邏輯
            
            restaurant_address = input("輸入餐廳地址 (可選): ")
            
            restaurant = Restaurant(
                id=restaurant_id or f"manual_{len(selected_restaurants)}",
                name=restaurant_name,
                address=restaurant_address or "手動輸入"
            )
            selected_restaurants.append(restaurant)
    
    # 開始分析
    if selected_restaurants:
        for restaurant in selected_restaurants:
            print(f"正在獲取 {restaurant.name} 的評論...")
            restaurant.get_reviews()
            
            filename = f"{restaurant.name}_reviews.json"
            with open(f"reviews_data/{filename}", 'w', encoding='utf-8') as f:
                reviews_data = []
                for review in restaurant.reviews:
                    reviews_data.append({
                        "reviewer_id": review.reviewer_id,
                        "review_id": review.review_id,
                        "comment": review.comment,
                        "star_rating": review.star_rating,
                        "comment_date": review.comment_date,
                        "photo_url": review.photo_url,
                        "service_type": review.service_type,
                        "meal_type": review.meal_type,
                        "spend": review.spend,
                        "food_score": review.food_score,
                        "service_score": review.service_score,
                        "atmosphere_score": review.atmosphere_score
                    })
                json.dump(reviews_data, f, ensure_ascii=False, indent=2)
            
            print(f"評論已保存到 {filename}")
            
            # 更新追蹤的餐廳列表
            try:
                with open('reviews_data/tracked_restaurants.json', 'r', encoding='utf-8') as f:
                    tracked_restaurants = json.load(f)
            except (FileNotFoundError, json.JSONDecodeError):
                tracked_restaurants = []
            
            # 檢查餐廳是否已經存在於列表中
            if not any(r.get('id') == restaurant.id for r in tracked_restaurants):
                tracked_restaurants.append({
                    'id': restaurant.id,
                    'name': restaurant.name,
                    'address': restaurant.address,
                    'last_updated': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                })
                
                with open('reviews_data/tracked_restaurants.json', 'w', encoding='utf-8') as f:
                    json.dump(tracked_restaurants, f, ensure_ascii=False, indent=2)
            
            print(f"是否上傳 {restaurant.name} 的資料到 Firestore?")
            upload_choice = input("輸入 'y' 確認上傳: ")
            if upload_choice.lower() == 'y':
                print(f"正在上傳 {restaurant.name} 的資料...")
                restaurant.upload_to_firestore()
                print("上傳完成!")
    
    print("程序完成!")


if __name__ == "__main__":
    main()
