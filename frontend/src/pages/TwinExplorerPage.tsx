import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import { Clock3, Loader2, Radar, Search } from 'lucide-react';
import { Bar, CartesianGrid, ComposedChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { ChartFrame } from '@/components/charts/ChartFrame';
import { PageHeader } from '@/components/ui/PageHeader';
import { KpiCard } from '@/components/ui/KpiCard';
import { fraudApi } from '@/lib/fraudApi';
import { compactCurrency } from '@/lib/chartUtils';
import { normalizeCustomerId } from '@/lib/decisionUtils';
import { twinSummary } from '@/lib/mockData';
import { env } from '@/config/env';

const SAMPLE_CUSTOMERS = ['CUST-0004315', 'CUST-0011646', 'CUST-0030430'];

function formatAmount(value: number) {
  return `$${value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function TwinExplorerPage() {
  const [customerId, setCustomerId] = useState('CUST-0004315');
  const [queryId, setQueryId] = useState('CUST-0004315');
  const [activeTab, setActiveTab] = useState<'profile' | 'timeline'>('profile');

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['twin-explorer', queryId],
    queryFn: () => fraudApi.twinExplorer(normalizeCustomerId(queryId)),
    enabled: !env.useMock
  });

  const { data: timeline, isLoading: timelineLoading } = useQuery({
    queryKey: ['customer-timeline', queryId],
    queryFn: () => fraudApi.customerTimeline(normalizeCustomerId(queryId)),
    enabled: !env.useMock && activeTab === 'timeline'
  });

  const twin = env.useMock
    ? {
        customerId: twinSummary.customerId,
        transactionCount: twinSummary.transactionCount,
        amountMean: twinSummary.amountMean,
        amountStdDev: twinSummary.amountStdDev,
        amountTotal: twinSummary.amountMean * twinSummary.transactionCount,
        lastMerchantCategory: twinSummary.topCategories[0]?.category ?? '',
        topMerchantCategory: twinSummary.topCategories[0]?.category ?? '',
        knownDevices: Array.from({ length: twinSummary.knownDevices }, (_, i) => `device-${i + 1}`),
        usualCountries: twinSummary.usualCountries,
        merchantCategories: twinSummary.topCategories.map((c) => ({
          category: c.category,
          displayCategory: c.category,
          count: c.count,
          frequency: 0,
          riskOverlay: c.category === 'travel' ? 54 : c.category === 'electronics' ? 39 : 12
        })),
        coldStart: false
      }
    : data;

  const chartData =
    twin?.merchantCategories.map((c) => ({
      category: c.displayCategory || c.category,
      count: c.count,
      riskOverlay: c.riskOverlay
    })) ?? [];

  const hasAmountBaseline = (twin?.transactionCount ?? 0) > 0 && (twin?.amountTotal ?? 0) > 0;

  const lookup = (id: string) => {
    const normalized = normalizeCustomerId(id);
    setCustomerId(normalized);
    setQueryId(normalized);
  };

  const velocity = timeline?.velocity;

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Customer Twin"
        title="Twin Explorer"
        description="Behavioral baseline plus Customer 360 timeline — decisions, drift, step-ups, cases, and velocity."
      />
      <div className="legend-row rich">
        <button className={`legend-chip ${activeTab === 'profile' ? 'active' : ''}`} type="button" onClick={() => setActiveTab('profile')}>Profile</button>
        <button className={`legend-chip ${activeTab === 'timeline' ? 'active' : ''}`} type="button" onClick={() => setActiveTab('timeline')}>Customer 360 timeline</button>
      </div>
      <div className="glass-card" style={{ display: 'flex', gap: 12 }}>
        <div style={{ position: 'relative', flex: 1 }}>
          <Search size={16} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', opacity: 0.5 }} />
          <input
            className="field-input"
            style={{ paddingLeft: 36, width: '100%' }}
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && lookup(customerId)}
            placeholder="Customer ID (e.g. CUST-001007)"
          />
        </div>
        <button className="primary-button" onClick={() => { lookup(customerId); refetch(); }}>
          {isLoading ? <Loader2 size={16} className="spin" /> : <Radar size={16} />}
          Look up
        </button>
      </div>
      <div className="legend-row rich">
        {SAMPLE_CUSTOMERS.map((id) => (
          <button key={id} className="legend-chip" type="button" onClick={() => lookup(id)}>
            {id}
          </button>
        ))}
      </div>
      {error && <div className="glass-card error-banner">Failed to load twin: {(error as Error).message}</div>}
      {isLoading && !twin && <div className="glass-card empty-state">Loading twin profile…</div>}
      {!isLoading && !error && !twin && (
        <div className="glass-card empty-state">No twin profile found. Search for a customer ID from your test dataset.</div>
      )}
      {twin && activeTab === 'profile' && (
        <>
          <div className="kpi-grid">
            <KpiCard label="Customer" value={twin.customerId} trend="Selected profile" icon={<Radar />} />
            <KpiCard
              label="Total spend"
              value={hasAmountBaseline ? compactCurrency(twin.amountTotal) : '—'}
              trend={hasAmountBaseline ? `${twin.transactionCount.toLocaleString()} trusted events` : 'No amount baseline yet'}
            />
            <KpiCard label="Known devices" value={String(twin.knownDevices.length)} trend="Trusted device fingerprints" />
            <KpiCard label="Usual countries" value={twin.usualCountries.join(', ') || '—'} trend="Learned baseline" />
          </div>
          <div className="dashboard-grid two">
            <div className="chart-card rich-chart">
              <div className="chart-head">
                <div>
                  <div className="chart-title">Merchant category memory</div>
                  <div className="chart-subtitle">
                    {twin.topMerchantCategory
                      ? `Top category: ${twin.topMerchantCategory}`
                      : 'Categories learned from trusted ALLOW decisions'}
                  </div>
                </div>
                {twin.lastMerchantCategory && (
                  <div className="chart-stats">
                    <span className="chart-stat-pill accent">Last: {twin.lastMerchantCategory}</span>
                  </div>
                )}
              </div>
              <ChartFrame height={320}>
                {chartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%" minWidth={0}>
                    <ComposedChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" opacity={0.16} />
                      <XAxis dataKey="category" tick={{ fill: '#8ea4c8', fontSize: 11 }} interval={0} />
                      <YAxis yAxisId="left" tick={{ fill: '#8ea4c8', fontSize: 11 }} allowDecimals={false} />
                      <YAxis yAxisId="right" orientation="right" domain={[0, 100]} tick={{ fill: '#8ea4c8', fontSize: 11 }} />
                      <Tooltip contentStyle={{ background: '#0b1628', border: '1px solid rgba(148,163,184,.2)', borderRadius: 12 }} />
                      <Bar yAxisId="left" dataKey="count" radius={[8, 8, 0, 0]} fill="#38bdf8" />
                      <Line yAxisId="right" type="monotone" dataKey="riskOverlay" stroke="#a78bfa" strokeWidth={2} dot={false} />
                    </ComposedChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="empty-state">No category memory yet. Twin needs trusted ALLOW decisions to build baseline.</div>
                )}
              </ChartFrame>
            </div>
            <div className="glass-card large-copy">
              <h3>Behavioral baseline</h3>
              {hasAmountBaseline ? (
                <>
                  <p>Amount mean: <b>{formatAmount(twin.amountMean)}</b></p>
                  <p>Amount standard deviation: <b>{formatAmount(twin.amountStdDev)}</b></p>
                  <p>Total trusted spend: <b>{formatAmount(twin.amountTotal)}</b></p>
                </>
              ) : (
                <p>Amount baseline is not available yet. Trusted ALLOW decisions with transaction amounts are required.</p>
              )}
              {twin.topMerchantCategory && <p>Top merchant category: <b>{twin.topMerchantCategory}</b></p>}
              {twin.lastMerchantCategory && <p>Last merchant category: <b>{twin.lastMerchantCategory}</b></p>}
              <p>The twin is updated only after trusted decisions. Blocked activity should not become customer-normal behavior.</p>
              {twin.coldStart && <p><b>Cold-start twin</b> — still building baseline history. Try one of the sample customer chips above.</p>}
              <div className="pill-row">
                {twin.knownDevices.map((device) => (
                  <span className="pill" key={device}>{device}</span>
                ))}
              </div>
            </div>
          </div>
        </>
      )}
      {activeTab === 'timeline' && (
        <>
          {timelineLoading && <div className="glass-card empty-state"><Loader2 className="spin" size={18} /> Loading Customer 360 timeline…</div>}
          {timeline && (
            <>
              <div className="kpi-grid">
                <KpiCard label="Txns 5m" value={String(velocity?.txnCount5m ?? 0)} trend="Real-time velocity" icon={<Clock3 />} />
                <KpiCard label="Txns 1h" value={String(velocity?.txnCount1h ?? 0)} trend={`$${(velocity?.amountSum1h ?? 0).toLocaleString()} volume`} icon={<Clock3 />} />
                <KpiCard label="Txns 24h" value={String(velocity?.txnCount24h ?? 0)} trend="Rolling window" icon={<Clock3 />} />
                <KpiCard label="Failed 30m" value={String(velocity?.failedAttempts30m ?? 0)} trend="Block/challenge attempts" icon={<Clock3 />} />
              </div>
              <div className="glass-card">
                <h3>Unified timeline</h3>
                <div className="timeline-list">
                  {timeline.events.map((event) => (
                    <div key={`${event.eventType}-${event.eventId}`} className="timeline-item">
                      <div className="timeline-dot" />
                      <div>
                        <strong>{event.eventType}</strong> · {event.title}
                        <div className="muted">{format(new Date(event.occurredAt), 'yyyy-MM-dd HH:mm:ss')}</div>
                        {event.description && <p>{event.description}</p>}
                      </div>
                    </div>
                  ))}
                </div>
                {timeline.events.length === 0 && <div className="empty-state">No timeline events yet for this customer.</div>}
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
}
