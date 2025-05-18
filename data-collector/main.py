from concurrent.futures import ThreadPoolExecutor as tpe
import json
import re
import time
import os

from bs4 import BeautifulSoup as bs
import math
import requests

from config import headers1, center_lat, center_lon, search_radius
from restaurant import Restaurant

def save_restaurants_to_json(restaurants: list[Restaurant], file_path="restaurants.json"):
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

def search_restaurants_id_in_radius(lat, lon, radius=50):
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
            restaurant_ids.update(future.result())
            print(f"已搜尋 {idx}/{total_points} 個點")

    return restaurant_ids

def get_restaurant_info_by_id(restaurant_id):
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

def get_restaurant_info_by_name(restaurant_name):
    pass

def get_restaurants_in_area(center_lat, center_lon, search_radius):
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

def add_res_by_name(name):
    pass


def main():
    restaurants: list[Restaurant] = get_restaurants_in_area(center_lat=center_lat, center_lon=center_lon, search_radius=search_radius)
    print(f"找到 {len(restaurants)} 家餐廳")
    save_restaurants_to_json(restaurants)
    with tpe() as executor:
        futures = [executor.submit(restaurant.get_reviews) for restaurant in restaurants]
        for idx, future in enumerate(futures, start=1):
            future.result()
            print(f"第 {idx}/{len(restaurants)} 家餐廳的評論抓取完成。")

    print("評論抓取完成，開始上傳到 Firestore...")
    save_restaurants_to_json(restaurants)

    with tpe() as executor:
        futures = [executor.submit(restaurant.upload_to_firestore) for restaurant in restaurants]
        for idx, future in enumerate(futures, start=1):
            future.result()
            print(f"第 {idx}/{len(restaurants)} 家餐廳的評論上傳完成。")

    print("所有餐廳的評論上傳完成。")
    save_restaurants_to_json(restaurants)

if __name__ == "__main__":
    main()