import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// Dev-mode proxy targets api-gateway so the SPA can call /v1/* without CORS.
// In production, Traefik routes /v1/* directly to api-gateway and / to this
// SPA — see infra/traefik/dynamic.yml.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      "/v1": "http://localhost:8080",
      "/actuator": "http://localhost:8080",
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: "./tests/setup.ts",
  },
});
