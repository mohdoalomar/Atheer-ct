import json
import psycopg2

def insert_into_db(tower_data):
    conn = psycopg2.connect(
        host="localhost",
        user="atheer",
        password="atheer123",
        database="atheer",
        port="5430"
    )
    cursor = conn.cursor()

    insert_query = """
        INSERT INTO tower (tawal_id, site_name, latitude, longitude, total_height, power, clutter)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
    """

    for tower in tower_data:
        props = tower["properties"]
        # Corrected: GeoJSON uses [longitude, latitude, elevation] format
        lon, lat, _ = tower["geometry"]["coordinates"]

        data = (
            props.get("Tawal_ID"),
            props.get("Site_Name"),
            props.get("Lat", lat),  # Use props lat if available, fallback to coordinate
            props.get("Long", lon), # Use props long if available, fallback to coordinate
            props.get("Total_Height"),
            props.get("Power"),
            props.get("Clutter")
        )
        cursor.execute(insert_query, data)

    conn.commit()
    cursor.close()
    conn.close()
    print("✅ Data imported into database.")

def main():
    with open("towers.geojson", "r", encoding="utf-8") as f:
        geojson_data = json.load(f)

    insert_into_db(geojson_data["features"])

if __name__ == "__main__":
    main()