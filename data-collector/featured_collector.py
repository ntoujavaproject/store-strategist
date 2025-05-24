#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
精選評論和照片收集器
專門用於收集餐廳的精選評論（高評分、有照片）和相關圖片
"""

import sys
import os
import json
import argparse
from datetime import datetime, timedelta
from typing import List, Dict, Any

# 添加當前目錄到路徑
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from restaurant import Restaurant
from review import Review

class FeaturedCollector:
    """精選評論和照片收集器"""
    
    def __init__(self):
        self.results = {
            'restaurant_name': '',
            'restaurant_id': '',
            'featured_reviews': [],
            'featured_photos': [],
            'total_reviews': 0,
            'collection_time': datetime.now().isoformat()
        }
    
    def collect_by_id(self, restaurant_id, restaurant_name="Unknown", max_pages=3):
        """
        根據餐廳ID收集精選評論和照片
        
        Args:
            restaurant_id (str): 餐廳的Google Maps ID
            restaurant_name (str): 餐廳名稱
            max_pages (int): 最大收集頁數，每頁約10則評論
        
        Returns:
            dict: 收集結果
        """
        try:
            print(f"開始收集餐廳資料: {restaurant_name} (ID: {restaurant_id})")
            
            # 創建餐廳實例
            restaurant = Restaurant(restaurant_id, restaurant_name, "Unknown")
            
            # 收集評論，按評分最高排序
            print(f"正在收集評論資料，預計收集 {max_pages} 頁...")
            restaurant.get_reviews(page_count=max_pages, sorted_by=3)  # sorted_by=3 表示評分最高
            
            # 更新基本資訊
            self.results['restaurant_name'] = restaurant.name
            self.results['restaurant_id'] = restaurant.id
            self.results['total_reviews'] = len(restaurant.reviews)
            
            print(f"成功收集到 {len(restaurant.reviews)} 則評論")
            
            # 過濾精選評論
            self._filter_featured_reviews(restaurant.reviews)
            
            print(f"篩選出 {len(self.results['featured_reviews'])} 則精選評論")
            print(f"收集到 {len(self.results['featured_photos'])} 張精選照片")
            
            return self.results
            
        except Exception as e:
            print(f"收集過程中發生錯誤: {e}")
            import traceback
            traceback.print_exc()
            return None
    
    def collect_by_search(self, search_query, max_pages=2):
        """
        根據搜尋關鍵字收集精選評論和照片
        
        Args:
            search_query (str): 搜尋關鍵字
            max_pages (int): 最大收集頁數
        
        Returns:
            dict: 收集結果
        """
        try:
            import requests
            from bs4 import BeautifulSoup
            import re
            
            print(f"正在搜尋餐廳: {search_query}")
            
            # 搜尋餐廳ID
            search_url = f'https://www.google.com.tw/maps/search/{search_query}'
            headers = {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
            
            response = requests.get(search_url, headers=headers, timeout=10)
            soup = BeautifulSoup(response.text, 'html.parser')
            
            # 提取餐廳ID
            pattern = r'0x[a-fA-F0-9]{16}:0x[a-fA-F0-9]{16}'
            matches = re.findall(pattern, str(soup))
            
            if not matches:
                print("找不到餐廳資料")
                return None
            
            restaurant_id = matches[0].replace('\\', '')
            print(f"找到餐廳 ID: {restaurant_id}")
            
            # 使用找到的ID收集評論
            return self.collect_by_id(restaurant_id, search_query, max_pages)
            
        except Exception as e:
            print(f"搜尋過程中發生錯誤: {e}")
            import traceback
            traceback.print_exc()
            return None
    
    def _filter_featured_reviews(self, reviews):
        """
        篩選精選評論
        
        精選條件:
        1. 評分 4 星以上
        2. 有照片
        3. 評論內容不為空
        
        Args:
            reviews: 評論列表
        """
        for review in reviews:
            is_featured = False
            reason = []
            
            # 檢查評分
            if review.star_rating and review.star_rating >= 4:
                is_featured = True
                reason.append(f"{review.star_rating}星高評分")
            
            # 檢查是否有照片
            has_photo = review.photo_url and review.photo_url.strip()
            if has_photo:
                is_featured = True
                reason.append("包含照片")
                self.results['featured_photos'].append(review.photo_url)
            
            # 檢查評論內容
            has_content = review.comment and review.comment.strip()
            
            # 如果符合精選條件且有內容
            if is_featured and has_content:
                featured_review = {
                    'reviewer_name': review.reviewer_name or "匿名用戶",
                    'reviewer_state': review.reviewer_state or "",
                    'star_rating': review.star_rating or 0,
                    'comment': review.comment or "",
                    'photo_url': review.photo_url or "",
                    'comment_date': review.comment_date or "",
                    'food_score': review.food_score or 0,
                    'service_score': review.service_score or 0,
                    'atmosphere_score': review.atmosphere_score or 0,
                    'featured_reason': reason
                }
                
                self.results['featured_reviews'].append(featured_review)
    
    def save_to_file(self, filename="featured_data.json"):
        """
        將結果保存到 JSON 檔案
        
        Args:
            filename (str): 輸出檔案名稱
        """
        try:
            with open(filename, 'w', encoding='utf-8') as f:
                json.dump(self.results, f, ensure_ascii=False, indent=2)
            print(f"結果已保存到 {filename}")
            return True
        except Exception as e:
            print(f"保存檔案時發生錯誤: {e}")
            return False

class FeaturedReviewsCollector:
    """
    精選評論收集器
    由於 Google Maps API 不提供按讚數，改用其他指標來判斷精選評論
    """
    
    def __init__(self):
        self.weight_config = {
            'authority': 0.35,   # 評論者權威性權重（包含在地嚮導加分）
            'quality': 0.35,     # 評論質量權重
            'recency': 0.2,      # 時間新近性權重
            'rating': 0.1        # 評分權重
        }
    
    def get_featured_reviews(self, reviews: List[Review], top_n: int = 10) -> List[Dict[str, Any]]:
        """
        獲取精選評論（模擬按讚數最高的概念）
        
        Args:
            reviews: 評論列表
            top_n: 返回前 N 條精選評論
            
        Returns:
            按質量評分排序的精選評論列表
        """
        if not reviews:
            return []
        
        # 計算每條評論的綜合評分
        scored_reviews = []
        for review in reviews:
            score = self._calculate_review_score(review)
            scored_reviews.append({
                'review': review,
                'score': score,
                'authority_score': self._calculate_authority_score(review),
                'quality_score': self._calculate_quality_score(review),
                'recency_score': self._calculate_recency_score(review),
                'rating_score': self._calculate_rating_score(review)
            })
        
        # 按評分排序並返回前 N 條
        featured = sorted(scored_reviews, key=lambda x: x['score'], reverse=True)
        return featured[:top_n]
    
    def _calculate_review_score(self, review: Review) -> float:
        """計算評論的綜合評分"""
        authority = self._calculate_authority_score(review)
        quality = self._calculate_quality_score(review)
        recency = self._calculate_recency_score(review)
        rating = self._calculate_rating_score(review)
        
        total_score = (
            authority * self.weight_config['authority'] +
            quality * self.weight_config['quality'] +
            recency * self.weight_config['recency'] +
            rating * self.weight_config['rating']
        )
        
        return total_score
    
    def _calculate_authority_score(self, review: Review) -> float:
        """
        計算評論者權威性評分
        基於評論者的總評論數、總照片數和在地嚮導身份
        """
        total_reviews = review.reviewer_total_reviews or 0
        total_photos = review.reviewer_total_photos or 0
        reviewer_state = review.reviewer_state or ""
        
        # 正規化評分 (0-1)
        review_score = min(total_reviews / 100.0, 1.0)  # 100+ 評論視為滿分
        photo_score = min(total_photos / 50.0, 1.0)     # 50+ 照片視為滿分
        
        # 檢查是否為在地嚮導，給予額外加分
        local_guide_bonus = 0.0
        if "在地嚮導" in reviewer_state or "Local Guide" in reviewer_state:
            local_guide_bonus = 0.2  # 在地嚮導額外加 20% 權威性
            print(f"發現在地嚮導: {review.reviewer_name} - {reviewer_state}")
        
        # 基礎分數：評論數和照片數的平均
        base_score = (review_score + photo_score) / 2
        
        # 加上在地嚮導加分，但總分不超過 1.0
        final_score = min(base_score + local_guide_bonus, 1.0)
        
        return final_score
    
    def _calculate_quality_score(self, review: Review) -> float:
        """
        計算評論質量評分
        基於評論長度、是否有照片、是否有詳細評分
        """
        score = 0.0
        
        # 評論長度評分
        comment_length = len(review.comment or "")
        if comment_length > 0:
            length_score = min(comment_length / 200.0, 1.0)  # 200字以上視為滿分
            score += length_score * 0.4
        
        # 是否有照片
        if review.photo_url:
            score += 0.3
        
        # 是否有詳細評分（餐點、服務、氣氛）
        detail_scores = [review.food_score, review.service_score, review.atmosphere_score]
        if any(score is not None for score in detail_scores):
            score += 0.3
        
        return min(score, 1.0)
    
    def _calculate_recency_score(self, review: Review) -> float:
        """
        計算時間新近性評分
        越新的評論評分越高
        """
        if not review.comment_date:
            return 0.1  # 沒有日期的評論給最低分
        
        try:
            review_date = datetime.strptime(review.comment_date.split()[0], '%Y/%m/%d')
            days_ago = (datetime.now() - review_date).days
            
            if days_ago <= 30:
                return 1.0      # 一個月內
            elif days_ago <= 90:
                return 0.8      # 三個月內
            elif days_ago <= 180:
                return 0.6      # 半年內
            elif days_ago <= 365:
                return 0.4      # 一年內
            else:
                return 0.2      # 一年以上
                
        except (ValueError, AttributeError):
            return 0.1
    
    def _calculate_rating_score(self, review: Review) -> float:
        """
        計算評分分數
        5星評論獲得最高分，但也會考慮其他星級的評論
        """
        rating = review.star_rating or 0
        
        if rating >= 5:
            return 1.0
        elif rating >= 4:
            return 0.8
        elif rating >= 3:
            return 0.6
        elif rating >= 2:
            return 0.4
        else:
            return 0.2
    
    def get_most_helpful_reviews(self, reviews: List[Review], top_n: int = 10) -> List[Dict[str, Any]]:
        """
        獲取最有幫助的評論（權威性評論者的評論）
        模擬「按讚數最高」的概念，特別重視在地嚮導
        """
        authority_reviews = []
        
        for review in reviews:
            # 計算權威性分數
            authority_score = self._calculate_authority_score(review)
            reviewer_state = review.reviewer_state or ""
            is_local_guide = "在地嚮導" in reviewer_state or "Local Guide" in reviewer_state
            
            # 降低權威性閾值，如果是在地嚮導的話
            threshold = 0.3 if is_local_guide else 0.5
            
            if authority_score >= threshold:
                authority_reviews.append({
                    'review': review,
                    'authority_score': authority_score,
                    'total_engagement': (review.reviewer_total_reviews or 0) + (review.reviewer_total_photos or 0),
                    'is_local_guide': is_local_guide,
                    'reviewer_state': reviewer_state
                })
        
        # 按權威性和參與度排序，在地嚮導會因為權威性加分而排在前面
        sorted_reviews = sorted(authority_reviews, 
                               key=lambda x: (x['authority_score'], x['total_engagement']), 
                               reverse=True)
        
        return sorted_reviews[:top_n]
    
    def export_featured_reviews(self, reviews: List[Review], filename: str, top_n: int = 10):
        """
        將精選評論導出到 JSON 檔案
        """
        featured = self.get_featured_reviews(reviews, top_n)
        
        export_data = []
        for item in featured:
            review = item['review']
            export_data.append({
                'reviewer_name': review.reviewer_name,
                'star_rating': review.star_rating,
                'comment': review.comment,
                'comment_date': review.comment_date,
                'reviewer_total_reviews': review.reviewer_total_reviews,
                'reviewer_total_photos': review.reviewer_total_photos,
                'photo_url': review.photo_url,
                'quality_score': round(item['score'], 3),
                'authority_score': round(item['authority_score'], 3),
                'quality_breakdown': {
                    'authority': round(item['authority_score'], 3),
                    'quality': round(item['quality_score'], 3),
                    'recency': round(item['recency_score'], 3),
                    'rating': round(item['rating_score'], 3)
                }
            })
        
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(export_data, f, ensure_ascii=False, indent=2)
        
        print(f"精選評論已導出到: {filename}")
        return export_data

def main():
    """主函數，處理命令行參數"""
    parser = argparse.ArgumentParser(description='收集餐廳精選評論和照片')
    parser.add_argument('--id', type=str, help='餐廳 Google Maps ID')
    parser.add_argument('--name', type=str, default='Unknown', help='餐廳名稱')
    parser.add_argument('--search', type=str, help='搜尋關鍵字')
    parser.add_argument('--pages', type=int, default=3, help='收集頁數 (每頁約10則評論)')
    parser.add_argument('--output', type=str, default='featured_data.json', help='輸出檔案名稱')
    
    args = parser.parse_args()
    
    # 檢查參數
    if not args.id and not args.search:
        print("錯誤: 必須提供 --id 或 --search 參數")
        parser.print_help()
        return
    
    # 創建收集器
    collector = FeaturedCollector()
    
    # 執行收集
    if args.id:
        results = collector.collect_by_id(args.id, args.name, args.pages)
    else:
        results = collector.collect_by_search(args.search, args.pages)
    
    if results:
        # 保存結果
        collector.save_to_file(args.output)
        
        # 顯示摘要
        print("\n" + "="*50)
        print("收集完成!")
        print(f"餐廳名稱: {results['restaurant_name']}")
        print(f"總評論數: {results['total_reviews']}")
        print(f"精選評論: {len(results['featured_reviews'])}")
        print(f"精選照片: {len(results['featured_photos'])}")
        if results['total_reviews'] > 0:
            percentage = len(results['featured_reviews']) / results['total_reviews'] * 100
            print(f"精選比例: {percentage:.1f}%")
        print("="*50)
    else:
        print("收集失敗!")
        sys.exit(1)

if __name__ == '__main__':
    main() 