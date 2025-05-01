import { MapContainer, TileLayer, Marker, Polyline, Tooltip, useMap, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { LocateFixed, ZoomIn, ZoomOut, RefreshCw, Navigation, MapPin, AlertCircle } from "lucide-react";
import { useState, useEffect } from 'react';
import axios from 'axios';
import API_BASE_URL from '../api/config';
import { set } from 'date-fns';

// Fix Leaflet default icon issue
delete L.Icon.Default.prototype._getIconUrl;

// Custom icons
const towerIcon = new L.Icon({
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

const virtualTowerIcon = new L.Icon({
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
  className: 'virtual-tower-icon'
});

const startIcon = new L.Icon({
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
  className: 'start-marker-icon'
});

const endIcon = new L.Icon({
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
  className: 'end-marker-icon'
});

const currentLocationIcon = new L.Icon({
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
  className: 'current-location-icon'
});


// Add custom styles
const customStyles = `
.virtual-tower-icon {
  filter: hue-rotate(120deg);
}

.start-marker-icon {
  filter: hue-rotate(180deg);
}

.end-marker-icon {
  filter: hue-rotate(280deg);
}

.current-location-icon {
  filter: hue-rotate(60deg) brightness(1.2);
}

/* Fixed animation for paths */
.tower-path {
  stroke-dasharray: 5, 8;
  animation: dash 30s linear infinite;
}

.custom-path {
  stroke-dasharray: 5, 10;
  animation: dash 20s linear infinite reverse;
}

@keyframes dash {
  from {
    stroke-dashoffset: 0;
  }
  to {
    stroke-dashoffset: 1000;
  }
}

.tower-details {
  font-size: 12px;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 6px;
  padding: 4px 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.custom-zoom-controls {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.custom-zoom-controls button {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.15);
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.custom-zoom-controls button:hover {
  background-color: #f0f0f0;
}

.tower-info-panel {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.info-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-weight: 500;
  color: #666;
  width: 120px;
  flex-shrink: 0;
}

.info-value {
  flex-grow: 1;
}

.map-container {
  height: 100vh;
  width: 100%;
  position: relative;
}

.selected-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
}

.tower-list-item {
  cursor: pointer;
  transition: background-color 0.2s ease;
  padding: 8px 12px;
  border-radius: 4px;
  display: flex;
  align-items: center;
}

.tower-list-item:hover {
  background-color: #f0f7ff;
}

.tower-list-item.selected {
  background-color: #e0f0ff;
}

.coordinate-selection-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 16px;
  margin-top: 16px;
}

.tab-container {
  display: flex;
  border-bottom: 1px solid #eee;
  margin-bottom: 12px;
}

.tab {
  padding: 8px 16px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s ease;
}

.tab.active {
  border-bottom-color: #3B82F6;
  color: #3B82F6;
  font-weight: 500;
}

.input-container {
  margin-bottom: 12px;
}

.coordinate-button {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  background-color: #f0f7ff;
  border: 1px solid #d1e0ff;
  border-radius: 4px;
  transition: all 0.2s ease;
  cursor: pointer;
  font-weight: 500;
  margin-bottom: 8px;
}

.coordinate-button:hover {
  background-color: #e0f0ff;
}

.coordinate-button.active {
  background-color: #3B82F6;
  color: white;
  border-color: #3B82F6;
}

.form-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 8px;
  font-size: 14px;
}

.error-alert {
  background-color: #FEF2F2;
  color: #B91C1C;
  border: 1px solid #FCA5A5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.error-alert-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

.distance-indicator {
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-radius: 8px;
  padding: 12px;
  margin-top: 12px;
  font-size: 14px;
}

.distance-label {
  font-weight: 500;
  margin-bottom: 4px;
}

.distance-value {
  font-size: 18px;
  font-weight: 600;
  color: #3B82F6;
}

.distance-warning {
  font-size: 12px;
  color: #B45309;
  margin-top: 4px;
}
`;

// Add the styles to the document
const styleSheet = document.createElement("style");
styleSheet.textContent = customStyles;
document.head.appendChild(styleSheet);

function ZoomControl({ towerData }) {
  const map = useMap();

  const zoomIn = () => {
    map.zoomIn();
  };

  const zoomOut = () => {
    map.zoomOut();
  };

  const centerOnPath = () => {
    if (towerData && towerData.path && towerData.path.length > 0) {
      const bounds = L.latLngBounds([]);
      towerData.path.forEach(tower => {
        bounds.extend([tower.latitude, tower.longitude]);
      });
      map.fitBounds(bounds, { padding: [50, 50] });
    }
  };

  return (
    <div className="absolute top-4 right-4 z-[1000] custom-zoom-controls">
      <button onClick={zoomIn} title="تكبير">
        <ZoomIn className="w-5 h-5" />
      </button>
      <button onClick={zoomOut} title="تصغير">
        <ZoomOut className="w-5 h-5" />
      </button>
      <button onClick={centerOnPath} title="إظهار المسار بالكامل">
        <RefreshCw className="w-5 h-5" />
      </button>
    </div>
  );
}

// Custom map click handler for coordinate selection
function CoordinateSelector({ selectingStart, startPoint, endPoint, setStartPoint, setEndPoint }) {
  useMapEvents({
    click(e) {
      if (!selectingStart && !startPoint && !endPoint) return;
      
      const { lat, lng } = e.latlng;
      const point = { lat, lng };
      
      if (selectingStart) {
        setStartPoint(point);
      } else {
        setEndPoint(point);
      }
    },
  });

  return null;
}

// Component to handle current location functionality
function LocationComponent({ map, setCurrentLocation }) {
  const handleLocationFound = (e) => {
    const { lat, lng } = e.latlng;
    const accuracy = e.accuracy;
    const point = { lat, lng, accuracy };
    
    setCurrentLocation(point);
    map.setView([lat, lng], map.getZoom());
  };
  
  useMapEvents({
    locationfound: handleLocationFound,
  });
  
  return null;
}

// Calculate distance between two points in km
function calculateDistance(point1, point2) {
  if (!point1 || !point2) return 0;
  
  const R = 6371; // Earth radius in km
  const dLat = (point2.lat - point1.lat) * Math.PI / 180;
  const dLon = (point2.lng - point1.lng) * Math.PI / 180;
  
  const a = 
    Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(point1.lat * Math.PI / 180) * Math.cos(point2.lat * Math.PI / 180) * 
    Math.sin(dLon/2) * Math.sin(dLon/2);
  
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  const distance = R * c;
  
  return distance;
}

function TowerMapPage() {
  const [selectedTower, setSelectedTower] = useState(null);
  const [mapCenter, setMapCenter] = useState([25.5493, 48.3327]); // Default center
  const [mapRef, setMapRef] = useState(null);
  const [activeTab, setActiveTab] = useState('coordinates'); // 'towers' or 'coordinates'
  
  // For coordinate selection
  const [startPoint, setStartPoint] = useState(null);
  const [endPoint, setEndPoint] = useState(null);
  const [selectingStart, setSelectingStart] = useState(true);
  const [coordUrl, setCoordUrl] = useState('');
  const [towerData, setTowerData] = useState(null);
  const [polylinePath, setPolylinePath] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentLocation, setCurrentLocation] = useState(null);
  const [distance, setDistance] = useState(0);
  const MAX_ALLOWED_DISTANCE = 10.1; // km

  // Update distance when points change
  useEffect(() => {
    if (startPoint && endPoint) {
      const calculatedDistance = calculateDistance(startPoint, endPoint);
      setDistance(calculatedDistance);
    } else {
      setDistance(0);
    }
  }, [startPoint, endPoint]);

  // Fetch tower data when coordinates are selected
  useEffect(() => {
    const fetchTowerData = async () => {
      if (startPoint && endPoint) {
        setIsLoading(true);
        setError(null);
        
        try {
          const response = await axios.get(
            `${API_BASE_URL}/findpath?startLat=${startPoint.lat.toFixed(7)}&startLon=${startPoint.lng.toFixed(7)}&endLat=${endPoint.lat.toFixed(7)}&endLon=${endPoint.lng.toFixed(7)}`
          ,
          {
            withCredentials: true,
            headers: {
              'Content-Type': 'application/json'
            }
          });
          if(response.data.error) {
           setError(response.data.error);
          }
          setTowerData(response.data);
          
          // Create polyline path data for towers
          if (response.data && response.data.path && response.data.path.length > 0) {
            const path = response.data.path.map(tower => [tower.latitude, tower.longitude]);
            setPolylinePath(path);
          }
        } catch (error) {
          console.error('Error fetching tower data:', error);
          
          // Handle specific API error 
          if (error.response && error.response.data && error.response.data.error) {
            setError(error.response.data.error);
          } else {
            setError("An error occurred while fetching tower data. Please try again.");
          }
          
          setTowerData(null);
          setPolylinePath(null);
        } finally {
          setIsLoading(false);
        }
      }
    };

    fetchTowerData();
  }, [startPoint, endPoint]);

  // Function to calculate bounds from all tower positions
  useEffect(() => {
    if (mapRef && towerData && towerData.path && towerData.path.length > 0) {
      const bounds = L.latLngBounds([]);
      towerData.path.forEach(tower => {
        bounds.extend([tower.latitude, tower.longitude]);
      });
      mapRef.fitBounds(bounds, { padding: [50, 50] });
    }
  }, [mapRef, towerData]);

  // Update URL when coordinates change
  useEffect(() => {
    if (startPoint && endPoint) {
      const url = API_BASE_URL+`/findpath?startLat=${startPoint.lat.toFixed(7)}&startLon=${startPoint.lng.toFixed(7)}&endLat=${endPoint.lat.toFixed(7)}&endLon=${endPoint.lng.toFixed(7)}`;
      setCoordUrl(url);
    } else {
      setCoordUrl('');
    }
  }, [startPoint, endPoint]);

  const handleTowerClick = (tower) => {
    setSelectedTower(tower);
    setMapCenter([tower.latitude, tower.longitude]);
    if (mapRef) {
      mapRef.setView([tower.latitude, tower.longitude], mapRef.getZoom());
    }
  };

  const isVirtualTower = (tower) => {
    return tower.tawalId === "START_VIRTUAL" || tower.tawalId === "END_VIRTUAL";
  };

  const copyUrlToClipboard = () => {
    navigator.clipboard.writeText(coordUrl);
    // You could add a toast notification here
  };

  const navigateToUrl = () => {
    window.location.href = coordUrl;
  };

  // Manual coordinate input handlers
  const handleManualStartInput = (e) => {
    e.preventDefault();
    const form = e.target;
    const lat = parseFloat(form.startLat.value);
    const lng = parseFloat(form.startLon.value);
    
    if (!isNaN(lat) && !isNaN(lng)) {
      setStartPoint({ lat, lng });
    }
  };
  
  const handleManualEndInput = (e) => {
    e.preventDefault();
    const form = e.target;
    const lat = parseFloat(form.endLat.value);
    const lng = parseFloat(form.endLon.value);
    
    if (!isNaN(lat) && !isNaN(lng)) {
      setEndPoint({ lat, lng });
    }
  };

  // Get current location handler
  const handleGetCurrentLocation = () => {
    if (mapRef) {
      mapRef.locate({ setView: true, maxZoom: 16 });
    }
  };

  // Use current location as start/end point
  const useCurrentLocationAsPoint = () => {
    if (currentLocation) {
      if (selectingStart) {
        setStartPoint({ lat: currentLocation.lat, lng: currentLocation.lng });
      } else {
        setEndPoint({ lat: currentLocation.lat, lng: currentLocation.lng });
      }
    }
  };

  return (
    <div className="flex flex-col md:flex-row h-screen bg-gray-100">
      {/* Sidebar with tower information */}
      <div className="w-full md:w-96 p-4 overflow-y-auto bg-white shadow-md z-10">
        <h1 className="text-xl font-bold mb-4 text-gray-800">خريطة الأبراج</h1>
        
        {/* Tab navigation */}
        <div className="tab-container">
          <div 
            className={`tab ${activeTab === 'towers' ? 'active' : ''}`}
            onClick={() => setActiveTab('towers')}
          >
            الأبراج
          </div>
          <div 
            className={`tab ${activeTab === 'coordinates' ? 'active' : ''}`}
            onClick={() => setActiveTab('coordinates')}
          >
            تحديد الإحداثيات
          </div>
        </div>
        
        {/* Error message display */}
        {error && (
          <div className="error-alert">
            <AlertCircle className="w-5 h-5 error-alert-icon" />
            <div>{error}</div>
          </div>
        )}
        
        {/* Tower list tab */}
        {activeTab === 'towers' && (
          <>
            <div className="mb-6">
              <h2 className="text-lg font-medium mb-2 text-gray-700">قائمة الأبراج</h2>
              {isLoading ? (
                <div className="text-center py-4">Loading...</div>
              ) : (
                <div className="space-y-1">
                  {towerData && towerData.path && towerData.path.length > 0 ? (
                    towerData.path.map((tower, index) => (
                      <div 
                        key={index}
                        className={`tower-list-item ${selectedTower === tower ? 'selected' : ''}`}
                        onClick={() => handleTowerClick(tower)}
                      >
                        <div 
                          className="selected-indicator" 
                          style={{ 
                            backgroundColor: isVirtualTower(tower) ? '#4ADE80' : '#3B82F6'
                          }}
                        ></div>
                        <span className="font-medium">{tower.siteName}</span>
                      </div>
                    ))
                  ) : (
                    <div className="text-center py-4 text-gray-500">
                      لا توجد أبراج لعرضها. الرجاء تحديد نقاط البداية والنهاية.
                    </div>
                  )}
                </div>
              )}
            </div>
            
            {/* Selected tower details */}
            {selectedTower && (
              <div className="tower-info-panel">
                <h2 className="text-lg font-medium mb-4 text-gray-700">تفاصيل البرج</h2>
                
                <div className="info-row">
                  <div className="info-label">اسم الموقع</div>
                  <div className="info-value">{selectedTower.siteName}</div>
                </div>
                
                <div className="info-row">
                  <div className="info-label">رقم التعريف</div>
                  <div className="info-value">{selectedTower.tawalId}</div>
                </div>
                
                <div className="info-row">
                  <div className="info-label">خط العرض</div>
                  <div className="info-value">{selectedTower.latitude}</div>
                </div>
                
                <div className="info-row">
                  <div className="info-label">خط الطول</div>
                  <div className="info-value">{selectedTower.longitude}</div>
                </div>
                
                {selectedTower.totalHeight !== undefined && selectedTower.totalHeight !== null && (
                  <div className="info-row">
                    <div className="info-label">الارتفاع الكلي</div>
                    <div className="info-value">{selectedTower.totalHeight} م</div>
                  </div>
                )}
                
                {selectedTower.power && (
                  <div className="info-row">
                    <div className="info-label">الطاقة</div>
                    <div className="info-value">{selectedTower.power}</div>
                  </div>
                )}
                
                {selectedTower.clutter && (
                  <div className="info-row">
                    <div className="info-label">البيئة</div>
                    <div className="info-value">{selectedTower.clutter}</div>
                  </div>
                )}
              </div>
            )}
          </>
        )}
        
        {/* Coordinate selection tab */}
        {activeTab === 'coordinates' && (
          <div className="coordinate-selection-container">
            <h2 className="text-lg font-medium mb-4 text-gray-700">تحديد الإحداثيات</h2>
            
            {/* Current location button */}
            <button 
              className="coordinate-button w-full mb-6 flex items-center justify-center"
              onClick={handleGetCurrentLocation}
            >
              <LocateFixed className="w-4 h-4 mr-2" />
              الحصول على الموقع الحالي
            </button>
            
            {currentLocation && (
              <div className="bg-blue-50 p-3 rounded-md mb-4 text-sm">
                <div className="font-medium mb-2">تم تحديد موقعك الحالي:</div>
                <div>خط العرض: {currentLocation.lat.toFixed(7)}</div>
                <div>خط الطول: {currentLocation.lng.toFixed(7)}</div>
                <div className="mt-2">
                  <button 
                    className="text-blue-600 underline"
                    onClick={useCurrentLocationAsPoint}
                  >
                    استخدام كنقطة {selectingStart ? 'بداية' : 'نهاية'}
                  </button>
                </div>
              </div>
            )}
            
            <div className="mb-4">
              <div className="flex gap-2 mb-4">
                <button 
                  className={`coordinate-button flex-1 ${selectingStart ? 'active' : ''}`}
                  onClick={() => setSelectingStart(true)}
                >
                  <MapPin className="w-4 h-4 mr-2" />
                  نقطة البداية
                </button>
                <button 
                  className={`coordinate-button flex-1 ${!selectingStart ? 'active' : ''}`}
                  onClick={() => setSelectingStart(false)}
                >
                  <Navigation className="w-4 h-4 mr-2" />
                  نقطة النهاية
                </button>
              </div>
              
              <p className="text-sm text-gray-600 mb-4">انقر على الخريطة لتحديد النقاط أو أدخل الإحداثيات يدويًا</p>
              
              {/* Display distance between points if both are set */}
              {distance > 0 && (
                <div className="distance-indicator">
                  <div className="distance-label">المسافة بين النقطتين:</div>
                  <div className="distance-value">{distance.toFixed(2)} كم</div>
                  {distance > MAX_ALLOWED_DISTANCE && error != null && (
                    <div className="distance-warning">
                    
                      تنبيه: المسافة تتجاوز الحد الأقصى المسموح به ({MAX_ALLOWED_DISTANCE} كم)
                    </div>
                  )}
                </div>
              )}
              
              {/* Start point manual input */}
              <div className="mb-6 mt-4">
                <h3 className="font-medium mb-2 text-gray-700">نقطة البداية</h3>
                <form onSubmit={handleManualStartInput}>
                  <div className="input-container">
                    <label className="block text-sm text-gray-600 mb-1">خط العرض</label>
                    <input 
                      type="text" 
                      name="startLat"
                      className="form-input" 
                      placeholder="25.5493"
                      defaultValue={startPoint?.lat?.toFixed(7) || ''}
                    />
                  </div>
                  <div className="input-container">
                    <label className="block text-sm text-gray-600 mb-1">خط الطول</label>
                    <input 
                      type="text" 
                      name="startLon"
                      className="form-input" 
                      placeholder="48.3327"
                      defaultValue={startPoint?.lng?.toFixed(7) || ''}
                    />
                  </div>
                  <button type="submit" className="coordinate-button">تحديث نقطة البداية</button>
                 
                </form>
              </div>
              
              {/* End point manual input */}
              <div className="mb-6">
                <h3 className="font-medium mb-2 text-gray-700">نقطة النهاية</h3>
                <form onSubmit={handleManualEndInput}>
                  <div className="input-container">
                    <label className="block text-sm text-gray-600 mb-1">خط العرض</label>
                    <input 
                      type="text" 
                      name="endLat"
                      className="form-input" 
                      placeholder="25.4908"
                      defaultValue={endPoint?.lat?.toFixed(7) || ''}
                    />
                  </div>
                  <div className="input-container">
                    <label className="block text-sm text-gray-600 mb-1">خط الطول</label>
                    <input 
                      type="text" 
                      name="endLon"
                      className="form-input" 
                      placeholder="48.1631"
                      defaultValue={endPoint?.lng?.toFixed(7) || ''}
                    />
                  </div>
                  <button type="submit" className="coordinate-button">تحديث نقطة النهاية</button>
                </form>
              </div>
              
         
              {/* URL and actions */}
              
            </div>
          </div>
        )}
      </div>

      {/* Map container */}
      <div className="flex-1 relative">
        <div className="map-container">
          <MapContainer 
            center={mapCenter}
            zoom={10}
            style={{ height: "100%", width: "100%" }}
            whenCreated={setMapRef}
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            
            {/* Tower path */}
            {polylinePath && polylinePath.length > 0 && (
              <Polyline 
                positions={polylinePath}
                pathOptions={{ 
                  color: '#3B82F6', 
                  weight: 3, 
                  opacity: 0.8,
                  dashArray: '5, 8',
                  dashOffset: 0
                }}
                className="tower-path"
              />
            )}
            
            {/* Custom coordinate path */}
            {startPoint && endPoint && (
              <Polyline 
                positions={[
                  [startPoint.lat, startPoint.lng],
                  [endPoint.lat, endPoint.lng]
                ]}
                pathOptions={{ 
                  color: '#8B5CF6', 
                  weight: 3, 
                  opacity: 0.8,
                  dashArray: '5, 10',
                  dashOffset: 0
                }}
                className="custom-path"
              />
            )}
            
            {/* Tower markers */}
            {towerData && towerData.path && towerData.path.map((tower, index) => (
              <Marker 
                key={index}
                position={[tower.latitude, tower.longitude]} 
                icon={isVirtualTower(tower) ? virtualTowerIcon : towerIcon}
                eventHandlers={{
                  click: () => setSelectedTower(tower)
                }}
              >
                <Tooltip 
                  permanent={false}
                  direction="top"
                  className="tower-details"
                >
                  <div>
                    <strong>{tower.siteName}</strong>
                    <br/>
                    {tower.tawalId}
                  </div>
                </Tooltip>
              </Marker>
            ))}
            
            {/* Custom coordinate markers */}
            {startPoint && (
              <Marker position={[startPoint.lat, startPoint.lng]} icon={startIcon}>
                <Tooltip 
                  permanent={false}
                  direction="top"
                  className="tower-details"
                >
                  <div>
                    <strong>نقطة البداية</strong>
                    <br/>
                    {startPoint.lat.toFixed(7)}, {startPoint.lng.toFixed(7)}
                  </div>
                </Tooltip>
              </Marker>
            )}
            
            {endPoint && (
              <Marker position={[endPoint.lat, endPoint.lng]} icon={endIcon}>
                <Tooltip 
                  permanent={false}
                  direction="top"
                  className="tower-details"
                >
                  <div>
                    <strong>نقطة النهاية</strong>
                    <br/>
                    {endPoint.lat.toFixed(7)}, {endPoint.lng.toFixed(7)}
                  </div>
                </Tooltip>
              </Marker>
            )}
            
            {/* Current location marker */}
            {currentLocation && (
              <Marker position={[currentLocation.lat, currentLocation.lng]} icon={currentLocationIcon}>
                <Tooltip 
                  permanent={false}
                  direction="top"
                  className="tower-details"
                >
                  <div>
                    <strong>موقعك الحالي</strong>
                    <br/>
                    {currentLocation.lat.toFixed(7)}, {currentLocation.lng.toFixed(7)}
                  </div>
                </Tooltip>
              </Marker>
            )}
            
            {/* Coordinate selection handler */}
            <CoordinateSelector 
              selectingStart={selectingStart}
              startPoint={startPoint}
              endPoint={endPoint}
              setStartPoint={setStartPoint}
              setEndPoint={setEndPoint}
            />
            
            {/* Add location component to track user's current location */}
            <LocationComponent 
              map={mapRef}
              setCurrentLocation={setCurrentLocation}
            />
            
            <ZoomControl towerData={towerData} />
          </MapContainer>
        </div>
      </div>
    </div>
  );
}

export default TowerMapPage;
 