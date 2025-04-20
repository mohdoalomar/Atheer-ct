// components/TowerMap/CoordinateSelector.jsx
import React from 'react';
import { useMapEvents } from 'react-leaflet';

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

export default CoordinateSelector;