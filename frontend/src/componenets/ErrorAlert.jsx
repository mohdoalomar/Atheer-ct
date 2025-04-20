// components/TowerMap/ErrorAlert.jsx
import React from 'react';
import { AlertCircle } from "lucide-react";

function ErrorAlert({ error }) {
  if (!error) return null;
  
  return (
    <div className="error-alert">
      <AlertCircle className="w-5 h-5 error-alert-icon" />
      <div>{error}</div>
    </div>
  );
}

export default ErrorAlert;