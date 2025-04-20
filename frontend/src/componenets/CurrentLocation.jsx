// components/TowerMap/CurrentLocation.jsx
import React from 'react';
import { LocateFixed } from "lucide-react";

function CurrentLocation({ 
  currentLocation, 
  handleGetCurrentLocation, 
  useCurrentLocationAsPoint, 
  selectingStart 
}) {
  return (
    <>
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
    </>
  );
}

export default CurrentLocation;