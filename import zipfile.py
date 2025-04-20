import zipfile
from fastkml import kml
import geojson
import os

def extract_features_recursively(obj, depth=0):
    features = []
    indent = "  " * depth
    if hasattr(obj, 'features'):
        for f in obj.features:
            print(f"{indent}🔍 Found container: {type(f).__name__} - {getattr(f, 'name', None)}")
            features += extract_features_recursively(f, depth + 1)
    elif hasattr(obj, 'geometry') and obj.geometry:
        print(f"{indent}📍 Found placemark: {obj.name}")
        geom = geojson.loads(geojson.dumps(obj.geometry.__geo_interface__))
        props = {"name": obj.name}
        if obj.extended_data and obj.extended_data.elements:
            for data in obj.extended_data.elements:
                props[data.name] = data.value
        feature = geojson.Feature(geometry=geom, properties=props)
        features.append(feature)
    else:
        print(f"{indent}⚠️ Skipped: {type(obj).__name__} - no geometry")
    return features

def kmz_to_geojson(kmz_path, output_geojson_path):
    # Step 1: Extract KMZ
    with zipfile.ZipFile(kmz_path, 'r') as kmz:
        kmz.extractall("temp_kmz_extracted")
        kml_file = None
        for file in kmz.namelist():
            if file.endswith(".kml"):
                kml_file = os.path.join("temp_kmz_extracted", file)
                break

    if not kml_file:
        print("No KML file found inside KMZ.")
        return

    # Step 2: Parse KML
    with open(kml_file, 'rt', encoding='utf-8') as f:
        doc = f.read()

    k = kml.KML()
    k.from_string(doc.encode('utf-8'))
    
    # Step 3: Extract features recursively
    features = extract_features_recursively(k)

    # Step 4: Write GeoJSON
    feature_collection = geojson.FeatureCollection(features)
    with open(output_geojson_path, 'w', encoding='utf-8') as f:
        geojson.dump(feature_collection, f, indent=2)

    print(f"✅ GeoJSON saved to: {output_geojson_path}")
    print(f"📍 Found {len(features)} features.")

# Usage
kmz_to_geojson("DB_Commercial_2025_Jan_damam.kmz", "towers.geojson")
