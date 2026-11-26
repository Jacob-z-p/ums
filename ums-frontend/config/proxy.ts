export function createProxy(basePath: string) {
  const apiPath = `${basePath}api`;
  return {
    [apiPath]: {
      target: process.env.UMS_BACKEND_URL || 'http://localhost:8080',
      changeOrigin: true,
      ...(basePath === '/' ? {} : { pathRewrite: { [`^${apiPath}`]: '/api' } }),
    },
  };
}
