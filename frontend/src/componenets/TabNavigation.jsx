// components/TowerMap/TabNavigation.jsx
import React from 'react';

function TabNavigation({ activeTab, setActiveTab }) {
  return (
    <div className="tab-container">
      <div 
        className={`tab ${activeTab === 'towers' ? 'active' : ''}`}
        onClick={() => setActiveTab('towers')}
      >
        الأبراج
      </div>
      <div 
        className={`tab ${activeTab === 'coordinates' ? 'active' : ''}`}
        onClick={() => setActiveTab('coordinates')}
      >
        تحديد الإحداثيات
      </div>
    </div>
  );
}

export default TabNavigation;