#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import os

# 添加data-collector目錄到Python路徑
sys.path.append(os.path.join(os.path.dirname(os.path.dirname(__file__)), 'data-collector'))

try:
    from utils import get_restaurant_id_by_name, get_restaurant_info_by_id
    
    def check_restaurant(restaurant_name):
        """檢查餐廳是否存在於Google Maps"""
        try:
            restaurant_id = get_restaurant_id_by_name(restaurant_name)
            if restaurant_id:
                name, address = get_restaurant_info_by_id(restaurant_id)
                if name:
                    print(f"FOUND_NAME:{name}")
                    return True
                else:
                    print("NO_NAME")
                    return False
            else:
                print("NO_ID")
                return False
        except Exception as e:
            print(f"ERROR:{str(e)}")
            return False

    if __name__ == "__main__":
        if len(sys.argv) < 2:
            print("ERROR:Missing restaurant name argument")
            sys.exit(1)
        
        restaurant_name = sys.argv[1]
        check_restaurant(restaurant_name)

except ImportError as e:
    print(f"ERROR:Import failed - {str(e)}")
    sys.exit(1) 