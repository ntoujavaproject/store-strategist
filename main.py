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

            writes = []
            for review in self.reviews:
                review_id = review.review_id
                document_path = f"projects/{project_id}/databases/(default)/documents/restaurants/{self.id}/reviews/{review_id}"

                def ensure_string(value):
                    return str(value) if value is not None else ""

                writes.append({
                    "update": {
                        "name": document_path,
                        "fields": {
                                "reviewer_id": {"stringValue": ensure_string(review.reviewer_id)},
                                "review_id": {"stringValue": ensure_string(review.review_id)},
                                "comment": {"stringValue": ensure_string(review.comment)},
                                "star_rating": {"doubleValue": review.star_rating or 0},
                                "comment_date": {"stringValue": ensure_string(review.comment_date)},
                                "photo_url": {"stringValue": ensure_string(review.photo_url)},
                                "service_type": {"stringValue": ensure_string(review.service_type)},
                                "meal_type": {"stringValue": ensure_string(review.meal_type)},
                                "spend": {"stringValue": ensure_string(review.spend)},
                                "food_score": {"doubleValue": review.food_score or 0},
                                "service_score": {"doubleValue": review.service_score or 0},
                                "atmosphere_score": {"doubleValue": review.atmosphere_score or 0}
                        }
                    }
                })

            json_data = {"writes": writes}

            response = requests.post(url, headers=headers, json=json_data)

            print(f"批量新增評論 HTTP 狀態碼：{response.status_code}")
            print(f"回應內容：{response.text}")

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
    restaurant_ids = set()
    types = ['Restaurants', 'Bars', 'Coffee', 'Takeout', 'Delivery']
    search_url = "https://www.google.com.tw/maps/search/{type}/@{lat},{lon},{radius}m/data=!3m1!1e3!4m2!2m1!6e5?entry=ttu&g_ep=EgoyMDI1MDUxMy4xIKXMDSoASAFQAw%3D%3D"

    for t in types:
        url = search_url.format(type=t, lat=lat, lon=lon, radius=radius)
        retries = 3
        for attempt in range(retries):
            try:
                response = requests.get(url, headers=headers, timeout=10)
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

def get_restaurant_info(restaurant_id):
    restaurant_name_url = "https://www.google.com.tw/maps/place/data=!4m5!3m4!1s{restaurant_id}!8m2!3d25.0564743!4d121.5204167?authuser=0&hl=zh-TW&rclk=1"
    url = restaurant_name_url.format(restaurant_id=restaurant_id)
    retries = 3
    for attempt in range(retries):
        try:
            response = requests.get(url, headers=headers, timeout=15)
            if response.status_code == 400:
                raise requests.RequestException("get_restaurant_info false HTTP 400 Bad Request {url}")
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

def get_restaurants_in_area(center_lat, center_lon, search_radius):
    restaurant_ids = search_restaurants_id_in_area(center_lat, center_lon, search_radius)
    restaurants: list[Restaurant] = []

    def fetch_name(restaurant_id):
        retries = 3
        for attempt in range(retries):
            try:
                return get_restaurant_info(restaurant_id)
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

def add_res_by_name():
    pass


def main():
    restaurants: list[Restaurant] = get_restaurants_in_area(center_lat=center_lat, center_lon=center_lon, search_radius=search_radius)
    print(f"找到 {len(restaurants)} 家餐廳")
    with tpe() as executor:
        futures = []
        for idx, restaurant in enumerate(restaurants, start=1):
            print(f"正在處理第 {idx}/{len(restaurants)} 家餐廳的評論抓取...")
            futures.append(executor.submit(restaurant.get_reviews))

        for idx, (future, restaurant) in enumerate(zip(futures, restaurants), start=1):
            future.result()
            print(f"第 {idx}/{len(restaurants)} 家餐廳的評論抓取完成。")
            if len(restaurant.reviews) > 0:
                print(f"正在上傳第 {idx}/{len(restaurants)} 家餐廳的評論到 Firestore...")
                executor.submit(restaurant.upload_to_firestore)


if __name__ == "__main__":
    main()

'''
評論者id            reviewer_id
評論id              review_id
評論者的總評論數     reviewer_total_reviews
評論者的總照片數     reviewer_total_photos
星級                star_rating 
評論                comment
一張照片            photo_url
使用服務            service_type
餐點類型            meal_type
消費金額            spend
餐點分數            food_score
服務分數            service_score
氣氛分數            atmosphere_score
留言日期            comment_date
'''