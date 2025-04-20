// components/TowerMap/CoordinateSelection.jsx
import React from 'react';
import CurrentLocation from './CurrentLocation';
import PointSelector from './PointSelector';
import DistanceIndicator from './DistanceIndicator';
import CoordinateForm from './CoordinateForm';

function CoordinateSelection({ 
  currentLocation,
  handleGetCurrentLocation,
  useCurrentLocationAsPoint,
  selectingStart,
  setSelectingStart,
  distance,
  MAX_ALLOWED_DISTANCE,
  error,
  startPoint,
  endPoint,
  handleManualStartInput,
  handleManualEndInput
}) {
  return (
    <div className="coordinate-selection-container">
      <h2 className="text-lg font-medium mb-4 text-gray-700">تحديد الإحداثيات</h2>
      
      <CurrentLocation 
        currentLocation={currentLocation}
        handleGetCurrentLocation={handleGetCurrentLocation}
        useCurrentLocationAsPoint={useCurrentLocationAsPoint}
        selectingStart={selectingStart}
      />
      
      <div className="mb-4">
        <PointSelector 
          selectingStart={selectingStart}
          setSelectingStart={setSelectingStart}
        />
        
        <p className="text-sm text-gray-600 mb-4">انقر على الخريطة لتحديد النقاط أو أدخل الإحداثيات يدويًا</p>
        
        <DistanceIndicator 
          distance={distance}
          maxAllowedDistance={MAX_ALLOWED_DISTANCE}
          error={error}
        />
        
        <CoordinateForm 
          type="start"
          point={startPoint}
          handleSubmit={handleManualStartInput}
        />
        
        <CoordinateForm 
          type="end"
          point={endPoint}
          handleSubmit={handleManualEndInput}
        />
      </div>
    </div>
  );
}

export default CoordinateSelection;