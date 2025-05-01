import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import API_BASE_URL from '../api/config';
import { useLanguage } from './LanguageContext';

const Login = () => {
  const [credentials, setCredentials] = useState({
    username: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [showError, setShowError] = useState(false);
  const navigate = useNavigate();
  const { translations, language } = useLanguage();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setCredentials(prev => ({
      ...prev,
      [name]: value
    }));
  };
  const formData = new URLSearchParams();
  formData.append('username', credentials.username.toLowerCase());
  formData.append('password', credentials.password);
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setShowError(false);
    try {
      const response = await axios.post(`${API_BASE_URL}/api/auth/login`, formData , {
        withCredentials: true
      });
      localStorage.setItem('username', credentials.username);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || translations.login.loginFailed);
      setShowError(true);
      console.error('Login error:', err);
    }
  };

  useEffect(() => {
    if (showError) {
      const timer = setTimeout(() => setShowError(false), 3500);
      return () => clearTimeout(timer);
    }
  }, [showError]);

  return (

  <div className="min-h-screen flex items-center justify-center bg-gray-50" dir={language === 'ar' ? 'rtl' : 'ltr'}>
      <div className="max-w-md w-full space-y-8 p-10 bg-white rounded-xl shadow-md m-4 animate-fade-in">
        <div className="text-center">
          <h2 className="mt-6 text-3xl font-extrabold text-gray-900">{translations.login.title}</h2>
        </div>
        {showError && error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-2 transition-opacity duration-500 animate-fade-in" role="alert">
            <span className="block sm:inline">{error}</span>
          </div>
        )}
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="rounded-md shadow-sm -space-y-px">
            <div>
              <label htmlFor="username" className="sr-only">{translations.login.username}</label>
              <input
                id="username"
                name="username"
                type="text"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder={translations.login.username}
                value={credentials.username}
                onChange={handleChange}
              />
            </div>
            <div>
              <label htmlFor="password" className="sr-only">{translations.login.password}</label>
              <input
                id="password"
                name="password"
                type="password"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
                placeholder={translations.login.password}
                value={credentials.password}
                onChange={handleChange}
              />
            </div>
          </div>
          <div>
            <button
              type="submit"
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              {translations.login.signIn}
            </button>
          </div>
          <div className="text-sm text-center">
            <p>{translations.login.dontHaveAccount} 
              <Link to="/register" className="font-medium text-indigo-600 hover:text-indigo-500 ml-1">
                {translations.login.registerHere}
              </Link>
            </p>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Login;