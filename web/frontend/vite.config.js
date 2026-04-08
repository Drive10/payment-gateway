import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(() => {
  return {
    plugins: [react()],
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        "/api/v1": {
          target: "http://127.0.0.1:8080",
          changeOrigin: true,
          secure: false,
        },
        "/platform": {
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
  };
});
