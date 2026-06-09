import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import { ChevronLeft, ChevronRight, ShieldAlert } from 'lucide-react';
import { toast } from 'sonner';
import { KpiCard } from '@/components/ui/KpiCard';
import { PageHeader } from '@/components/ui/PageHeader';
import { fraudApi } from '@/lib/fraudApi';
import { env } from '@/config/env';

const POLL_MS = 15_000;

export function StepUpPage() {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const { data, isLoading, error, isFetching } = useQuery({
    queryKey: ['step-up-queue', page],
    queryFn: () => fraudApi.stepUpQueue(page, 50),
    enabled: !env.useMock,
    refetchInterval: POLL_MS,
    placeholderData: (prev) => prev
  });

  const approve = useMutation({
    mutationFn: fraudApi.approveStepUpChallenge,
    onSuccess: (response) => {
      toast.success(`Challenge ${response.challengeId} approved`);
      queryClient.invalidateQueries({ queryKey: ['step-up-queue'] });
    },
    onError: (err: Error) => toast.error(err.message)
  });

  const reject = useMutation({
    mutationFn: fraudApi.rejectStepUpChallenge,
    onSuccess: (response) => {
      toast.success(`Challenge ${response.challengeId} rejected`);
      queryClient.invalidateQueries({ queryKey: ['step-up-queue'] });
    },
    onError: (err: Error) => toast.error(err.message)
  });

  const rows = data?.items ?? [];

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Customer Protection"
        title="Step-Up Challenge Queue"
        description="Pending challenges from CHALLENGE decisions. Twin baseline updates only after approval."
      />
      <div className="kpi-grid">
        <KpiCard
          label="Queue size"
          value={(data?.totalElements ?? 0).toLocaleString()}
          trend="Persisted step_up_challenges"
          icon={<ShieldAlert />}
        />
        <KpiCard label="Workflow" value="Verify" trend="Approve to sync twin" />
        <KpiCard label="Reject path" value="Block" trend="Marks transaction as fraud" />
        <KpiCard label="Twin update" value="Hold" trend="Until challenge success" />
      </div>
      <div className="glass-card audit-toolbar">
        <button className="ghost-button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}><ChevronLeft size={16} /></button>
        <span className="audit-page-label">
          Page {page + 1}{data ? ` of ${data.totalPages.toLocaleString()}` : ''}
        </span>
        <button className="ghost-button" disabled={!data || page >= data.totalPages - 1} onClick={() => setPage((p) => p + 1)}><ChevronRight size={16} /></button>
        {(isLoading || isFetching) && <span className="audit-status">Refreshing…</span>}
      </div>
      {error && <div className="glass-card error-banner">Failed to load step-up queue: {(error as Error).message}</div>}
      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>Created</th>
              <th>Challenge</th>
              <th>Customer</th>
              <th>Transaction</th>
              <th>Risk</th>
              <th>Status</th>
              <th>Reason</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((item) => (
              <tr key={item.challengeId}>
                <td>{format(new Date(item.createdAt), 'yyyy-MM-dd HH:mm')}</td>
                <td>{item.challengeId.slice(0, 8)}</td>
                <td>{item.customerId}</td>
                <td>{item.transactionId}</td>
                <td><b>{item.finalRiskScore.toFixed(1)}</b></td>
                <td>{item.status}</td>
                <td className="reason-cell">{item.reasonDescription}</td>
                <td className="action-cell">
                  <button
                    className="primary-button compact"
                    disabled={approve.isPending}
                    onClick={() => approve.mutate(item.challengeId)}
                  >
                    Approve
                  </button>
                  <button
                    className="secondary-button compact"
                    disabled={reject.isPending}
                    onClick={() => reject.mutate(item.challengeId)}
                  >
                    Reject
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!isLoading && rows.length === 0 && !error && (
          <div className="empty-state">No pending step-up challenges.</div>
        )}
      </div>
    </div>
  );
}
