window.__ENV__ = {
  API_BASE_URL: "/api/v1",
  IS_PRODUCTION: window.location.hostname !== 'localhost' && window.location.port !== '3000'
};
