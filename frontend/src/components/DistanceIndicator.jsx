// components/TowerMap/DistanceIndicator.jsx
import React from 'react';

function DistanceIndicator({ distance, maxAllowedDistance, error }) {
  if (distance <= 0) return null;
  
  return (
    <div className="distance-indicator">
      <div className="distance-label">المسافة بين النقطتين:</div>
      <div className="distance-value">{distance.toFixed(2)} كم</div>
      {distance > maxAllowedDistance && error != null && (
        <div className="distance-warning">
          تنبيه: المسافة تتجاوز الحد الأقصى المسموح به ({maxAllowedDistance} كم)
        </div>
      )}
    </div>
  );
}

export default DistanceIndicator;