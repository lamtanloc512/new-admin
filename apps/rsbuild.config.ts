import { defineConfig } from '@rsbuild/core';
import { pluginReact } from '@rsbuild/plugin-react';

export default defineConfig({
  plugins: [pluginReact()],
  dev: {
    setupMiddlewares: (middlewares) => {
      middlewares.unshift(async (req, res, next) => {
        const url = req.url ?? '/';

        // Bỏ qua request mà Rsbuild phải xử lý (bundle, HMR, static build)
        if (
          url.startsWith('/src') ||
          url.startsWith('/@rsbuild') ||
          url.startsWith('/__rsbuild_hmr') ||
          url.startsWith('/assets') ||
          url.startsWith('/node_modules')
        ) {
          return next();
        }

        try {
          const upstreamUrl = `http://localhost:9090${url}`;
          const headers: Record<string, string> = {};
          if (req.headers.cookie) {
            headers.cookie = req.headers.cookie;
          }
          const upstreamResp = await fetch(upstreamUrl, {
            redirect: 'manual', // giữ nguyên 302 để forward Location
            headers,
          });

          // Forward status
          res.statusCode = upstreamResp.status;

          // Forward headers (bao gồm Set-Cookie, Location)
          upstreamResp.headers.forEach((value, key) => {
            if (key.toLowerCase() !== 'content-encoding') {
              if (key.toLowerCase() === 'set-cookie') {
                // Modify cookie domain for localhost proxying
                const modifiedCookies = Array.isArray(value) ? value : [value];
                const newCookies = modifiedCookies.map((cookie) =>
                  cookie.replace(/domain=[^;]+/i, 'domain=localhost'),
                );
                res.setHeader(
                  key,
                  newCookies.length === 1 ? newCookies[0] : newCookies,
                );
              } else {
                res.setHeader(key, value);
              }
            }
          });

          // Nếu redirect (302/301), không cần body
          if (upstreamResp.status >= 300 && upstreamResp.status < 400) {
            res.end();
            return;
          }

          // Nếu HTML → inject script
          if (upstreamResp.headers.get('content-type')?.includes('text/html')) {
            let body = await upstreamResp.text();
            const injection = `<script type="module" src="/src/index.tsx"></script>`;
            if (body.includes('</body>')) {
              body = body.replace(/<\/body>/i, `${injection}</body>`);
            } else if (body.includes('</html>')) {
              body = body.replace(/<\/html>/i, `${injection}</html>`);
            } else {
              body += injection;
            }
            // Ensure root div exists for React mounting
            if (!body.includes('id="root"')) {
              if (body.includes('<body>')) {
                body = body.replace(/<body>/i, '<body><div id="root"></div>');
              } else {
                body = '<div id="root"></div>' + body;
              }
            }
            res.setHeader('content-length', Buffer.byteLength(body).toString());
            res.end(body);
          } else {
            // Handle binary/static resources properly
            const buffer = await upstreamResp.arrayBuffer();
            res.end(Buffer.from(buffer));
          }
        } catch (err) {
          console.error('Proxy error:', err);
          res.statusCode = 500;
          res.end('Proxy to EzyPlatform failed');
        }
      });
    },
  },
});
