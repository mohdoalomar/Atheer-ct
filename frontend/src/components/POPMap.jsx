import { MapContainer, TileLayer, Marker, Polyline, Tooltip, useMap, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { LocateFixed, ZoomIn, ZoomOut, RefreshCw, Plus, Trash, AlertCircle, Network, Send } from "lucide-react";
import { useState, useEffect } from 'react';
import axios from 'axios';
import API_BASE_URL from '../api/config';

// Fix Leaflet default icon issue
delete L.Icon.Default.prototype._getIconUrl;

// Custom icons
const popIcon = new L.Icon({
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
  className: 'pop-marker-icon'
});

const destIcon = new L.Icon({
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
  className: 'dest-marker-icon'
});

const towerIcon = new L.Icon({
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
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
.pop-marker-icon {
  filter: hue-rotate(0deg) brightness(1.2); /* Red */
}

.dest-marker-icon {
  filter: hue-rotate(280deg); /* Purple */
}

.tower-marker-icon {
  filter: hue-rotate(180deg); /* Blue */
}

.current-location-icon {
  filter: hue-rotate(60deg) brightness(1.2);
}

/* Fix for animated dash lines */
.pop-path {
  stroke-dasharray: 5, 8;
  animation: dash 25s linear infinite;
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

.path-list-item {
  cursor: pointer;
  transition: background-color 0.2s ease;
  padding: 8px 12px;
  border-radius: 4px;
  display: flex;
  align-items: center;
}

.path-list-item:hover {
  background-color: #f0f7ff;
}

.path-list-item.selected {
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

.coordinate-button.red {
  background-color: #FEE2E2;
  border-color: #FECACA;
  color: #B91C1C;
}

.coordinate-button.red:hover {
  background-color: #FECACA;
}

.coordinate-button.green {
  background-color: #DCFCE7;
  border-color: #BBF7D0;
  color: #16A34A;
}

.coordinate-button.green:hover {
  background-color: #BBF7D0;
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

.stats-container {
  background-color: #F3F4F6;
  border-radius: 8px;
  padding: 12px;
  margin-top: 16px;
}

.stats-heading {
  font-weight: 600;
  margin-bottom: 8px;
  color: #4B5563;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.stat-card {
  background-color: white;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #3B82F6;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #6B7280;
}

.destination-chip {
  display: inline-flex;
  align-items: center;
  background-color: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-radius: 16px;
  padding: 4px 12px;
  margin-right: 8px;
  margin-bottom: 8px;
  font-size: 12px;
}

.destination-chip-number {
  background-color: #3B82F6;
  color: white;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 6px;
  font-size: 10px;
}

.destination-chip-remove {
  margin-left: 6px;
  cursor: pointer;
  color: #6B7280;
}

.destination-chip-remove:hover {
  color: #EF4444;
}

.destinations-container {
  display: flex;
  flex-wrap: wrap;
  margin-top: 8px;
  margin-bottom: 12px;
}
`;

// Add the styles to the document
const styleSheet = document.createElement("style");
styleSheet.textContent = customStyles;
document.head.appendChild(styleSheet);

function ZoomControl({ networkData }) {
  const map = useMap();

  const zoomIn = () => {
    map.zoomIn();
  };

  const zoomOut = () => {
    map.zoomOut();
  };

  const centerOnNetwork = () => {
    if (networkData && networkData.paths && networkData.paths.length > 0) {
      const bounds = L.latLngBounds([]);
      
      // Add POP location
      if (networkData.pop) {
        bounds.extend([networkData.pop.latitude, networkData.pop.longitude]);
      }
      
      // Add all paths and their points
      networkData.paths.forEach(pathData => {
        if (pathData.path && pathData.path.length > 0) {
          pathData.path.forEach(tower => {
            bounds.extend([tower.latitude, tower.longitude]);
          });
        }
      });
      
      map.fitBounds(bounds, { padding: [50, 50] });
    }
  };

  return (
    <div className="absolute top-4 right-4 z-[1000] custom-zoom-controls">
      <button onClick={zoomIn} title="<bdi>تكبير</bdi>">
        <ZoomIn className="w-5 h-5" />
      </button>
      <button onClick={zoomOut} title="<bdi>تصغير</bdi>">
        <ZoomOut className="w-5 h-5" />
      </button>
      <button onClick={centerOnNetwork} title="<bdi>إظهار الشبكة بالكامل</bdi>">
        <RefreshCw className="w-5 h-5" />
      </button>
    </div>
  );
}

// Custom map click handler for POP selection
function POPSelector({ active, setPOPPoint }) {
  useMapEvents({
    click(e) {
      if (!active) return;
      
      const { lat, lng } = e.latlng;
      setPOPPoint({ lat, lng });
    },
  });

  return null;
}

// Custom map click handler for destination selection
function DestinationSelector({ active, addDestination }) {
  useMapEvents({
    click(e) {
      if (!active) return;
      
      const { lat, lng } = e.latlng;
      addDestination({ lat, lng });
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

function POPMapPage() {
  const [selectedPath, setSelectedPath] = useState(null);
  const [selectedTower, setSelectedTower] = useState(null);
  const [mapCenter, setMapCenter] = useState([25.5493, 48.3327]); // Default center
  const [mapRef, setMapRef] = useState(null);
  const [activeTab, setActiveTab] = useState('pop'); // 'pop' or 'paths'
  
  // For POP and destination selection
  const [popPoint, setPOPPoint] = useState(null);
  const [destinations, setDestinations] = useState([]);
  const [addingDestination, setAddingDestination] = useState(false);
  const [addingPOP, setAddingPOP] = useState(false); // New state for POP selection
  const [networkData, setNetworkData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentLocation, setCurrentLocation] = useState(null);

  
  // Update map bounds when network data changes
  useEffect(() => {
    if (mapRef && networkData && networkData.paths && networkData.paths.length > 0) {
      const bounds = L.latLngBounds([]);
      
      // Add POP location
      if (networkData.pop) {
        bounds.extend([networkData.pop.latitude, networkData.pop.longitude]);
      }
      
      // Add all destinations
      networkData.paths.forEach(pathData => {
        if (pathData.path && pathData.path.length > 0) {
          pathData.path.forEach(tower => {
            bounds.extend([tower.latitude, tower.longitude]);
          });
        }
      });
      
      mapRef.fitBounds(bounds, { padding: [50, 50] });
    }
  }, [mapRef, networkData]);

  // Fetch network data when POP and destinations are set
  const fetchNetworkData = async () => {
    if (!popPoint || destinations.length === 0) {
      setError(<bdi>"يرجى تحديد نقطة POP ونقطة وجهة واحدة على الأقل"</bdi>);
      return;
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      // Format destinations for API
      const destinationList = destinations.map(dest => ({
        latitude: dest.lat,
        longitude: dest.lng
      }));
      
      // Make API call
      const response = await axios.post(
        `${API_BASE_URL}/pop?popLat=${popPoint.lat.toFixed(7)}&popLon=${popPoint.lng.toFixed(7)}`,
        destinationList,
        {
          withCredentials: true, 
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );
      if (response.data.error) {
        setError(response.data.error);
        setNetworkData(null);
      } else {
        setNetworkData(response.data);
        
        // Add pop data if not included in the response
        if (!response.data.pop) {
          response.data.pop = {
            latitude: popPoint.lat,
            longitude: popPoint.lng,
            name: "POP Virtual Tower"
          };
        }
      }
    } catch (error) {
      console.error('Error fetching network data:', error);
      
      // Handle specific API error 
      if (error.response && error.response.data && error.response.data.error) {
        setError(error.response.data.error);
      } else {
        setError(<bdi>"حدث خطأ أثناء جلب بيانات الشبكة. يرجى المحاولة مرة أخرى."</bdi>);
      }
      
      setNetworkData(null);
    } finally {
      setIsLoading(false);
    }
  };

  const handleTowerClick = (tower) => {
    setSelectedTower(tower);
    setSelectedPath(null);
    setMapCenter([tower.latitude, tower.longitude]);
    if (mapRef) {
      mapRef.setView([tower.latitude, tower.longitude], mapRef.getZoom());
    }
  };

  const handlePathClick = (path, index) => {
    setSelectedPath({ ...path, index });
    setSelectedTower(null);
    
    // Center map on middle of path
    if (mapRef && path.path && path.path.length > 0) {
      const bounds = L.latLngBounds([]);
      path.path.forEach(tower => {
        bounds.extend([tower.latitude, tower.longitude]);
      });
      mapRef.fitBounds(bounds, { padding: [50, 50] });
    }
  };

  // Manual coordinate input handlers
  const handleManualPOPInput = (e) => {
    e.preventDefault();
    const form = e.target;
    const lat = parseFloat(form.popLat.value);
    const lng = parseFloat(form.popLon.value);
    
    if (!isNaN(lat) && !isNaN(lng)) {
      setPOPPoint({ lat, lng });
    }
  };
  
  // Add a destination point
  const addDestination = (point) => {
    setDestinations([...destinations, point]);

  };
  
  // Remove a destination point
  const removeDestination = (index) => {
    const newDestinations = [...destinations];
    newDestinations.splice(index, 1);
    setDestinations(newDestinations);
  };
  
  // Clear all destinations
  const clearDestinations = () => {
    setDestinations([]);
  };

  // Get current location handler
  const handleGetCurrentLocation = () => {
    if (mapRef) {
      mapRef.locate({ setView: true, maxZoom: 16 });
    }
  };

  // Use current location as POP point
  const useCurrentLocationAsPOP = () => {
    if (currentLocation) {
      setPOPPoint({ lat: currentLocation.lat, lng: currentLocation.lng });
    }
  };
  
  // Use Al Hofuf example data
  const useAlHofufExample = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await axios.get(
        `${API_BASE_URL}/example/alhofuf`
      );
      
      if (response.data.error) {
        setError(response.data.error);
        setNetworkData(null);
      } else {
        setNetworkData(response.data);
        
        // Set the POP point
        if (response.data.pop) {
          setPOPPoint({
            lat: response.data.pop.latitude,
            lng: response.data.pop.longitude
          });
        }
        
        // Set destinations based on the paths
        const newDestinations = response.data.paths.map(path => {
          const dest = path.destination;
          return {
            lat: dest.latitude,
            lng: dest.longitude
          };
        });
        
        setDestinations(newDestinations);
        
        // Center map on Al Hofuf
        if (mapRef) {
          mapRef.setView([25.3790, 49.5883], 12);
        }
      }
    } catch (error) {
      console.error('Error fetching Al Hofuf example:', error);
      setError(<bdi>"حدث خطأ أثناء جلب بيانات مثال الهفوف. يرجى المحاولة مرة أخرى."</bdi>);
      setNetworkData(null);
    } finally {
      setIsLoading(false);
    }
  };
  
  // Get a color for a path based on its index
  const getPathColor = (index) => {
    const colors = [
      '#3B82F6', // Blue
      '#10B981', // Green
      '#8B5CF6', // Purple
      '#F59E0B', // Yellow
      '#EF4444', // Red
      '#EC4899', // Pink
      '#6366F1', // Indigo
      '#14B8A6'  // Teal
    ];
    
    return colors[index % colors.length];
  };

  return (
    <div className="flex flex-col md:flex-row h-screen bg-gray-100">
      {/* Sidebar with network information */}
      <div className="w-full md:w-96 p-4 overflow-y-auto bg-white shadow-md z-10">
        <h1 className="text-xl font-bold mb-4 text-gray-800"><bdi>خريطة شبكة POP</bdi></h1>
        
        {/* Tab navigation */}
        <div className="tab-container">
          <div 
            className={`tab ${activeTab === 'pop' ? 'active' : ''}`}
            onClick={() => setActiveTab('pop')}
          >
            <bdi>إعداد الشبكة</bdi>
          </div>
          <div 
            className={`tab ${activeTab === 'paths' ? 'active' : ''}`}
            onClick={() => setActiveTab('paths')}
          >
            <bdi>المسارات</bdi>
          </div>
        </div>
        
        {/* Error message display */}
        {error && (
          <div className="error-alert">
            <AlertCircle className="w-5 h-5 error-alert-icon" />
            <div>{error}</div>
          </div>
        )}
        
        {/* Network setup tab */}
        {activeTab === 'pop' && (
          <div className="coordinate-selection-container">
            <h2 className="text-lg font-medium mb-4 text-gray-700"><bdi>إعداد نقطة الحضور (POP)</bdi></h2>
            
            {/* Current location button */}
            <button 
              className="coordinate-button w-full mb-6 flex items-center justify-center"
              onClick={handleGetCurrentLocation}
            >
              <LocateFixed className="w-4 h-4 mr-2" />
              <bdi>الحصول على الموقع الحالي</bdi>
            </button>
            
            {currentLocation && (
              <div className="bg-blue-50 p-3 rounded-md mb-4 text-sm">
                <div className="font-medium mb-2"><bdi>تم تحديد موقعك الحالي:</bdi></div>
                <div><bdi>خط العرض: {currentLocation.lat.toFixed(7)}</bdi></div>
                <div><bdi>خط الطول: {currentLocation.lng.toFixed(7)}</bdi></div>
                <div className="mt-2">
                  <button 
                    className="text-blue-600 underline"
                    onClick={useCurrentLocationAsPOP}
                  >
                    <bdi>استخدام كنقطة POP</bdi>
                  </button>
                </div>
              </div>
            )}
            
            {/* POP point manual input */}
            <div className="mb-6">
              <h3 className="font-medium mb-2 text-gray-700"><bdi>نقطة الحضور (POP)</bdi></h3>
              <p className="text-sm text-gray-600 mb-2"><bdi>انقر على الخريطة لتحديد نقطة POP أو أدخل الإحداثيات يدويًا</bdi></p>
              
              {/* Add POP selection button */}
              <button 
                className={`coordinate-button w-full mb-3 ${addingPOP ? 'active' : ''}`}
                onClick={() => {
                  setAddingPOP(!addingPOP);
                  setAddingDestination(false); // Disable destination selection if active
                }}
              >
                <Plus className="w-4 h-4 mr-2" />
                <bdi>{addingPOP ? 'إلغاء تحديد POP' : 'تحديد POP على الخريطة'}</bdi>
                
              </button>
              
              {addingPOP && (
                <div className="bg-blue-50 p-3 rounded-md mb-4 mx-auto text-center text-sm">
                  <bdi className="font-medium mx-auto ">انقر على الخريطة لتحديد النقطة</bdi>
                </div>
              )}
              
              <form onSubmit={handleManualPOPInput}>
                <div className="input-container">
                  <label className="block text-sm text-gray-600 mb-1">خط العرض</label>
                  <input 
                    type="text" 
                    name="popLat"
                    className="form-input" 
                    placeholder="25.3790"
                    defaultValue={popPoint?.lat?.toFixed(7) || ''}
                  />
                </div>
                <div className="input-container">
                  <label className="block text-sm text-gray-600 mb-1">خط الطول</label>
                  <input 
                    type="text" 
                    name="popLon"
                    className="form-input" 
                    placeholder="49.5883"
                    defaultValue={popPoint?.lng?.toFixed(7) || ''}
                  />
                </div>
                <button type="submit" className="coordinate-button">تحديث نقطة POP</button>
              </form>
            </div>
            
            {/* Destinations section */}
            <div className="mb-6">
              <h3 className="font-medium mb-2 text-gray-700">نقاط الوجهة</h3>
              
              <div className="flex gap-2 mb-4">
                <button 
                  className={`coordinate-button flex-1 ${addingDestination ? 'active' : ''}`}
                  onClick={() => {
                    setAddingDestination(!addingDestination);
                    setAddingPOP(false); // Disable POP selection if active
                  }}
                >
                  <Plus className="w-4 h-4 mr-2" />
                  {addingDestination ? 'إلغاء الإضافة' : 'إضافة وجهة'}
                </button>
                <button 
                  className="coordinate-button flex-1 red"
                  onClick={clearDestinations}
                  disabled={destinations.length === 0}
                >
                  <Trash className="w-4 h-4 mr-2" />
                  مسح الكل
                </button>
              </div>
              
              {addingDestination && (
                <div className="bg-blue-50 p-3 rounded-md mb-4 text-sm">
                  <div className="font-medium">انقر على الخريطة لإضافة نقطة وجهة</div>
                </div>
              )}
              
              {/* Destination chips */}
              {destinations.length > 0 && (
                <div className="mb-4">
                  <div className="font-medium mb-2 text-gray-700">الوجهات المحددة ({destinations.length})</div>
                  <div className="destinations-container">
                    {destinations.map((dest, index) => (
                      <div key={index} className="destination-chip">
                        <span className="destination-chip-number">{index + 1}</span>
                        <span>{dest.lat.toFixed(5)}, {dest.lng.toFixed(5)}</span>
                        <span 
                          className="destination-chip-remove"
                          onClick={() => removeDestination(index)}
                        >
                          ✕
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
              
           
              {/* Generate network button */}
              <button 
                className="coordinate-button green w-full flex items-center justify-center"
                onClick={fetchNetworkData}
                disabled={!popPoint || destinations.length === 0 || isLoading}
              >
                <Network className="w-4 h-4 mr-2" />
                {isLoading ? 'جاري إنشاء الشبكة...' : 'إنشاء شبكة POP'}
              </button>
              
              {/* Example button */}
              <button 
                className="coordinate-button w-full flex items-center justify-center mt-2"
                onClick={useAlHofufExample}
                disabled={isLoading}
              >
                <Send className="w-4 h-4 mr-2" />
                استخدام مثال الهفوف
              </button>
            </div>
            
            {/* Network statistics */}
            {networkData && networkData.statistics && (
              <div className="stats-container">
                <h3 className="stats-heading">إحصائيات الشبكة</h3>
                <div className="stats-grid">
                  <div className="stat-card">
                    <div className="stat-value">{networkData.statistics.uniqueTowersUsed}</div>
                    <div className="stat-label">أبراج فريدة</div>
                  </div>
                  <div className="stat-card">
                    <div className="stat-value">{networkData.statistics.totalDestinations}</div>
                    <div className="stat-label">إجمالي الوجهات</div>
                  </div>
                  <div className="stat-card">
                    <div className="stat-value">{networkData.statistics.totalDistance?.toFixed(1)}</div>
                    <div className="stat-label">المسافة الكلية (كم)</div>
                  </div>
                  <div className="stat-card">
                    <div className="stat-value">{(networkData.statistics.totalDistance / networkData.statistics.uniqueTowersUsed).toFixed(1)}</div>
                    <div className="stat-label">كم/برج</div>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
        
        {/* Paths tab */}
        {activeTab === 'paths' && (
          <>
            <div className="mb-6">
              <h2 className="text-lg font-medium mb-2 text-gray-700">قائمة المسارات</h2>
              {isLoading ? (
                <div className="text-center py-4">جاري التحميل...</div>
              ) : (
                <div className="space-y-1">
                  {networkData && networkData.paths && networkData.paths.length > 0 ? (
                    networkData.paths.map((path, index) => (
                      <div 
                        key={index}
                        className={`path-list-item ${selectedPath && selectedPath.index === index ? 'selected' : ''}`}
                        onClick={() => handlePathClick(path, index)}
                      >
                        <div 
                          className="selected-indicator" 
                          style={{ 
                            backgroundColor: getPathColor(index)
                          }}
                        ></div>
                        <span className="font-medium">
                          المسار {index + 1}: {path.towerCount} برج, {path.distance?.toFixed(2)} كم
                        </span>
                      </div>
                    ))
                  ) : (
                    <div className="text-center py-4 text-gray-500">
                      لا توجد مسارات لعرضها. يرجى إنشاء شبكة POP أولاً.
                    </div>
                  )}
                </div>
              )}
            </div>
            
            {/* Selected path details */}
            {selectedPath && (
              <div className="tower-info-panel">
                <h2 className="text-lg font-medium mb-4 text-gray-700">تفاصيل المسار {selectedPath.index + 1}</h2>
                
                <div className="info-row">
                  <div className="info-label">عدد الأبراج</div>
                  <div className="info-value">{selectedPath.towerCount}</div>
                </div>
                
                <div className="info-row">
                  <div className="info-label">المسافة</div>
                  <div className="info-value">{selectedPath.distance?.toFixed(2)} كم</div>
                </div>
                
                <div className="info-row">
                  <div className="info-label">الوجهة</div>
                  <div className="info-value">
                    {selectedPath.destination?.latitude?.toFixed(5)}, {selectedPath.destination?.longitude?.toFixed(5)}
                  </div>
                </div>
                
                <h3 className="font-medium mt-4 mb-2 text-gray-700">قائمة الأبراج في المسار</h3>
                
                <div className="space-y-1 mt-2">
                  {selectedPath.path && selectedPath.path.map((tower, idx) => (
                    <div 
                      key={idx}
                      className={`path-list-item ${selectedTower === tower ? 'selected' : ''}`}
                      onClick={() => handleTowerClick(tower)}
                    >
                      <div 
                        className="selected-indicator" 
                        style={{ 
                          backgroundColor: idx === 0 ? '#EF4444' : 
                                          idx === selectedPath.path.length - 1 ? '#8B5CF6' : 
                                          '#3B82F6'
                        }}
                      ></div>
                      <span className="font-medium">
                        {idx === 0 ? 'نقطة POP' : 
                         idx === selectedPath.path.length - 1 ? 'الوجهة' : 
                         (tower.siteName || tower.tawalId || `Tower ${idx}`)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
            
            {/* Selected tower details */}
            {selectedTower && !selectedPath && (
              <div className="tower-info-panel">
                <h2 className="text-lg font-medium mb-4 text-gray-700">تفاصيل البرج</h2>
                
                <div className="info-row">
                  <div className="info-label">اسم الموقع</div>
                  <div className="info-value">{selectedTower.siteName || 'غير متوفر'}</div>
                </div>
                
                <div className="info-row">
                  <div className="info-label">رقم التعريف</div>
                  <div className="info-value">{selectedTower.tawalId || 'غير متوفر'}</div>
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
            
            {/* Network paths */}
            {networkData && networkData.paths && networkData.paths.map((pathData, pathIndex) => {
              if (!pathData.path || pathData.path.length < 2) return null;
              
              const positions = pathData.path.map(tower => [tower.latitude, tower.longitude]);
              const isSelected = selectedPath && selectedPath.index === pathIndex;
              
              return (
                <Polyline 
                  key={`path-${pathIndex}`}
                  positions={positions}
                  pathOptions={{ 
                    color: getPathColor(pathIndex), 
                    weight: isSelected ? 5 : 3, 
                    opacity: isSelected ? 1 : 0.7,
                    dashArray: '5, 8',
                    // Use dashOffset to enable animation compatibility
                    dashOffset: 0
                  }}
                  className="pop-path" // Use className for animation
                  eventHandlers={{
                    click: () => handlePathClick(pathData, pathIndex)
                  }}
                />
              );
            })}
            
            {/* Tower markers */}
            {networkData && networkData.paths && networkData.paths.flatMap((pathData, pathIndex) => {
              if (!pathData.path || pathData.path.length === 0) return [];
              
              // Skip the first (POP) and last (destination) towers as they'll be shown separately
              return pathData.path.slice(1, -1).map((tower, towerIndex) => {
                // Check if this tower has already been rendered (shared towers)
                const towerKey = tower.tawalId || `${tower.latitude}-${tower.longitude}`;
                
                // Check if it's virtual or real
                const isVirtual = tower.tawalId && tower.tawalId.includes("VIRTUAL");
                
                return (
                  <Marker 
                    key={`tower-${pathIndex}-${towerIndex}-${towerKey}`}
                    position={[tower.latitude, tower.longitude]} 
                    icon={towerIcon}
                    eventHandlers={{
                      click: (e) => {
                        e.originalEvent.stopPropagation();
                        handleTowerClick(tower);
                      }
                    }}
                  >
                    <Tooltip 
                      permanent={false}
                      direction="top"
                      className="tower-details"
                    >
                      <div>
                        <strong>{tower.siteName || tower.tawalId || "Tower"}</strong>
                        <br/>
                        {tower.tawalId || `${tower.latitude.toFixed(5)}, ${tower.longitude.toFixed(5)}`}
                      </div>
                    </Tooltip>
                  </Marker>
                );
              });
            })}
            
            {/* POP marker */}
            {popPoint && (
              <Marker position={[popPoint.lat, popPoint.lng]} icon={popIcon}>
                <Tooltip 
                  permanent={false}
                  direction="top"
                  className="tower-details"
                >
                  <div>
                    <strong>نقطة الحضور (POP)</strong>
                    <br/>
                    {popPoint.lat.toFixed(7)}, {popPoint.lng.toFixed(7)}
                  </div>
                </Tooltip>
              </Marker>
            )}
            
            {/* Destination markers */}
            {destinations.map((dest, index) => (
              <Marker key={`dest-${index}`} position={[dest.lat, dest.lng]} icon={destIcon}>
                <Tooltip 
                  permanent={false}
                  direction="top"
                  className="tower-details"
                >
                  <div>
                    <strong>وجهة {index + 1}</strong>
                    <br/>
                    {dest.lat.toFixed(7)}, {dest.lng.toFixed(7)}
                  </div>
                </Tooltip>
              </Marker>
            ))}
            
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
            
            {/* Map event handlers */}
            <POPSelector 
              active={addingPOP}
              setPOPPoint={setPOPPoint}
            />
            
            <DestinationSelector 
              active={addingDestination}
              addDestination={addDestination}
            />
            
            <LocationComponent 
              map={mapRef}
              setCurrentLocation={setCurrentLocation}
            />
            
            <ZoomControl networkData={networkData} />
          </MapContainer>
        </div>
      </div>
    </div>
  );
}

export default POPMapPage;