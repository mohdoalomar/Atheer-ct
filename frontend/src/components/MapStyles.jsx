// components/TowerMap/MapStyles.jsx
import React, { useEffect } from 'react';

const MapStyles = () => {
  useEffect(() => {
    // Add custom styles to the document
    const customStyles = `
    .virtual-tower-icon {
      filter: hue-rotate(120deg);
    }

    .start-marker-icon {
      filter: hue-rotate(180deg);
    }

    .end-marker-icon {
      filter: hue-rotate(280deg);
    }

    .current-location-icon {
      filter: hue-rotate(60deg) brightness(1.2);
    }

    .tower-path {
      animation: dash 30s linear infinite;
    }

    .custom-path {
      stroke-dasharray: 5, 10;
      animation: dash 20s linear infinite reverse;
    }

    @keyframes dash {
      to {
        stroke-dashoffset: 1000;
      }
    }

    .tower-details {
      font-size: 12px;
      background-color: rgba(255, 255, 255, 0.9);
      border-radius: 6px;
      padding: 4px 8px;
      box-shadow: 0 2px 5px rgba(0, 0, 0, 0.15);
      border: 1px solid rgba(0, 0, 0, 0.1);
    }

    .custom-zoom-controls {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .custom-zoom-controls button {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: white;
      border-radius: 4px;
      box-shadow: 0 2px 5px rgba(0, 0, 0, 0.15);
      border: none;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .custom-zoom-controls button:hover {
      background-color: #f0f0f0;
    }

    .tower-info-panel {
      background-color: white;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      padding: 16px;
      max-height: 400px;
      overflow-y: auto;
    }

    .info-row {
      display: flex;
      padding: 8px 0;
      border-bottom: 1px solid #eee;
    }

    .info-row:last-child {
      border-bottom: none;
    }

    .info-label {
      font-weight: 500;
      color: #666;
      width: 120px;
      flex-shrink: 0;
    }

    .info-value {
      flex-grow: 1;
    }

    .map-container {
      height: 100vh;
      width: 100%;
      position: relative;
    }

    .selected-indicator {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      margin-right: 8px;
    }

    .tower-list-item {
      cursor: pointer;
      transition: background-color 0.2s ease;
      padding: 8px 12px;
      border-radius: 4px;
      display: flex;
      align-items: center;
    }

    .tower-list-item:hover {
      background-color: #f0f7ff;
    }

    .tower-list-item.selected {
      background-color: #e0f0ff;
    }

    .coordinate-selection-container {
      background-color: white;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      padding: 16px;
      margin-top: 16px;
    }

    .tab-container {
      display: flex;
      border-bottom: 1px solid #eee;
      margin-bottom: 12px;
    }

    .tab {
      padding: 8px 16px;
      cursor: pointer;
      border-bottom: 2px solid transparent;
      transition: all 0.2s ease;
    }

    .tab.active {
      border-bottom-color: #3B82F6;
      color: #3B82F6;
      font-weight: 500;
    }

    .input-container {
      margin-bottom: 12px;
    }

    .coordinate-button {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 8px 16px;
      background-color: #f0f7ff;
      border: 1px solid #d1e0ff;
      border-radius: 4px;
      transition: all 0.2s ease;
      cursor: pointer;
      font-weight: 500;
      margin-bottom: 8px;
    }

    .coordinate-button:hover {
      background-color: #e0f0ff;
    }

    .coordinate-button.active {
      background-color: #3B82F6;
      color: white;
      border-color: #3B82F6;
    }

    .form-input {
      width: 100%;
      padding: 8px 12px;
      border: 1px solid #ddd;
      border-radius: 4px;
      margin-bottom: 8px;
      font-size: 14px;
    }

    .error-alert {
      background-color: #FEF2F2;
      color: #B91C1C;
      border: 1px solid #FCA5A5;
      border-radius: 8px;
      padding: 12px;
      margin-bottom: 16px;
      display: flex;
      align-items: flex-start;
      gap: 8px;
    }

    .error-alert-icon {
      margin-top: 2px;
      flex-shrink: 0;
    }

    .distance-indicator {
      background-color: #EFF6FF;
      border: 1px solid #BFDBFE;
      border-radius: 8px;
      padding: 12px;
      margin-top: 12px;
      font-size: 14px;
    }

    .distance-label {
      font-weight: 500;
      margin-bottom: 4px;
    }

    .distance-value {
      font-size: 18px;
      font-weight: 600;
      color: #3B82F6;
    }

    .distance-warning {
      font-size: 12px;
      color: #B45309;
      margin-top: 4px;
    }
    `;

    const styleSheet = document.createElement("style");
    styleSheet.textContent = customStyles;
    document.head.appendChild(styleSheet);

    // Clean up on component unmount
    return () => {
      document.head.removeChild(styleSheet);
    };
  }, []);

  return null;
};

export default MapStyles;