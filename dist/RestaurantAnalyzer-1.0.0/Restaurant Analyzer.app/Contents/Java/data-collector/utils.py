from concurrent.futures import ThreadPoolExecutor as tpe
import json
import re
import time

from bs4 import BeautifulSoup as bs
import math
import requests

from config import headers1, project_id
from restaurant import Restaurant

def get_all_restaurants_id_on_firebase():
    '''
    從 Firebase 資料庫中獲取所有餐廳的ID
    
    Returns:
        set: 包含所有餐廳ID的集合
    
    此函數會：
    1. 連接到 Firebase 資料庫
    2. 使用分頁方式獲取所有餐廳文檔
    3. 從每個文檔中提取餐廳ID
    4. 將所有ID收集到一個集合中
    '''
    try:
        base_url = f"https://firestore.googleapis.com/v1/projects/{project_id}/databases/(default)/documents/restaurants"
        next_page_token = None
        ids = set()

        while True:
            url = base_url
            if next_page_token:
                url += f"?pageToken={next_page_token}"

            response = requests.get(url)

            if response.status_code == 200:
                data = response.json()
                restaurants = data.get("documents", [])
                for restaurant in restaurants:
                    id = restaurant["fields"].get("id", {}).get("stringValue", "未知")
                    ids.add(id)
                next_page_token = data.get("nextPageToken")
                if not next_page_token:
                    break
            else:
                print(f"讀取餐廳失敗，HTTP 狀態碼：{response.status_code}")
                break
        return ids


    except Exception as e:
        print(f"發生錯誤：{e}")

def save_restaurants_to_json(restaurants: list[Restaurant], file_path="restaurants.json"):
    '''
    將餐廳資料儲存為JSON檔案
    
    Args:
        restaurants (list[Restaurant]): 要儲存的餐廳列表
        file_path (str, optional): JSON檔案的儲存路徑。預設為 "restaurants.json"
    
    將每個餐廳的資料（包含基本資訊和評論）轉換為JSON格式並儲存到指定檔案
    '''
    data = []
    for restaurant in restaurants:
        data.append({
            "id": restaurant.id,
            "name": restaurant.name,
            "address": restaurant.address,
            "is_upload": restaurant.is_upload,
            "reviews": [
                {
                    "reviewer_name": review.reviewer_name,
                    "reviewer_state": review.reviewer_state,
                    "reviewer_id": review.reviewer_id,
                    "reviewer_total_reviews": review.reviewer_total_reviews,
                    "reviewer_total_photos": review.reviewer_total_photos,
                    "star_rating": review.star_rating,
                    "comment": review.comment,
                    "photo_url": review.photo_url,
                    "service_type": review.service_type,
                    "meal_type": review.meal_type,
                    "spend": review.spend,
                    "food_score": review.food_score,
                    "service_score": review.service_score,
                    "atmosphere_score": review.atmosphere_score,
                    "comment_date": review.comment_date
                } for review in restaurant.reviews
            ]
        })

    with open(file_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=4)

    print(f"餐廳資料已儲存至 {file_path}")

def search_restaurants_id_in_radius(lat, lon, radius=125):
    '''
    在指定座標的圓形範圍內搜尋餐廳ID
    
    Args:
        lat (float): 緯度
        lon (float): 經度
        radius (int, optional): 搜尋半徑（公尺）。預設為125公尺
    
    Returns:
        set: 包含所有找到的餐廳ID的集合
    
    會搜尋不同類型的場所，包括：
    - 餐廳 (Restaurants)
    - 酒吧 (Bars)
    - 咖啡店 (Coffee)
    - 外帶店 (Takeout)
    - 外送服務 (Delivery)
    '''
    restaurant_ids = set()
    types = ['Restaurants', 'Bars', 'Coffee', 'Takeout', 'Delivery']
    search_url = "https://www.google.com.tw/maps/search/{type}/@{lat},{lon},{radius}m/data=!3m1!1e3!4m2!2m1!6e5?entry=ttu&g_ep=EgoyMDI1MDUxMy4xIKXMDSoASAFQAw%3D%3D"

    for t in types:
        url = search_url.format(type=t, lat=lat, lon=lon, radius=radius)
        retries = 3
        for attempt in range(retries):
            try:
                response = requests.get(url, headers=headers1, timeout=10)
                response.raise_for_status()
                soup = bs(response.text, "html.parser")
                pattern = r'0x.{16}:0x.{16}'
                store_id_list = set(re.findall(pattern, str(soup)))
                restaurant_ids.update([store_id.replace('\\', '') for store_id in store_id_list])
                break
            except (requests.RequestException, re.error) as e:
                if attempt < retries - 1:
                    time.sleep(2 ** attempt)
                else:
                    print(f"Failed to fetch data for type {t} after {retries} attempts: {e}")

    return restaurant_ids

def search_restaurants_id_in_area(center_lat, center_lon, search_radius, grid_radius=50):
    '''
    在大範圍區域內搜尋餐廳ID，使用網格搜尋方式
    
    Args:
        center_lat (float): 中心點緯度
        center_lon (float): 中心點經度
        search_radius (float): 整體搜尋半徑（公尺）
        grid_radius (int, optional): 每個網格的半徑（公尺）。預設為50公尺
    
    Returns:
        set: 包含所有找到的新餐廳ID的集合（排除已存在於Firebase的ID）
    
    使用多執行緒方式在網格點上進行搜尋，提高搜尋效率
    '''
    existing_ids = get_all_restaurants_id_on_firebase()

    restaurant_ids = set()

    lat_increment = grid_radius / 111000
    lon_increment = grid_radius / (111000 * math.cos(math.radians(center_lat)))

    num_steps = math.ceil(search_radius / grid_radius)

    total_points = (2 * num_steps + 1) ** 2
    print(f"總共有 {total_points} 個點要搜尋")

    def fetch_ids(i, j):
        grid_lat = center_lat + i * lat_increment
        grid_lon = center_lon + j * lon_increment
        return search_restaurants_id_in_radius(grid_lat, grid_lon, grid_radius)

    with tpe() as executor:
        futures = [executor.submit(fetch_ids, i, j) for i in range(-num_steps, num_steps + 1) for j in range(-num_steps, num_steps + 1)]
        for idx, future in enumerate(futures, start=1):
            new_ids = future.result()
            restaurant_ids.update(new_ids - existing_ids)
            print(f"已搜尋 {idx}/{total_points} 個點")

    return restaurant_ids

def get_restaurant_info_by_id(restaurant_id):
    '''
    根據餐廳ID獲取餐廳的名稱和地址
    
    Args:
        restaurant_id (str): 餐廳的唯一識別碼
    
    Returns:
        tuple: (餐廳名稱, 餐廳地址)
        如果獲取失敗，則返回 ("Unknown", "Unknown")
    
    重試機制：
    - 最多重試3次
    - 每次重試的間隔時間以指數增加
    '''
    restaurant_name_url = "https://www.google.com.tw/maps/place/data=!4m5!3m4!1s{restaurant_id}!8m2!3d25.0564743!4d121.5204167?authuser=0&hl=zh-TW&rclk=1"
    url = restaurant_name_url.format(restaurant_id=restaurant_id)
    retries = 3
    for attempt in range(retries):
        try:
            response = requests.get(url, headers=headers1, timeout=15)
            if response.status_code == 400:
                raise requests.RequestException(f"get_restaurant_info_by_id false HTTP 400 Bad Request {url}")
            response.raise_for_status()
            soup = bs(response.text, "html.parser")
            meta_list = soup.find_all('meta')
            restaurant_name = []
            restaurant_address = []
            for i in meta_list:
                if '''itemprop="name"''' in str(i):
                    name_match = re.search('".*·', str(i))
                    address_match = re.search('·.*" ', str(i))
                    if name_match:
                        restaurant_name.append(name_match.group()[1:-2])
                    if address_match:
                        restaurant_address.append(address_match.group()[2:-2])
                    
            name = restaurant_name[0] if restaurant_name else "Unknown"
            address = restaurant_address[0] if restaurant_address else "Unknown"
            return name, address
        except (requests.RequestException, re.error) as e:
            if attempt < retries - 1:
                time.sleep(2 ** attempt)
            else:
                print(f"Failed to fetch name and address for restaurant ID {restaurant_id} after {retries} attempts: {e}")
                return "Unknown", "Unknown"

def get_restaurant_id_by_name(restaurant_name):
    '''
    根據餐廳名稱獲取餐廳ID
    
    Args:
        restaurant_name (str): 餐廳名稱，必須與Google地圖搜尋結果完全一致
            例如：隱家拉麵 士林店
    
    Returns:
        str: 餐廳的唯一識別碼，如果找不到則返回 None
    
    注意：餐廳名稱必須完全匹配Google地圖上的名稱
    '''
    try:
        store_id_url = "https://www.google.com.tw/maps/search/{restaurant_name}"
        url = store_id_url.format(restaurant_name=restaurant_name)
        response = requests.get(url, headers=headers1, timeout=10)
        response.raise_for_status()
        soup = bs(response.text, "html.parser")
        pattern = r'0x.{16}:0x.{16}'
        match = re.search(pattern, str(soup))
        
        if match:
            store_id = match.group()
            # 清理 ID，移除可能的多餘字符
            store_id = store_id.replace('\\', '').replace('"', '').strip()
            return store_id
        else:
            print(f"找不到餐廳 ID，搜尋關鍵字：{restaurant_name}")
            return None
    except Exception as e:
        print(f"搜尋餐廳 ID 時發生錯誤：{e}")
        return None

def get_restaurants_in_area(center_lat, center_lon, search_radius):
    '''
    在指定區域內搜尋並獲取所有餐廳的完整資訊
    
    Args:
        center_lat (float): 中心點緯度
        center_lon (float): 中心點經度
        search_radius (float): 搜尋半徑（公尺）
    
    Returns:
        list[Restaurant]: 包含所有找到的餐廳物件的列表
    
    流程：
    1. 先使用網格搜尋獲取區域內所有餐廳ID
    2. 使用多執行緒方式並行獲取每個餐廳的詳細資訊
    3. 建立Restaurant物件並收集到列表中
    '''
    restaurant_ids = search_restaurants_id_in_area(center_lat, center_lon, search_radius)
    restaurants: list[Restaurant] = []

    def fetch_name(restaurant_id):
        retries = 3
        for attempt in range(retries):
            try:
                return get_restaurant_info_by_id(restaurant_id)
            except Exception as e:
                if attempt < retries - 1:
                    time.sleep(2 ** attempt)
                else:
                    print(f"Failed to fetch name for restaurant ID {restaurant_id} after {retries} attempts: {e}")
                    return "Unknown", "Unknown"

    with tpe() as executor:
        futures = {executor.submit(fetch_name, restaurant_id): restaurant_id for restaurant_id in restaurant_ids}
        for idx, (future, restaurant_id) in enumerate(futures.items(), start=1):
            name, address = future.result()
            if name == "Unknown" or address == "Unknown":
                print(f"無法獲取餐廳名稱或地址，ID: {restaurant_id}")
                continue
            restaurants.append(Restaurant(restaurant_id, name, address))
            print(f"已獲取 {idx}/{len(restaurant_ids)} 家餐廳的名稱和地址")

    return restaurants

def main():
    pass

if __name__ == "__main__":
    main()