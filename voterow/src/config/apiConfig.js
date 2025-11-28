const API_CONFIG = {
  // Default backend URL. Configure via VITE_API_BASE_URL or REACT_APP_API_URL when starting the frontend.
  // Fallback to localhost:8083 which is the port the backend is currently listening on in this workspace.
  BASE_URL: import.meta.env?.VITE_API_BASE_URL || import.meta.env.REACT_APP_API_URL || 'http://localhost:8083',
  ENDPOINTS: {
    AUTH: '/api/auth',
    ADMIN: '/api/admin',
    VOTING: '/api/voting',
    CAMPAIGN: '/api/campaign'
  }
};

export const getApiUrl = (endpoint) => `${API_CONFIG.BASE_URL}${endpoint}`;
export default API_CONFIG;