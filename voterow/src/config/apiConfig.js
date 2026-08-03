const API_CONFIG = {
  // When using Vite dev server, proxy forwards /api to backend automatically.
  // In production, set VITE_API_BASE_URL to the backend origin.
  BASE_URL: import.meta.env?.VITE_API_BASE_URL || '',
  ENDPOINTS: {
    AUTH: '/api/auth',
    ADMIN: '/api/admin',
    VOTING: '/api/voting',
    CAMPAIGN: '/api/campaign-materials',
    PARTICIPANT_CAMPAIGN: '/api/participant/campaign-materials'
  }
};

export const getApiUrl = (endpoint) => `${API_CONFIG.BASE_URL}${endpoint}`;
export default API_CONFIG;