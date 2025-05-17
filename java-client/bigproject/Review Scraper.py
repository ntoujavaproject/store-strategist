from bs4 import BeautifulSoup
from datetime import datetime
import requests
import json
import emoji
import time
import re

 
class GoogleMapSpider:
    def __init__(self):
        self.headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                       "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36"
        }
        self.store_id_url = "https://www.google.com.tw/maps/search/{store_name}"
        self.store_name_url = "https://www.google.com.tw/maps/place/data=!4m5!3m4!1s{store_id}!8m2!3d25.0564743!4d121.5204167?authuser=0&hl=zh-TW&rclk=1"
        self.comment_url = "https://www.google.com.tw/maps/rpc/listugcposts"


    def get_store_id(self, store_name):
        '''store_name必須與google地圖搜尋結果完全一致, 例如: 隱家拉麵 士林店'''
        url = self.store_id_url.format(store_name=store_name)
        response = requests.get(url, headers=self.headers)
        soup = BeautifulSoup(response.text, "html.parser")
        pattern = r'0x.{16}:0x.{16}'
        match = re.search(pattern, str(soup)) 
        store_id = match.group()
        
        return store_id

    def get_store_name(self, store_id):
        url = self.store_name_url.format(store_id=store_id)
        response = requests.get(url, headers=self.headers)
        soup = BeautifulSoup(response.text, "html.parser")
        meta_list=soup.find_all('meta')
        store_name=[]
        for i in meta_list:
            if '''itemprop="name"''' in str(i):
                store_name.append(re.search('".*·',str(i)).group()[1:-2])
        store_name=store_name[0]

        return store_name

    
    def get_related_store_names(self, store_name):
        '''輸入店名，返回與搜尋最相關的店名與id'''
        url = self.store_id_url.format(store_name=store_name)
        response = requests.get(url, headers=self.headers)
        soup = BeautifulSoup(response.text, "html.parser")
        pattern = r'0x.{16}:0x.{16}'
        store_id_list = set(re.findall(pattern, str(soup)))
        store_id_list = [store_id.replace('\\', '') for store_id in store_id_list]
        store_name_list = []
        for store_id in store_id_list:
            try:
                store_name_list.append(self.get_store_name(store_id))
            except:
                pass

        store_dict = {index: letter for index,
                    letter in zip(store_name_list, store_id_list)}

        return store_dict
    
    def get_comment(self, store_id, page_count=1, sorted_by= 2):
        '''
        sorted_by 參數對應：
        1 - 最相關 (Most Relevant)
        2 - 最新 (Newest)
        3 - 評分最高 (Highest Rating)
        4 - 評分最低 (Lowest Rating)
        
        每個 page 會有10筆資料，除非評論數未達10筆

        '''
        next_token = ""
        commont_list = []
        for page in range(1, page_count+1):
            print(f"第 {page} 頁開始抓取")
            
            params = {
                "authuser": "0",
                "hl": "zh-TW",
                "gl": "tw",
                "pb": (
                    f"!1m6!1s{store_id}!6m4!4m1!1e1!4m1!1e3!2m2!1i10!2s"
                    f"{next_token}"
                    f"!5m2!1s0OBwZ4OnGsrM1e8PxIjW6AI!7e81!8m5!1b1!2b1!3b1!5b1!7b1!11m0!13m1!1e{sorted_by}"
                )
            }

            response = requests.get(self.comment_url, params=params, headers=self.headers)
            data = json.loads(emoji.demojize(response.text[4:]))
            print(f"第 {page} 抓取結束")

            next_token = data[1]
            commont_list.extend(data[2])
            if not next_token:
                print(f"所有評論以抓取完成，總共抓取 {len(commont_list)} 則評論")
                break
            time.sleep(0.1)
                
        # 提取需要的資料
        commont_dict_list = []
        for comment_data in commont_list:
            
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
                ser = comment_data[0][2][6][0][2][0][0][0][0]
            except:
                ser = None

            try:
                ty = comment_data[0][2][6][1][2][0][0][0][0]
            except:
                ty = None

            try:
                co = comment_data[0][2][6][2][2][0][0][0][0]
            except:
                co = None

            try:
                aa = comment_data[0][2][6][3][11][0]
            except:
                aa = None
            try:
                bb = comment_data[0][2][6][4][11][0]
            except:
                bb = None
            try:
                cc = comment_data[0][2][6][5][11][0]
            except:
                cc = None

            try:
                pp = comment_data[0][2][2][0][1][6]
            except:
                pp = None

            comment_info = {
                "評論者": comment_data[0][1][4][5][0],
                "服務內容": ser,
                "餐點類型": ty,
                "平均每人消費": co,
                "餐點": aa,
                "服務": bb,
                "氣氛": cc,
                "照片數": comment_data[0][1][4][5][6],
                "照片": pp,
                "評論數": comment_data[0][1][4][5][5],
                "評論者id": comment_data[0][0],
                "評論者狀態": comment_data[0][1][4][5][10][0],
                "留言時間":comment_data[0][1][6],
                "留言日期":comment_date,
                "評論":comment_text,
                "評論分數":comment_data[0][2][0][0]

            }
            print(comment_info)
            commont_dict_list.append(comment_info)





        return commont_dict_list


if __name__ == "__main__":

    gms = GoogleMapSpider()
    store_name = "海那邊小食堂（無訂位，無固定公休日）"
    store_id = gms.get_store_id(store_name)
    print(store_id)
    store_name = gms.get_store_name(store_id)
    print(store_name)
    store_dict = gms.get_related_store_names(store_name)
    print(store_dict)
    commont_dict_list = gms.get_comment(store_id=store_id, page_count=20000, sorted_by=2)
    
    with open('comments.json', 'w', encoding='utf-8') as f:
        json.dump(commont_dict_list, f, ensure_ascii=False, indent=4)
