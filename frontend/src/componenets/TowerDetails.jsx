// components/TowerMap/TowerDetails.jsx
import React from 'react';

function TowerDetails({ selectedTower }) {
  if (!selectedTower) return null;

  return (
    <div className="tower-info-panel">
      <h2 className="text-lg font-medium mb-4 text-gray-700">تفاصيل البرج</h2>
      
      <div className="info-row">
        <div className="info-label">اسم الموقع</div>
        <div className="info-value">{selectedTower.siteName}</div>
      </div>
      
      <div className="info-row">
        <div className="info-label">رقم التعريف</div>
        <div className="info-value">{selectedTower.tawalId}</div>
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
  );
}

export default TowerDetails;