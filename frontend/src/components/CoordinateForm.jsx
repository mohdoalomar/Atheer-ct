// components/TowerMap/CoordinateForm.jsx
import React from 'react';

function CoordinateForm({ 
  type, 
  point, 
  handleSubmit
}) {
  const isStart = type === 'start';
  const title = isStart ? 'نقطة البداية' : 'نقطة النهاية';
  const placeholder = isStart ? { lat: "25.5493", lng: "48.3327" } : { lat: "25.4908", lng: "48.1631" };

  return (
    <div className="mb-6">
      <h3 className="font-medium mb-2 text-gray-700">{title}</h3>
      <form onSubmit={handleSubmit}>
        <div className="input-container">
          <label className="block text-sm text-gray-600 mb-1">خط العرض</label>
          <input 
            type="text" 
            name={`${type}Lat`}
            className="form-input" 
            placeholder={placeholder.lat}
            defaultValue={point?.lat?.toFixed(7) || ''}
          />
        </div>
        <div className="input-container">
          <label className="block text-sm text-gray-600 mb-1">خط الطول</label>
          <input 
            type="text" 
            name={`${type}Lon`}
            className="form-input" 
            placeholder={placeholder.lng}
            defaultValue={point?.lng?.toFixed(7) || ''}
          />
        </div>
        <button type="submit" className="coordinate-button">تحديث {title}</button>
      </form>
    </div>
  );
}

export default CoordinateForm;