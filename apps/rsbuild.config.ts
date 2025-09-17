import { defineConfig } from '@rsbuild/core';
import { pluginReact } from '@rsbuild/plugin-react';

export default defineConfig({
  plugins: [pluginReact()],
  source: {
    entry: {
      plugin: './src/index.ts',
    },
  },
  output: {
    filenameHash: false,
  },
  dev: {
    liveReload: true,
    progressBar: true,
    hmr: true,
    setupMiddlewares: (middlewares) => {
      middlewares.unshift(async (req, res, next) => {
        const url = req.url ?? '/';
        if (
          url.startsWith('/@rsbuild') || // runtime
          url.startsWith('/rsbuild-hmr') || // HMR
          url.startsWith('/assets') || // static assets
          url.startsWith('/plugin') ||
          url.startsWith('/static/')
        ) {
          if (url.endsWith('.js')) {
            res.setHeader(
              'Content-Type',
              'application/javascript; charset=utf-8',
            );
          }
          return next();
        }

        try {
          const upstreamUrl = `http://localhost:9090${url}`;
          const headers: Record<string, string> = {};
          if (req.headers.cookie) {
            headers.cookie = req.headers.cookie;
          }
          const upstreamResp = await fetch(upstreamUrl, {
            redirect: 'manual',
            headers,
          });

          res.statusCode = upstreamResp.status;

          upstreamResp.headers.forEach((value, key) => {
            if (key.toLowerCase() !== 'content-encoding') {
              res.setHeader(key, value);
            }
          });

          // Nếu redirect 30x
          if (upstreamResp.status >= 300 && upstreamResp.status < 400) {
            res.end();
            return;
          }

          // Nếu là HTML → inject script dev
          if (upstreamResp.headers.get('content-type')?.includes('text/html')) {
            let body = await upstreamResp.text();

            // Fetch Rsbuild's HTML to get the scripts and styles
            const rsbuildResp = await fetch('http://localhost:3000/plugin');
            const rsbuildHtml = await rsbuildResp.text();

            // Extract scripts and links from Rsbuild's HTML
            const scriptRegex = /<script[^>]*>[\s\S]*?<\/script>/gi;
            const linkRegex = /<link[^>]*>/gi;
            const scripts = rsbuildHtml.match(scriptRegex) || [];
            const links = rsbuildHtml.match(linkRegex) || [];

            const injection = [...links, ...scripts].join('\n');

            if (body.includes('</body>')) {
              body = body.replace(/<\/body>/i, `${injection}</body>`);
            } else {
              body += injection;
            }

            // đảm bảo tồn tại <div id="root">
            if (!body.includes('id="root"')) {
              body = body.replace(
                /<div id="pageContentBodyWrapper">/,
                `<div id="pageContentBodyWrapper"><div id="root"></div>`,
              );
            }

            res.setHeader('content-length', Buffer.byteLength(body).toString());
            res.end(body);
          } else {
            // Các resource khác (CSS, JS, ảnh…)
            const buffer = Buffer.from(await upstreamResp.arrayBuffer());
            res.setHeader('content-length', buffer.length);
            res.end(buffer);
          }
        } catch (err) {
          console.error('Proxy error:', err);
          res.statusCode = 500;
          res.end('Proxy to EzyPlatform failed');
        }
      });
    },
    client: {
      protocol: 'ws',
      host: 'localhost',
      port: 3000,
    },
  },
});
