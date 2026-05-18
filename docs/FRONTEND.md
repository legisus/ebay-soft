# Frontend — stack and chart library analysis

## Stack recommendation

| Layer             | Choice                                  | Why                                                                 |
|-------------------|-----------------------------------------|---------------------------------------------------------------------|
| Framework         | **React 19 + TypeScript**               | Largest talent pool, mature chart libs, SSR not required for a logged-in dashboard |
| Build             | **Vite 6**                              | Fast HMR, ESM-first, no webpack pain                                |
| Routing           | TanStack Router or React Router 7       | Type-safe routes; TanStack if we want strict types end-to-end       |
| Server state      | **TanStack Query (React Query) v5**     | The right answer for REST-heavy SaaS                                |
| UI components     | **shadcn/ui** + Tailwind CSS v4         | Own the component code, no vendor lock-in, great defaults           |
| Forms             | React Hook Form + Zod                   | Validation matches Java DTO constraints                             |
| Tables (huge)     | **TanStack Table v8**                   | Headless, virtualized, handles 100k-row inventory views             |
| Charts            | **See comparison below**                |                                                                     |
| Real-time         | Native `EventSource` for SSE            | Backend uses SSE; no library needed                                 |
| i18n              | i18next                                 | Sellers are global — EN, DE, FR, ES, IT, PL day one                 |
| Testing           | Vitest + React Testing Library + Playwright | Same Vite pipeline, fast                                        |

### Why not alternatives

- **Next.js** — overkill. We have no public marketing pages that need SSR (we'll do those in Astro or plain HTML on the marketing subdomain). For a logged-in dashboard, plain Vite SPA is simpler, cheaper, and faster.
- **Vue / Svelte** — fine technically; smaller hiring pool, weaker chart ecosystem (especially financial).
- **Angular** — heavy, opinionated, and the ecosystem fit for our chart needs is worse.
- **MUI / Ant Design** — saves time at first, locks aesthetics later, hard to brand. shadcn/ui gives us the same speed without the lock-in.

---

## Chart library comparison

A seller dashboard lives or dies on its charts. We need: time-series P&L, candlestick-like price vs. competitors, heatmaps (best-selling hours/days), Pareto, funnels, and sometimes geospatial (where buyers are).

| Library             | License        | Strengths                                                                  | Weaknesses                                                            | Verdict for us |
|---------------------|----------------|----------------------------------------------------------------------------|-----------------------------------------------------------------------|----------------|
| **Apache ECharts**  | Apache 2.0     | Vast chart set (candlestick, Sankey, treemap, heatmap, geo), incredibly performant on large series, beautiful defaults, free | Imperative API, larger bundle (~400kB gz), not React-idiomatic | **Top pick for analytics-heavy dashboards** |
| **Highcharts**      | Commercial ($) | Most polished out of the box, excellent docs, stock & gantt add-ons        | License required for commercial SaaS (~$535–$1,690+/year per dev)     | Skip — cost beats benefit |
| **Recharts**        | MIT            | Declarative React API, composable, easy to theme                           | Slow above ~5k points, limited chart types, no candlestick            | Good for simple KPIs only |
| **Visx (Airbnb)**   | MIT            | D3-powered primitives, fully customizable, React-idiomatic                 | You build the chart — high effort, slow time-to-market                | Use only for one signature custom viz |
| **Plotly.js**       | MIT            | Scientific charts, 3D, statistical                                         | Heavy (~3MB), not aimed at dashboards, dated aesthetics               | Skip |
| **Chart.js + react-chartjs-2** | MIT | Lightweight, easy                                                          | Not enough chart types, weak interactivity                            | Skip |
| **Nivo**            | MIT            | Beautiful React-native charts on D3, good defaults                         | Less performant than ECharts on big datasets, smaller chart catalog   | Solid runner-up |
| **TanStack Charts** | MIT            | Same authors as Query/Table — promising                                    | Too new (v0.x), small chart set                                       | Watch, don't ship |
| **AG Charts**       | Commercial / free community | Enterprise feel, great financial charts                          | Commercial for full feature set                                        | Skip unless we already buy AG Grid |

### Recommendation

**Apache ECharts via `echarts-for-react`** as the primary chart library, with **Recharts** for trivial KPI sparklines if bundle weight matters on the marketing pages. Reasoning:

1. **Coverage** — ECharts has every chart we'll need (candlestick for price-vs-competition, sankey for funnel, treemap for category mix, heatmap for hourly demand, geo for buyer map) without pulling extra libraries.
2. **Performance** — eBay sellers with 50k+ orders will request multi-year P&L by day. ECharts renders that smoothly; Recharts chokes.
3. **Cost** — free under Apache 2.0, no per-developer license.
4. **Theming** — JSON-driven themes; we can ship a dark mode and a light mode with one config object.

Wrap ECharts in a thin `<Chart options={...} />` React component so we can swap implementations later without touching screens.

### When to escape to D3 / Visx

Only when we need a **signature visualization** that's part of the brand — e.g. an interactive "profit-vs-velocity quadrant" that we put in marketing. One custom chart, not the whole dashboard.

---

## Performance budgets

| Page                    | Bundle (gz) | LCP target | Notes                            |
|-------------------------|-------------|------------|----------------------------------|
| Login                   | < 80 kB     | < 1.5 s    | No charts                        |
| Dashboard               | < 350 kB    | < 2.5 s    | ECharts lazy-loaded              |
| Inventory table (100k)  | < 400 kB    | < 2.5 s    | Virtualized rows, server pagination |
| Report PDF preview      | < 500 kB    | < 3.0 s    | Generated server-side as PDF; the client just renders an iframe |

Use route-level code splitting and dynamic `import('echarts/core')` so the chart engine doesn't bloat the login page.

---

## Design language

- **Don't look like generic AI dashboards.** Avoid the purple-gradient-Tailwind-template look. A sellers' dashboard should feel closer to a Bloomberg terminal or QuickBooks than to a crypto landing page.
- Dense by default, low-chrome, monospaced numerics (`font-variant-numeric: tabular-nums`).
- Dark mode is required, not optional — power users keep this open all day.
- Money fields always show currency, sign, and reconcile to the penny.

---

## Accessibility

- All charts must have a `<table>` fallback below for screen readers (ECharts supports an a11y plugin that does this automatically).
- Color is never the only signal — gain/loss uses + / − glyphs plus color.
- Keyboard shortcuts: `g i` go to inventory, `g r` reports, `/` open command palette.
