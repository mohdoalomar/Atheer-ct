// components/TowerMap/LocationComponent.jsx
import React from 'react';
import { useMapEvents } from 'react-leaflet';

// Component to handle current location functionality
function LocationComponent({ map, setCurrentLocation }) {
  const handleLocationFound = (e) => {
    const { lat, lng } = e.latlng;
    const accuracy = e.accuracy;
    const point = { lat, lng, accuracy };
    
    setCurrentLocation(point);
    map?.setView([lat, lng], map.getZoom());
  };
  
  useMapEvents({
    locationfound: handleLocationFound,
  });
  
  return null;
}

export default LocationComponent;