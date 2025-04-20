// components/TowerMap/TowerList.jsx
import React from 'react';

function TowerList({ towerData, isLoading, selectedTower, handleTowerClick, isVirtualTower }) {
  return (
    <div className="mb-6">
      <h2 className="text-lg font-medium mb-2 text-gray-700">قائمة الأبراج</h2>
      {isLoading ? (
        <div className="text-center py-4">Loading...</div>
      ) : (
        <div className="space-y-1">
          {towerData && towerData.path && towerData.path.length > 0 ? (
            towerData.path.map((tower, index) => (
              <div 
                key={index}
                className={`tower-list-item ${selectedTower === tower ? 'selected' : ''}`}
                onClick={() => handleTowerClick(tower)}
              >
                <div 
                  className="selected-indicator" 
                  style={{ 
                    backgroundColor: isVirtualTower(tower) ? '#4ADE80' : '#3B82F6'
                  }}
                ></div>
                <span className="font-medium">{tower.siteName}</span>
              </div>
            ))
          ) : (
            <div className="text-center py-4 text-gray-500">
              لا توجد أبراج لعرضها. الرجاء تحديد نقاط البداية والنهاية.
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default TowerList;