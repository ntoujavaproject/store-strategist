class Review:
    def __init__(self, reviewer_name, reviewer_state, reviewer_id,
                 reviewer_total_reviews, reviewer_total_photos, star_rating,
                 comment, photo_url, service_type, meal_type, spend,
                 food_score, service_score, atmosphere_score, comment_date):
        self.reviewer_name = reviewer_name
        self.reviewer_state = reviewer_state
        self.reviewer_id = reviewer_id
        self.reviewer_total_reviews = reviewer_total_reviews
        self.reviewer_total_photos = reviewer_total_photos
        self.star_rating = star_rating
        self.comment = comment
        self.photo_url = photo_url
        self.service_type = service_type
        self.meal_type = meal_type
        self.spend = spend
        self.food_score = food_score
        self.service_score = service_score
        self.atmosphere_score = atmosphere_score
        self.comment_date = comment_date

'''
評論者姓名          reviewer_name
評論者狀態          reviewer_state
評論者id            reviewer_id
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