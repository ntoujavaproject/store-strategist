from datetime import datetime
import json
import time

import emoji
import requests

from config import project_id, comment_url, headers1, headers2
from review import Review

class Restaurant:
    def __init__(self, id, name, address):
        '''
        初始化餐廳物件
        Args:
            id (str): 餐廳的唯一識別碼
            name (str): 餐廳名稱
            address (str): 餐廳地址
        '''
        self.id = id
        self.name = name
        self.address = address
        self.reviews: list[Review] = []
        self.is_upload = False

    def get_reviews(self, page_count=2000, sorted_by=2):
        '''
        從 Google Maps 抓取餐廳評論資料
        
        Args:
            page_count (int, optional): 要抓取的頁數上限。預設為 2000
            sorted_by (int, optional): 評論排序方式。預設為 2 (最新)
                1 - 最相關 (Most Relevant)
                2 - 最新 (Newest)
                3 - 評分最高 (Highest Rating)
                4 - 評分最低 (Lowest Rating)
        
        每個 page 會有10筆資料，除非評論數未達10筆
        評論資料會被儲存在 self.reviews 列表中
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

            response = requests.get(comment_url, params=params, headers=headers1)
            raw_text = response.text
            
            # 檢查回應是否有效
            if response.status_code != 200:
                print(f"⚠️ Google Maps API 回應錯誤 {response.status_code} - 餐廳: {self.name}")
                if page == 1:
                    print(f"❌ 無法獲取第一頁評論，停止收集")
                    break
                else:
                    print(f"🔄 第 {page} 頁失敗，但已收集 {len(comment_list)} 則評論")
                    break
            
            # 嘗試解析 JSON 資料
            try:
                data = json.loads(emoji.demojize(raw_text[4:]))
            except (json.JSONDecodeError, IndexError) as e:
                print(f"⚠️ JSON 解析失敗 - 餐廳: {self.name}, 頁數: {page}")
                print(f"原始回應長度: {len(raw_text)}")
                print(f"回應開頭: {raw_text[:200]}...")
                if page == 1:
                    print(f"❌ 第一頁解析失敗，可能此餐廳沒有評論或API格式變更")
                    break
                else:
                    print(f"🔄 第 {page} 頁解析失敗，但已收集 {len(comment_list)} 則評論")
                    break
            
            # 檢查資料結構
            try:
                next_token = data[1] if len(data) > 1 else None
                current_comments = data[2] if len(data) > 2 else []
            except (IndexError, TypeError):
                print(f"⚠️ 資料結構異常 - 餐廳: {self.name}, 頁數: {page}")
                print(f"資料類型: {type(data)}, 長度: {len(data) if isinstance(data, list) else 'N/A'}")
                if page == 1:
                    print(f"❌ 第一頁資料結構異常，可能此餐廳沒有評論")
                    break
                else:
                    print(f"🔄 第 {page} 頁資料異常，但已收集 {len(comment_list)} 則評論")
                    break
            
            # 添加評論到清單
            if current_comments:
                comment_list.extend(current_comments)
                print(f"✅ 第 {page} 頁: 獲取 {len(current_comments)} 則評論，累計 {len(comment_list)} 則")
            else:
                print(f"🔄 第 {page} 頁: 沒有更多評論")
            
            if not next_token:
                print(f"🎉 所有評論收集完成，總共 {len(comment_list)} 則評論")
                break
            time.sleep(0.1)

        # 提取需要的資料
        for comment_data in comment_list:
            '''try:
                comment_text = comment_data[0][2][-1][0][0]
            except:
                comment_text = None'''

            def extract_nested_data(path, default=None):
                try:
                    data = comment_data
                    for key in path:
                        data = data[key]
                    return data
                except:
                    return default

            reviewer_name          = extract_nested_data([0, 1, 4, 5, 0])
            reviewer_status        = extract_nested_data([0, 1, 4, 5, 10, 0])
            reviewer_id            = extract_nested_data([0, 0])
            reviewer_total_reviews = extract_nested_data([0, 1, 4, 5, 5])
            reviewer_total_photos  = extract_nested_data([0, 1, 4, 5, 6])
            star_rating            = extract_nested_data([0, 2, 0, 0])
            comment                = extract_nested_data([0, 2, -1, 0, 0])
            photo_url              = extract_nested_data([0, 2, 2, 0, 1, 6, 0])
            service_type           = extract_nested_data([0, 2, 6, 0, 2, 0, 0, 0, 0])
            meal_type              = extract_nested_data([0, 2, 6, 1, 2, 0, 0, 0, 0])
            spend                  = extract_nested_data([0, 2, 6, 2, 2, 0, 0, 0, 0])
            food_score             = extract_nested_data([0, 2, 6, 3, 11, 0])
            service_score          = extract_nested_data([0, 2, 6, 4, 11, 0])
            atmosphere_score       = extract_nested_data([0, 2, 6, 5, 11, 0])
            raw_date               = extract_nested_data([0, 2, 2, 0, 1, 21, 6, -1])
            comment_time           = extract_nested_data([0, 1, 6])
            if raw_date and isinstance(raw_date, list) and len(raw_date) >= 4 and all(isinstance(i, int) for i in raw_date[:4]):
                comment_date = str(datetime(raw_date[0], raw_date[1], raw_date[2], raw_date[3]).strftime('%Y/%m/%d %H:%M:%S')) + (comment_time or '')
            else:
                comment_date = None


            self.reviews.append(Review(
                reviewer_name=reviewer_name,
                reviewer_state=reviewer_status,
                reviewer_id=reviewer_id,
                reviewer_total_reviews=reviewer_total_reviews,
                reviewer_total_photos=reviewer_total_photos,
                star_rating=star_rating,
                comment=comment,
                photo_url=photo_url,
                service_type=service_type,
                meal_type=meal_type,
                spend=spend,
                food_score=food_score,
                service_score=service_score,
                atmosphere_score=atmosphere_score,
                comment_date=comment_date
            ))
        return

    def upload_to_firestore(self):
        '''
        上傳餐廳資料到 Firestore
        
        會嘗試上傳餐廳基本資料和評論資料
        如果上傳失敗會重試最多5次
        每次重試的間隔時間會以指數增加
        成功上傳後會將 is_upload 設為 True
        '''
        retries = 5
        for attempt in range(retries):
            try:
                self.upload_res()
                time.sleep(0.1)
                self.upload_review()
                self.is_upload = True
                break
            except Exception as e:
                if attempt < retries - 1:
                    print(f"重試中，嘗試次數：{attempt + 1}/{retries}")
                    time.sleep(2 ** attempt)
                else:
                    print(f"上傳失敗：{e}")

    def upload_res(self):
        '''
        上傳餐廳基本資料到 Firestore
        
        上傳的資料包含：
        - 餐廳名稱
        - 餐廳地址
        - 餐廳ID
        
        如果上傳失敗會重試一次
        '''
        retries = 2
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

                print(f"新增餐廳{self.name} {self.id} HTTP 狀態碼：{response.status_code}")
                break
            except Exception as e:
                if attempt < retries - 1:
                    time.sleep(2 ** attempt)
                else:
                    print(f"發生錯誤：{e}")

    def upload_review(self):
        '''
        批量上傳餐廳評論資料到 Firestore
        
        上傳的評論資料包含：
        - 評論者資訊（名稱、狀態、ID、評論數、照片數）
        - 評論內容（星級、評論文字、照片URL）
        - 用餐資訊（服務類型、用餐類型、消費金額）
        - 評分詳情（食物、服務、氣氛分數）
        - 評論日期時間
        
        使用批次寫入方式，一次上傳所有評論
        '''
        if not self.reviews:
            print(f"⚠️ 餐廳 {self.name} 沒有評論資料，跳過評論上傳")
            return
        
        print(f"📤 準備上傳 {len(self.reviews)} 則評論到 Firestore...")
        
        try:
            url = f"https://firestore.googleapis.com/v1/projects/{project_id}/databases/(default)/documents:commit"
            writes = []
            for review in self.reviews:
                document_path = f"projects/{project_id}/databases/(default)/documents/restaurants/{self.id}/reviews/{review.reviewer_id}"

                def ensure_string(value):
                    return str(value) if value is not None else ""
                
                def ensure_integer(value):
                    return int(value) if value is not None else 0

                writes.append({
                    "update": {
                        "name": document_path,
                        "fields": {
                            "reviewer_name"         : {"stringValue": ensure_string(review.reviewer_name)},
                            "reviewer_state"        : {"stringValue": ensure_string(review.reviewer_state)},
                            "reviewer_id"           : {"stringValue": ensure_string(review.reviewer_id)},
                            "reviewer_total_reviews": {"integerValue": ensure_integer(review.reviewer_total_reviews)},
                            "reviewer_total_photos" : {"integerValue": ensure_integer(review.reviewer_total_photos)},
                            "star_rating"           : {"integerValue": ensure_integer(review.star_rating)},
                            "comment"               : {"stringValue": ensure_string(review.comment)},
                            "photo_url"             : {"stringValue": ensure_string(review.photo_url)},
                            "service_type"          : {"stringValue": ensure_string(review.service_type)},
                            "meal_type"             : {"stringValue": ensure_string(review.meal_type)},
                            "spend"                 : {"stringValue": ensure_string(review.spend)},
                            "food_score"            : {"integerValue": ensure_integer(review.food_score)},
                            "service_score"         : {"integerValue": ensure_integer(review.service_score)},
                            "atmosphere_score"      : {"integerValue": ensure_integer(review.atmosphere_score)},
                            "comment_date"          : {"stringValue": ensure_string(review.comment_date)}
                        }
                    }
                })

            json_data = {"writes": writes}

            response = requests.post(url, headers=headers2, json=json_data)

            print(f"批量新增評論 HTTP 狀態碼：{response.status_code} 餐廳名稱：{self.name} 餐廳ID：{self.id}")
            #print(f"回應內容：{response.text}")

        except Exception as e:
            print(f"發生錯誤：{e} 餐廳名稱：{self.name} 餐廳ID：{self.id}")
