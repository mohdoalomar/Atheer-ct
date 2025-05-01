// components/TowerMap/index.jsx
import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import axios from 'axios';
import API_BASE_URL from '../api/config';
// Import Components
import MapStyles from './MapStyles';

import ZoomControl from './ZoomControl';
import CoordinateSelector from './CoordinateSelector';
import LocationComponent from './LocationComponent';
import MapMarkers from './MapMarkers';
import MapPaths from './MapPaths';
import TowerList from './TowerList';
import TowerDetails from './TowerDetails';
import CoordinateInput from './CoordinateSelection';
import ErrorAlert from './ErrorAlert';


// Fix Leaflet default icon issue
delete L.Icon.Default.prototype._getIconUrl;

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
          , {
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
      <MapStyles />
      
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
        {error && <ErrorAlert message={error} />}
        
        {/* Tower list tab */}
        {activeTab === 'towers' && (
          <>
            <TowerList 
              towerData={towerData}
              isLoading={isLoading}
              selectedTower={selectedTower}
              handleTowerClick={handleTowerClick}
              isVirtualTower={isVirtualTower}
            />
            
            {/* Selected tower details */}
            {selectedTower && <TowerDetails tower={selectedTower} />}
          </>
        )}
        
        {/* Coordinate selection tab */}
        {activeTab === 'coordinates' && (
          <CoordinateInput 
            startPoint={startPoint}
            endPoint={endPoint}
            setStartPoint={setStartPoint}
            setEndPoint={setEndPoint}
            selectingStart={selectingStart}
            setSelectingStart={setSelectingStart}
            currentLocation={currentLocation}
            handleGetCurrentLocation={handleGetCurrentLocation}
            useCurrentLocationAsPoint={useCurrentLocationAsPoint}
            handleManualStartInput={handleManualStartInput}
            handleManualEndInput={handleManualEndInput}
            distance={distance}
            MAX_ALLOWED_DISTANCE={MAX_ALLOWED_DISTANCE}
            error={error}
          />
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
            
            <MapPaths 
              polylinePath={polylinePath}
              startPoint={startPoint}
              endPoint={endPoint}
            />
            
            <MapMarkers 
              towerData={towerData}
              startPoint={startPoint}
              endPoint={endPoint}
              setSelectedTower={setSelectedTower}
              isVirtualTower={isVirtualTower}
            />
            
            <CoordinateSelector 
              selectingStart={selectingStart}
              startPoint={startPoint}
              endPoint={endPoint}
              setStartPoint={setStartPoint}
              setEndPoint={setEndPoint}
            />
            
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