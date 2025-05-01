import React from 'react';
import { MapPin, Navigation } from "lucide-react";

function PointSelector({ selectingStart, setSelectingStart }) {
  return (
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
  );
}

export default PointSelector;