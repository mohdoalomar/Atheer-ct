// components/TowerMap/MapMarkers.jsx
import React from 'react';
import { Marker, Tooltip } from 'react-leaflet';
import { towerIcon, virtualTowerIcon, startIcon, endIcon } from './CustomIcons';

function MapMarkers({ towerData, startPoint, endPoint, setSelectedTower, isVirtualTower }) {
  return (
    <>
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
    </>
  );
}

export default MapMarkers;