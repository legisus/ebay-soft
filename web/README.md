# web — ebay-soft SPA

Vite + React 19 + TypeScript. Tailwind v4 + shadcn-ready, TanStack Query + TanStack Router, ECharts.

## Run

```bash
npm install
npm run dev          # serves on http://localhost:5173, proxies /v1 → api-gateway
npm test
```

## Layout

- `src/main.tsx` — TanStack Router + Query client bootstrap
- `src/routes/` — one file per route component
- `src/lib/api.ts` — gateway client with JWT bearer + Problem-Detail-aware errors
- `src/lib/cn.ts` — Tailwind class merger (used by shadcn components)
- `src/index.css` — Tailwind v4 + design tokens (Bloomberg-density, tabular-num money, dark default)

## Configuration

Set `VITE_GATEWAY_URL` (default `http://localhost:8080`) to point at a different api-gateway during dev.

## shadcn/ui

`components.json` is configured. To add a component:

```bash
npx shadcn@latest add button
```
