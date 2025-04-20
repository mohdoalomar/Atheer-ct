// components/TowerMap/MapPaths.jsx
import React from 'react';
import { Polyline } from 'react-leaflet';

function MapPaths({ polylinePath, startPoint, endPoint }) {
  return (
    <>
      {/* Tower path */}
      {polylinePath && polylinePath.length > 0 && (
        <Polyline 
          positions={polylinePath}
          pathOptions={{ 
            color: '#3B82F6', 
            weight: 3, 
            opacity: 0.8,
            dashArray: '5, 8',
            className: 'tower-path'
          }}
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
            className: 'custom-path'
          }}
        />
      )}
    </>
  );
}

export default MapPaths;