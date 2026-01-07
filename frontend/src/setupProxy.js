const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://localhost:8081',
      changeOrigin: true,
      secure: false,
      logLevel: 'debug',
      pathRewrite: function (path, req) {
        return '/api' + path;
      },
      onProxyReq: (proxyReq, req, res) => {
        const targetUrl = `http://localhost:8081/api${req.url}`;
        console.log(`[Proxy] ${req.method} ${req.url} -> ${targetUrl} (after pathRewrite: /api${req.url})`);
      },
    })
  );
};

