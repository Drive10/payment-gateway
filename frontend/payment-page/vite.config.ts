import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig(async () => {
  const plugins = [react()];

  if (process.env.VITE_SENTRY_ENABLED) {
    try {
      const sentryVitePlugin = (await import("@sentry/vite-plugin")).default;
      plugins.push(sentryVitePlugin({
        org: process.env.VITE_SENTRY_ORG,
        project: process.env.VITE_SENTRY_PROJECT,
        silent: !process.env.PROD,
      }));
    } catch {
      // Sentry plugin not available
    }
  }

  return {
    plugins,
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
    server: {
      port: 5173,
      strictPort: true,
proxy: {
        "/api/v1": {
          target: "http://127.0.0.1:8080",
          changeOrigin: true,
          secure: false,
        },
        "/api": {
          target: "http://127.0.0.1:8080",
          changeOrigin: true,
          secure: false,
        },
      },
    },
    preview: {
      port: 5173,
      strictPort: true,
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            "vendor-react": ["react", "react-dom", "react-router-dom"],
            "vendor-motion": ["framer-motion"],
            "vendor-forms": ["react-hook-form", "@hookform/resolvers", "zod"],
            "vendor-axios": ["axios"],
          },
        },
      },
      chunkSizeWarningLimit: 500,
      minify: "esbuild",
      target: "esnext",
    },
    optimizeDeps: {
      include: ["react", "react-dom", "react-router-dom", "axios"],
    },
    test: {
      include: ["src/**/*.test.{ts,tsx}"],
      exclude: ["tests/**", "node_modules/**"],
      environment: "jsdom",
      setupFiles: [],
    },
  };
});
