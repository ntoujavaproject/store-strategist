'''
此程式用於根據餐廳名稱搜尋並上傳資料到 Firestore

執行流程：
1. 從使用者輸入獲取餐廳名稱
2. 根據餐廳名稱搜尋餐廳ID
3. 使用ID獲取餐廳詳細資訊
4. 抓取該餐廳的所有評論
5. 將餐廳資料和評論上傳到 Firestore

錯誤處理：
- 找不到餐廳ID時會拋出ValueError
- 無法獲取餐廳資訊時會拋出ValueError
- 其他未預期的錯誤會被捕捉並顯示錯誤信息
'''

from utils import (
    get_restaurant_id_by_name,
    get_restaurant_info_by_id,
)
from restaurant import Restaurant

try:
    restaurant_name = input("請輸入餐廳名稱: ")
    restaurant_id = get_restaurant_id_by_name(restaurant_name)
    if not restaurant_id:
        raise ValueError("無法找到該餐廳的 ID，請確認餐廳名稱是否正確。")

    restaurant_name, restaurant_address = get_restaurant_info_by_id(restaurant_id)
    if not restaurant_name or not restaurant_address:
        raise ValueError("無法取得餐廳資訊，請確認餐廳 ID 是否正確。")

    restaruant = Restaurant(restaurant_id, restaurant_name, restaurant_address)
    restaruant.get_reviews()
    restaruant.upload_to_firestore()
    print("餐廳資訊已成功上傳至 Firestore。")

except ValueError as ve:
    print(f"錯誤: {ve}")
except Exception as e:
    print(f"發生未預期的錯誤: {e}")
