from flask import Flask, request, jsonify
import json

app = Flask(__name__)

# Example data - replace with actual API calls later
competitors_data = {
    "競爭對手 1": {"lat": 25.034, "lng": 121.560},
    "競爭對手 5": {"lat": 25.030, "lng": 121.568},
    "某某咖啡": {"lat": 25.033, "lng": 121.565}
}

@app.route('/search', methods=['GET'])
def search_address():
    address = request.args.get('address')
    if not address:
        return jsonify({"error": "Address parameter is required"}), 400

    print(f"Received search request for address: {address}")

    # --- Placeholder for actual Geocoding API call ---
    # In a real scenario, you would call Google Geocoding API here
    # For now, let's return a dummy coordinate based on the address length
    # and add some competitors nearby
    lat = 25.0330 + (len(address) * 0.0001)
    lng = 121.5654 + (len(address) * 0.0001)
    result = {
        "address": address,
        "location": {"lat": lat, "lng": lng},
        "message": "Dummy location based on address length",
        "nearby_competitors": list(competitors_data.keys()) # Send competitor names
    }
    # --- End Placeholder ---

    print(f"Returning search result: {result}")
    return jsonify(result)

@app.route('/competitor_location', methods=['GET'])
def get_competitor_location():
    name = request.args.get('name')
    if not name:
        return jsonify({"error": "Competitor name parameter is required"}), 400

    print(f"Received location request for competitor: {name}")

    location = competitors_data.get(name)

    if location:
        print(f"Found location for {name}: {location}")
        return jsonify({"name": name, "location": location})
    else:
        print(f"Location not found for {name}")
        return jsonify({"error": f"Location not found for competitor: {name}"}), 404

if __name__ == '__main__':
    # Use 0.0.0.0 to be accessible from other containers/machines if needed,
    # otherwise 127.0.0.1 is safer. Port 5000 is default for Flask.
    app.run(host='127.0.0.1', port=5000, debug=True) 