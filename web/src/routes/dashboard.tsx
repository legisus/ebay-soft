import ReactECharts from "echarts-for-react";

/**
 * Phase 0 placeholder dashboard — one ECharts widget with hardcoded data, exercising
 * the full chart pipeline before any real analytics data flows. Replaced by real widgets
 * during Phase 2 (issues #55 / #56).
 */
export function DashboardPage() {
  return (
    <div className="min-h-dvh px-8 py-6 max-w-7xl mx-auto space-y-6">
      <header className="flex items-baseline justify-between border-b border-border pb-4">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">Dashboard</h1>
          <p className="text-xs text-muted-foreground">Phase 0 preview — chart pipeline smoke test</p>
        </div>
        <span className="text-xs text-muted-foreground font-mono">staging.ebay-soft.com</span>
      </header>

      <section className="grid gap-4 md:grid-cols-3">
        <Stat label="Revenue (30d)" value="$12,430.59" delta="+8.2%" positive />
        <Stat label="Net margin (30d)" value="$3,118.04" delta="+12.1%" positive />
        <Stat label="Orders (30d)" value="384" delta="-2.3%" />
      </section>

      <section className="border border-border bg-card rounded-lg p-6">
        <h2 className="text-sm font-medium text-muted-foreground mb-4">Daily revenue · last 30 days</h2>
        <ReactECharts
          option={chartOption}
          style={{ height: 320 }}
          theme="dark"
          opts={{ renderer: "svg" }}
        />
      </section>
    </div>
  );
}

function Stat({ label, value, delta, positive }: { label: string; value: string; delta: string; positive?: boolean }) {
  return (
    <div className="border border-border bg-card rounded-lg p-4">
      <div className="text-xs uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className="mt-2 flex items-baseline gap-2">
        <span className="text-2xl font-numeric font-semibold">{value}</span>
        <span className={positive ? "text-positive text-sm font-numeric" : "text-negative text-sm font-numeric"}>
          {delta}
        </span>
      </div>
    </div>
  );
}

const chartOption = {
  backgroundColor: "transparent",
  tooltip: { trigger: "axis" },
  grid: { left: 40, right: 16, top: 24, bottom: 32 },
  xAxis: {
    type: "category",
    data: Array.from({ length: 30 }, (_, i) => `D-${30 - i}`),
    axisLine: { lineStyle: { color: "#1e242c" } },
    axisLabel: { color: "#8a93a0", fontSize: 10 },
  },
  yAxis: {
    type: "value",
    splitLine: { lineStyle: { color: "#1e242c", type: "dashed" } },
    axisLabel: { color: "#8a93a0", fontSize: 10, formatter: (v: number) => `$${v}` },
  },
  series: [
    {
      type: "line",
      smooth: true,
      symbol: "none",
      lineStyle: { color: "#ffb000", width: 2 },
      areaStyle: { color: "rgba(255, 176, 0, 0.08)" },
      data: [
        310, 285, 340, 392, 410, 388, 360, 412, 455, 480,
        510, 502, 478, 521, 549, 588, 612, 590, 605, 638,
        670, 692, 661, 705, 740, 718, 752, 781, 766, 802,
      ],
    },
  ],
};
