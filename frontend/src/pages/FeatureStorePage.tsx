import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import { Database, Search } from 'lucide-react';
import { PageHeader } from '@/components/ui/PageHeader';
import { KpiCard } from '@/components/ui/KpiCard';
import { fraudApi } from '@/lib/fraudApi';
import { normalizeCustomerId } from '@/lib/decisionUtils';
import { env } from '@/config/env';

export function FeatureStorePage() {
  const [featureName, setFeatureName] = useState('customer_24h_amount_sum');
  const [customerId, setCustomerId] = useState('');
  const [queryFeature, setQueryFeature] = useState('customer_24h_amount_sum');
  const [queryCustomer, setQueryCustomer] = useState('');

  const { data: catalog } = useQuery({
    queryKey: ['feature-store-catalog'],
    queryFn: () => fraudApi.featureStoreCatalog(),
    enabled: !env.useMock
  });

  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['feature-store', queryFeature, queryCustomer],
    queryFn: () =>
      fraudApi.featureStore(
        queryCustomer ? undefined : queryFeature,
        queryCustomer || undefined
      ),
    enabled: !env.useMock
  });

  const rows = data ?? [];

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="ML Platform"
        title="Feature Store"
        description="Online feature values mirrored from velocity aggregation and PostgreSQL feature tables."
      />

      <div className="kpi-grid">
        <KpiCard label="Feature definitions" value={String(catalog?.featureNames.length ?? 0)} trend="Registered feature names" icon={<Database />} />
        <KpiCard label="Stored values" value={String(catalog?.totalEntries ?? 0)} trend="PostgreSQL online store" icon={<Database />} />
        <KpiCard label="Backend cache" value={env.useMock ? 'Mock' : 'Redis when APP_REDIS_ENABLED=true'} trend="Low-latency velocity counters" icon={<Database />} />
      </div>

      <div className="glass-card audit-toolbar">
        <select className="field-input" value={featureName} onChange={(e) => setFeatureName(e.target.value)}>
          {(catalog?.featureNames ?? ['customer_24h_amount_sum']).map((name) => (
            <option key={name} value={name}>{name}</option>
          ))}
        </select>
        <div style={{ position: 'relative', flex: 1 }}>
          <Search size={16} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', opacity: 0.5 }} />
          <input
            className="field-input"
            style={{ paddingLeft: 36, width: '100%' }}
            placeholder="Optional customer ID filter"
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value)}
          />
        </div>
        <button
          className="primary-button"
          onClick={() => {
            setQueryFeature(featureName);
            setQueryCustomer(customerId ? normalizeCustomerId(customerId) : '');
            refetch();
          }}
        >
          Load features
        </button>
      </div>

      {error && <div className="glass-card error-banner">Failed to load feature store: {(error as Error).message}</div>}

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>Entity key</th>
              <th>Feature</th>
              <th>Value</th>
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.entityKey}>
                <td>{row.entityKey}</td>
                <td>{row.featureName}</td>
                <td><b>{row.featureValue.toLocaleString(undefined, { maximumFractionDigits: 4 })}</b></td>
                <td>{format(new Date(row.updatedAt), 'yyyy-MM-dd HH:mm:ss')}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {isLoading && <div className="empty-state">Loading feature values…</div>}
        {!isLoading && rows.length === 0 && !error && <div className="empty-state">No feature values found for this query.</div>}
      </div>
    </div>
  );
}
