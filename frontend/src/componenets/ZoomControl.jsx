// components/TowerMap/ZoomControl.jsx
import React from 'react';
import { useMap } from 'react-leaflet';
import { ZoomIn, ZoomOut, RefreshCw } from "lucide-react";
import L from 'leaflet';

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

export default ZoomControl;