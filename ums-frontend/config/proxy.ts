export default {
  '/api': {
    target: process.env.UMS_BACKEND_URL || 'http://localhost:8080',
    changeOrigin: true,
  },
};
