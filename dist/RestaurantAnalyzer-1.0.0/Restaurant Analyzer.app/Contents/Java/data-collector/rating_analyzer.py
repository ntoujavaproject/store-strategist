#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
餐廳評分分析器 - 從 Firestore 獲取評論數據並計算各項評分的平均值
用於生成柱狀圖數據
"""

import json
import requests
import sys
import statistics
from typing import Dict, List, Optional, Tuple
from config import project_id

class RatingAnalyzer:
    def __init__(self):
        self.firestore_base_url = f"https://firestore.googleapis.com/v1/projects/{project_id}/databases/(default)/documents"
    
    def get_restaurant_reviews(self, restaurant_id: str) -> List[Dict]:
        """
        從 Firestore 獲取指定餐廳的所有評論
        
        Args:
            restaurant_id (str): 餐廳 ID
            
        Returns:
            List[Dict]: 評論數據列表
        """
        try:
            # 查詢指定餐廳的評論子集合
            # 路徑：restaurants/{restaurant_id}/reviews
            url = f"{self.firestore_base_url}/restaurants/{restaurant_id}/reviews"
            params = {
                "pageSize": 1000  # 最大獲取 1000 筆評論
            }
            
            all_reviews = []
            page_token = None
            
            while True:
                if page_token:
                    params["pageToken"] = page_token
                
                response = requests.get(url, params=params, timeout=30)
                response.raise_for_status()
                data = response.json()
                
                if "documents" not in data:
                    break
                
                # 直接獲取所有評論（已經是該餐廳的評論）
                for doc in data["documents"]:
                    fields = doc.get("fields", {})
                    all_reviews.append(fields)
                
                # 檢查是否有下一頁
                page_token = data.get("nextPageToken")
                if not page_token:
                    break
            
            print(f"✅ 成功獲取 {len(all_reviews)} 則評論")
            return all_reviews
            
        except Exception as e:
            print(f"❌ 獲取評論數據失敗: {e}")
            # 如果子集合不存在，嘗試舊的查詢方式
            return self._get_reviews_from_main_collection(restaurant_id)
    
    def _get_reviews_from_main_collection(self, restaurant_id: str) -> List[Dict]:
        """
        從主 reviews 集合查詢評論（備用方法）
        """
        try:
            print("🔄 嘗試從主 reviews 集合查詢...")
            url = f"{self.firestore_base_url}/reviews"
            params = {
                "orderBy": "restaurant_id",
                "pageSize": 1000
            }
            
            all_reviews = []
            page_token = None
            
            while True:
                if page_token:
                    params["pageToken"] = page_token
                
                response = requests.get(url, params=params, timeout=30)
                response.raise_for_status()
                data = response.json()
                
                if "documents" not in data:
                    break
                
                # 篩選出指定餐廳的評論
                for doc in data["documents"]:
                    fields = doc.get("fields", {})
                    if fields.get("restaurant_id", {}).get("stringValue") == restaurant_id:
                        all_reviews.append(fields)
                
                page_token = data.get("nextPageToken")
                if not page_token:
                    break
            
            print(f"✅ 從主集合獲取 {len(all_reviews)} 則評論")
            return all_reviews
            
        except Exception as e:
            print(f"❌ 從主集合獲取評論失敗: {e}")
            return []
    
    def extract_rating_scores(self, reviews: List[Dict]) -> Dict[str, List[float]]:
        """
        從評論數據中提取各項評分
        
        Args:
            reviews (List[Dict]): 評論數據列表
            
        Returns:
            Dict[str, List[float]]: 各項評分數據
        """
        scores = {
            "餐點": [],
            "服務": [],
            "環境": [],
            "價格": [],
            "總評": []
        }
        
        for review in reviews:
            # 提取餐點評分
            food_score = self._extract_score(review, "food_score")
            if food_score is not None:
                scores["餐點"].append(food_score)
            
            # 提取服務評分
            service_score = self._extract_score(review, "service_score")
            if service_score is not None:
                scores["服務"].append(service_score)
            
            # 提取環境評分
            atmosphere_score = self._extract_score(review, "atmosphere_score")
            if atmosphere_score is not None:
                scores["環境"].append(atmosphere_score)
            
            # 提取總評分（星級）
            star_rating = self._extract_score(review, "star_rating")
            if star_rating is not None:
                scores["總評"].append(star_rating)
            
            # 從消費金額推算價格評分（簡化處理）
            spend = review.get("spend", {}).get("stringValue", "")
            price_score = self._estimate_price_rating(spend)
            if price_score is not None:
                scores["價格"].append(price_score)
        
        print(f"📊 評分數據統計:")
        for category, score_list in scores.items():
            print(f"  {category}: {len(score_list)} 筆數據")
        
        return scores
    
    def _extract_score(self, review: Dict, field_name: str) -> Optional[float]:
        """
        從評論中提取特定評分字段
        
        Args:
            review (Dict): 評論數據
            field_name (str): 字段名稱
            
        Returns:
            Optional[float]: 評分值，如果無效則返回 None
        """
        try:
            field_data = review.get(field_name, {})
            
            # 嘗試不同的數據類型
            if "doubleValue" in field_data:
                score = float(field_data["doubleValue"])
            elif "integerValue" in field_data:
                score = float(field_data["integerValue"])
            elif "stringValue" in field_data:
                score_str = field_data["stringValue"].strip()
                if score_str:
                    score = float(score_str)
                else:
                    return None
            else:
                return None
            
            # 驗證評分範圍（1-5分制）
            if 1.0 <= score <= 5.0:
                return score
            else:
                return None
                
        except (ValueError, TypeError):
            return None
    
    def _estimate_price_rating(self, spend_str: str) -> Optional[float]:
        """
        根據消費金額估算價格評分
        
        Args:
            spend_str (str): 消費金額字符串
            
        Returns:
            Optional[float]: 價格評分 (1-5，數值越低表示越便宜)
        """
        if not spend_str:
            return None
        
        try:
            # 移除非數字字符並提取數值
            import re
            numbers = re.findall(r'\d+', spend_str)
            if not numbers:
                return None
            
            amount = int(numbers[0])
            
            # 根據消費金額範圍給出價格評分（分數越低越便宜）
            if amount <= 200:
                return 5.0  # 非常便宜
            elif amount <= 400:
                return 4.0  # 便宜
            elif amount <= 600:
                return 3.0  # 適中
            elif amount <= 1000:
                return 2.0  # 偏貴
            else:
                return 1.0  # 很貴
                
        except (ValueError, IndexError):
            return None
    
    def calculate_average_ratings(self, scores: Dict[str, List[float]]) -> Dict[str, Dict[str, float]]:
        """
        計算各項評分的平均值和統計信息
        
        Args:
            scores (Dict[str, List[float]]): 各項評分數據
            
        Returns:
            Dict[str, Dict[str, float]]: 包含平均值、中位數、標準差等統計信息
        """
        results = {}
        
        for category, score_list in scores.items():
            if not score_list:
                results[category] = {
                    "average": 0.0,
                    "median": 0.0,
                    "std_dev": 0.0,
                    "count": 0,
                    "min": 0.0,
                    "max": 0.0
                }
                continue
            
            # 計算統計值
            avg = statistics.mean(score_list)
            median = statistics.median(score_list)
            std_dev = statistics.stdev(score_list) if len(score_list) > 1 else 0.0
            count = len(score_list)
            min_score = min(score_list)
            max_score = max(score_list)
            
            results[category] = {
                "average": round(avg, 2),
                "median": round(median, 2),
                "std_dev": round(std_dev, 2),
                "count": count,
                "min": min_score,
                "max": max_score
            }
            
            print(f"📈 {category}評分統計:")
            print(f"  平均分: {avg:.2f}")
            print(f"  中位數: {median:.2f}")
            print(f"  標準差: {std_dev:.2f}")
            print(f"  評論數: {count}")
            print(f"  最低分: {min_score}")
            print(f"  最高分: {max_score}")
            print()
        
        return results
    
    def generate_chart_data(self, restaurant_id: str, restaurant_name: str = "") -> Dict:
        """
        生成柱狀圖所需的完整數據
        
        Args:
            restaurant_id (str): 餐廳 ID
            restaurant_name (str): 餐廳名稱（可選）
            
        Returns:
            Dict: 包含所有統計數據的字典
        """
        print(f"🔍 開始分析餐廳評分數據: {restaurant_name or restaurant_id}")
        
        # 獲取評論數據
        reviews = self.get_restaurant_reviews(restaurant_id)
        
        if not reviews:
            print("❌ 未找到評論數據")
            return {
                "restaurant_id": restaurant_id,
                "restaurant_name": restaurant_name,
                "ratings": {},
                "success": False,
                "message": "未找到評論數據"
            }
        
        # 提取評分數據
        scores = self.extract_rating_scores(reviews)
        
        # 計算統計信息
        rating_stats = self.calculate_average_ratings(scores)
        
        # 生成最終結果
        result = {
            "restaurant_id": restaurant_id,
            "restaurant_name": restaurant_name,
            "total_reviews": len(reviews),
            "ratings": rating_stats,
            "success": True,
            "message": f"成功分析 {len(reviews)} 則評論",
            "timestamp": __import__('datetime').datetime.now().isoformat()
        }
        
        return result

def main():
    """
    主函數 - 命令行接口
    """
    if len(sys.argv) < 2:
        print("使用方法: python rating_analyzer.py <restaurant_id> [restaurant_name]")
        print("範例: python rating_analyzer.py ChIJN1t_tDeuEmsRUsoyG83frY4 '海大燒臘'")
        sys.exit(1)
    
    restaurant_id = sys.argv[1]
    restaurant_name = sys.argv[2] if len(sys.argv) > 2 else ""
    
    # 創建分析器並生成數據
    analyzer = RatingAnalyzer()
    result = analyzer.generate_chart_data(restaurant_id, restaurant_name)
    
    # 輸出結果為 JSON 格式
    print("\n" + "="*50)
    print("📊 評分分析結果:")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    
    # 同時保存到文件
    output_file = f"rating_analysis_{restaurant_id}.json"
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    
    print(f"\n💾 結果已保存到: {output_file}")

if __name__ == "__main__":
    main() 