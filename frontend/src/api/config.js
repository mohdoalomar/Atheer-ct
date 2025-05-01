// API configuration file
// In Docker environment, we use the service name instead of localhost
// This works because the frontend will be running inside a Docker container
// and communicating with the backend via Docker's internal network
const API_BASE_URL = process.env.NODE_ENV === 'production' 
  ? '' // Empty base URL for production as nginx will handle proxying
  : 'http://localhost:8081';

export default API_BASE_URL;