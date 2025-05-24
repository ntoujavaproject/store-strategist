'''
此程式用於在指定區域內搜尋餐廳並上傳到 Firestore

執行流程：
1. 使用給定的中心點座標和搜尋半徑尋找區域內的所有餐廳
2. 使用多執行緒方式並行抓取每家餐廳的評論
3. 將評論資料保存到本地 JSON 檔案
4. 使用多執行緒方式並行上傳餐廳資料到 Firestore
'''

from concurrent.futures import ThreadPoolExecutor as tpe

from utils import (
    get_restaurants_in_area,
    save_restaurants_to_json,
)

from config import (
    center_lat,
    center_lon,
    search_radius,
)
from restaurant import Restaurant



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