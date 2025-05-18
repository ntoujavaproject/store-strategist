from datetime import datetime
import json
import time

import emoji
import requests

from config import project_id, comment_url, headers1, headers2
from review import Review

class Restaurant:
    def __init__(self, id, name, address):
        self.id = id
        self.name = name
        self.address = address
        self.reviews: list[Review] = []
        self.is_upload = False

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

            response = requests.get(comment_url, params=params, headers=headers1)
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
