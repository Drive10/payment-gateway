import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token');
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    if (config.data) {
      const payloadString = JSON.stringify(config.data);
      const timestamp = Date.now().toString();
      
      config.headers['X-Timestamp'] = timestamp;
      
      const dataForSig = `${timestamp}.${payloadString}`;
      
      if (typeof window !== 'undefined' && window.crypto) {
        window.crypto.subtle.digest('SHA-256', new TextEncoder().encode(dataForSig))
          .then(hash => {
            const hashArray = Array.from(new Uint8Array(hash));
            const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
            config.headers['X-Signature'] = hashHex;
          })
          .catch(() => {
            config.headers['X-Signature'] = '';
          });
      }
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('auth_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
