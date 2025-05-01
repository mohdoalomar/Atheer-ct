import { useEffect, useState } from 'react'
import POPMapPage from './components/POPMap' 
import TowerMapPage from './components/TowerMapPage'
import OldMap from './components/Map';
import { BrowserRouter as Router, Routes, Route, Link, useNavigate} from 'react-router-dom';
import { Home } from 'lucide-react';
import Login from './components/Login';
import Register from './components/Register';
import API_BASE_URL from './api/config';
import { LanguageProvider } from './components/LanguageContext';
import Homepage from './components/Homepage';

// Create a separate component for the app content that uses navigation
function AppContent() {
  const navigate = useNavigate();

  useEffect(() => {
    const checkAuth = async () => {
      // Skip auth check if already on login or register page
      if (window.location.pathname === '/login' || window.location.pathname === '/register') {
        return;
      }
      
      try {
        const response = await fetch(`${API_BASE_URL}/api/users/check-auth`, {
          method: 'GET',
          credentials: 'include', // This ensures cookies are sent with the request
          headers: {
            'Content-Type': 'application/json',
            // Include any auth tokens if you're using token-based auth
            // 'Authorization': `Bearer ${localStorage.getItem('token')}` 
          }
        });
        
        if (!response.ok) {
          console.error('Auth check failed:', response.status);
          navigate('/login');
        }
      } catch (error) {
        console.error('Auth check error:', error);
        navigate('/login');
      }
    };
    
    checkAuth();
  }, [navigate]);

  return (
    <>
      {/* Navigation Menu */}
      <div className="fixed top-0 right-0 z-[999999] p-4">
        <div className="bg-white shadow-lg rounded-lg p-2">
          <Link to="/" className="block p-2 hover:bg-gray-100 rounded">
            <Home className="w-6 h-6" />
          </Link>
          <div className="mt-2 border-t pt-2">
            <Link to="/tower-map" className="block p-2 text-sm hover:bg-gray-100 rounded">
              خريطة الأبراج
            </Link>
            <Link to="/pop-map" className="block p-2 text-sm hover:bg-gray-100 rounded">
              خريطة شبكة POP
            </Link>
          </div>
        </div>
      </div>
      
      {/* Routes */}
      <Routes>
      <Route path="/" element={<Homepage />} />
        <Route path="/tower-map" element={<TowerMapPage />} />
        <Route path="/old-map" element={<OldMap />} />
        <Route path="/pop-map" element={<POPMapPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
      </Routes>
    
    </>
  );
}

function App() {
  return (
    <Router>
    <LanguageProvider>
      <AppContent />
      </LanguageProvider>
    </Router>
  )
}

export default App
