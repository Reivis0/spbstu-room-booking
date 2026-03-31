import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || '/api/v1';

console.log('API Base URL:', API_BASE_URL);

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: false,
});

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('[API Request] Token added to headers:', `Bearer ${token.substring(0, 20)}...`);
    } else {
      console.warn('[API Request] No token found in localStorage');
    }
    const fullUrl = config.baseURL ? `${config.baseURL}${config.url}` : config.url;
    const authHeader = config.headers.Authorization;
    const authHeaderStr = typeof authHeader === 'string' ? authHeader.substring(0, 30) + '...' : 'none';
    console.log(`[API Request] ${config.method?.toUpperCase()} ${fullUrl}`, {
      hasAuth: !!authHeader,
      authHeader: authHeaderStr,
    });
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      console.error('[API Response] Unauthorized/Forbidden:', {
        status: error.response?.status,
        url: error.config?.url,
        hasToken: !!localStorage.getItem('accessToken'),
      });
      
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      
      const currentPath = window.location.pathname;
      if (currentPath !== '/login' && !currentPath.startsWith('/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);


