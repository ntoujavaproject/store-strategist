from utils import (
    get_restaurant_info_by_id,
)
from restaurant import Restaurant

try:
    restaurant_id = input("請輸入餐廳ID: ")

    restaurant_name, restaurant_address = get_restaurant_info_by_id(restaurant_id)
    if not restaurant_name or not restaurant_address:
        raise ValueError("無法取得餐廳資訊，請確認餐廳 ID 是否正確。")

    restaruant = Restaurant(restaurant_id, restaurant_name, restaurant_address)
    restaruant.get_reviews()
    restaruant.upload_review()
    print("餐廳資訊已成功上傳至 Firestore。")

except ValueError as ve:
    print(f"錯誤: {ve}")
except Exception as e:
    print(f"發生未預期的錯誤: {e}")