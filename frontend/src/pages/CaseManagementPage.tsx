import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { format, formatDistanceToNow } from 'date-fns';
import { AlertTriangle, ClipboardList, ExternalLink, Loader2, RefreshCw, UserPlus } from 'lucide-react';
import { Link } from 'react-router-dom';
import { DecisionExplainabilityPanel } from '@/components/explainability/DecisionExplainabilityPanel';
import { CaseStatusFilterTabs, type CaseStatusFilter } from '@/components/ui/CaseStatusFilterTabs';
import { KpiCard } from '@/components/ui/KpiCard';
import { PageHeader } from '@/components/ui/PageHeader';
import { TablePagination } from '@/components/ui/TablePagination';
import { canAssignCases, canBackfillCases, canEscalateCases } from '@/config/pagePermissions';
import { buildFallbackNarrative } from '@/lib/explainabilityUtils';
import { fraudApi } from '@/lib/fraudApi';
import { env } from '@/config/env';
import { useAuthStore } from '@/store/authStore';

function statusClass(status: string) {
  if (status === 'OPEN' || status === 'ASSIGNED') return 'status-pill warn';
  if (status === 'IN_REVIEW' || status === 'WAITING_CUSTOMER') return 'status-pill';
  if (status === 'CONFIRMED_FRAUD') return 'status-pill danger';
  if (status === 'FALSE_POSITIVE' || status === 'CLOSED') return 'status-pill ok';
  return 'status-pill';
}

export function CaseManagementPage() {
  const queryClient = useQueryClient();
  const userRole = useAuthStore((state) => state.user?.role);
  const canAssign = canAssignCases(userRole);
  const canEscalate = canEscalateCases(userRole);
  const canBackfill = canBackfillCases(userRole);

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [statusFilter, setStatusFilter] = useState<CaseStatusFilter>('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [customerSearch, setCustomerSearch] = useState('');
  const [transactionSearch, setTransactionSearch] = useState('');
  const [assigneeSearch, setAssigneeSearch] = useState('');
  const [selectedCaseId, setSelectedCaseId] = useState<string | null>(null);
  const [assignTo, setAssignTo] = useState('');
  const [note, setNote] = useState('');

  const { data: stats, refetch: refetchStats } = useQuery({
    queryKey: ['case-summary'],
    queryFn: () => fraudApi.caseSummary(),
    enabled: !env.useMock
  });

  const { data, isLoading, error, isFetching } = useQuery({
    queryKey: [
      'fraud-cases',
      page,
      pageSize,
      statusFilter,
      priorityFilter,
      customerSearch,
      transactionSearch,
      assigneeSearch
    ],
    queryFn: () =>
      fraudApi.cases(
        page,
        pageSize,
        statusFilter,
        customerSearch,
        transactionSearch,
        assigneeSearch,
        priorityFilter
      ),
    enabled: !env.useMock,
    placeholderData: (prev) => prev
  });

  const rows = data?.items ?? [];

  useEffect(() => {
    if (rows.length === 0) {
      setSelectedCaseId(null);
      return;
    }
    setSelectedCaseId((current) =>
      current && rows.some((row) => row.caseId === current) ? current : rows[0].caseId
    );
  }, [rows]);

  const { data: detail, isLoading: detailLoading } = useQuery({
    queryKey: ['fraud-case', selectedCaseId],
    queryFn: () => fraudApi.caseDetail(selectedCaseId!),
    enabled: !env.useMock && !!selectedCaseId
  });

  const assessmentId = detail?.assessmentId ?? null;

  const {
    data: narrative,
    isLoading: narrativeLoading,
    isError: narrativeError
  } = useQuery({
    queryKey: ['decision-narrative', assessmentId],
    queryFn: () => fraudApi.decisionNarrative(assessmentId!),
    enabled: !env.useMock && !!assessmentId,
    retry: 1
  });

  const { data: auditDetail, isLoading: auditDetailLoading } = useQuery({
    queryKey: ['audit-detail', assessmentId],
    queryFn: () => fraudApi.auditDetail(assessmentId!),
    enabled: !env.useMock && !!assessmentId && narrativeError
  });

  const displayNarrative = useMemo(() => {
    if (narrative) return narrative;
    if (auditDetail && narrativeError) return buildFallbackNarrative(auditDetail);
    return null;
  }, [narrative, auditDetail, narrativeError]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['fraud-cases'] });
    queryClient.invalidateQueries({ queryKey: ['case-summary'] });
    if (selectedCaseId) queryClient.invalidateQueries({ queryKey: ['fraud-case', selectedCaseId] });
    if (assessmentId) {
      queryClient.invalidateQueries({ queryKey: ['decision-narrative', assessmentId] });
    }
  };

  const assign = useMutation({
    mutationFn: () => fraudApi.assignCase(selectedCaseId!, assignTo.trim()),
    onSuccess: () => {
      setAssignTo('');
      invalidate();
    }
  });

  const escalate = useMutation({
    mutationFn: () => fraudApi.escalateCase(selectedCaseId!),
    onSuccess: invalidate
  });

  const addNote = useMutation({
    mutationFn: () => fraudApi.addCaseNote(selectedCaseId!, note.trim()),
    onSuccess: () => {
      setNote('');
      invalidate();
    }
  });

  const closeCase = useMutation({
    mutationFn: (status: string) => fraudApi.updateCaseStatus(selectedCaseId!, status, status, note.trim()),
    onSuccess: () => {
      setNote('');
      invalidate();
    }
  });

  const backfill = useMutation({
    mutationFn: () => fraudApi.backfillCases(500),
    onSuccess: () => {
      invalidate();
      refetchStats();
    }
  });

  const explainabilityLoading =
    !!assessmentId && (narrativeLoading || (narrativeError && auditDetailLoading && !displayNarrative));

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Fraud Operations"
        title="Case Management"
        description="Investigation queue with assignment, SLA, escalation, notes, and decision explainability."
      />

      {stats && (
        <div className="kpi-grid">
          <KpiCard
            label="Active cases"
            value={stats.activeCases.toLocaleString()}
            trend={`${stats.openCases.toLocaleString()} open`}
            icon={<ClipboardList />}
          />
          <KpiCard
            label="High priority"
            value={stats.highPriorityCases.toLocaleString()}
            trend="CRITICAL and HIGH queue"
            icon={<AlertTriangle />}
          />
          <KpiCard
            label="SLA breached"
            value={stats.slaBreachedCases.toLocaleString()}
            trend="Needs immediate action"
            icon={<AlertTriangle />}
          />
          <KpiCard
            label="Total cases"
            value={stats.totalCases.toLocaleString()}
            trend={`${stats.assignedCases.toLocaleString()} assigned`}
            icon={<UserPlus />}
          />
        </div>
      )}

      <div className="glass-card audit-toolbar case-toolbar">
        <CaseStatusFilterTabs
          value={statusFilter}
          stats={stats}
          onChange={(next) => {
            setStatusFilter(next);
            setPage(0);
          }}
        />
        <div className="audit-toolbar-actions case-search-row">
          <input
            className="field-input compact"
            placeholder="Customer ID"
            value={customerSearch}
            onChange={(event) => {
              setCustomerSearch(event.target.value);
              setPage(0);
            }}
          />
          <input
            className="field-input compact"
            placeholder="Transaction ID"
            value={transactionSearch}
            onChange={(event) => {
              setTransactionSearch(event.target.value);
              setPage(0);
            }}
          />
          <input
            className="field-input compact"
            placeholder="Assignee"
            value={assigneeSearch}
            onChange={(event) => {
              setAssigneeSearch(event.target.value);
              setPage(0);
            }}
          />
          <select
            className="field-input compact"
            value={priorityFilter}
            onChange={(event) => {
              setPriorityFilter(event.target.value);
              setPage(0);
            }}
          >
            <option value="ALL">All priorities</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
          {canBackfill && (
            <button
              className="ghost-button"
              disabled={backfill.isPending}
              onClick={() => backfill.mutate()}
            >
              <RefreshCw size={16} /> Backfill from audit
            </button>
          )}
          {(isLoading || isFetching) && <span className="audit-status">Loading…</span>}
        </div>
      </div>

      {backfill.data && (
        <div className="glass-card explainability-banner warn">
          Backfill created {backfill.data.created.toLocaleString()} cases · {backfill.data.remaining.toLocaleString()} audit decisions still without cases
        </div>
      )}

      {error && <div className="glass-card error-banner">Failed to load cases: {(error as Error).message}</div>}

      <div className="dashboard-grid two">
        <div className="table-card">
          <table>
            <thead>
              <tr>
                <th>Case</th>
                <th>Customer</th>
                <th>Priority</th>
                <th>Status</th>
                <th>SLA</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((item) => (
                <tr
                  key={item.caseId}
                  className={selectedCaseId === item.caseId ? 'row-selected' : ''}
                  onClick={() => setSelectedCaseId(item.caseId)}
                  style={{ cursor: 'pointer' }}
                >
                  <td>
                    <strong>{item.caseId}</strong>
                    <div className="muted">{item.transactionId}</div>
                  </td>
                  <td>{item.customerId}</td>
                  <td>
                    <span
                      className={
                        item.priority === 'CRITICAL'
                          ? 'status-pill danger'
                          : item.priority === 'HIGH'
                            ? 'status-pill warn'
                            : 'status-pill'
                      }
                    >
                      {item.priority}
                    </span>
                  </td>
                  <td><span className={statusClass(item.status)}>{item.status}</span></td>
                  <td>
                    {item.slaDueAt
                      ? formatDistanceToNow(new Date(item.slaDueAt), { addSuffix: true })
                      : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {isLoading && (
            <div className="empty-state">
              <Loader2 className="spin" size={18} /> Loading cases…
            </div>
          )}
          {!isLoading && rows.length === 0 && (
            <div className="empty-state">
              No cases found. Use backfill to create cases from BLOCK/CHALLENGE audit records.
            </div>
          )}
          {data && (
            <TablePagination
              page={page}
              pageSize={pageSize}
              totalElements={data.totalElements}
              totalPages={data.totalPages}
              disabled={isLoading || isFetching}
              pageSizeOptions={[25, 50, 100]}
              onPageChange={setPage}
              onPageSizeChange={(nextSize) => {
                setPageSize(nextSize);
                setPage(0);
              }}
            />
          )}
        </div>

        <div className="glass-card explainability-card">
          {!selectedCaseId && <div className="empty-state">Select a case to view details and actions.</div>}
          {selectedCaseId && detailLoading && (
            <div className="empty-state">
              <Loader2 className="spin" size={18} /> Loading case…
            </div>
          )}
          {detail && (
            <>
              <div className="case-detail-head">
                <div>
                  <h3>{detail.caseId}</h3>
                  <p>{detail.summary}</p>
                  <p>
                    Customer <b>{detail.customerId}</b> · Transaction <b>{detail.transactionId}</b>
                  </p>
                  <p>
                    Assigned <b>{detail.assignedTo ?? 'Unassigned'}</b> · Escalation <b>{detail.escalationLevel}</b>
                  </p>
                </div>
                {assessmentId && (
                  <Link className="ghost-button case-audit-link" to={`/audit?assessmentId=${encodeURIComponent(assessmentId)}`}>
                    <ExternalLink size={16} /> Audit trail
                  </Link>
                )}
              </div>

              {canAssign && (
                <div className="audit-toolbar">
                  <input
                    className="field-input"
                    placeholder="Assign to analyst"
                    value={assignTo}
                    onChange={(event) => setAssignTo(event.target.value)}
                  />
                  <button
                    className="ghost-button"
                    disabled={!assignTo.trim() || assign.isPending}
                    onClick={() => assign.mutate()}
                  >
                    Assign
                  </button>
                </div>
              )}

              <div className="audit-toolbar">
                <input
                  className="field-input"
                  placeholder="Case note"
                  value={note}
                  onChange={(event) => setNote(event.target.value)}
                />
                <button
                  className="ghost-button"
                  disabled={!note.trim() || addNote.isPending}
                  onClick={() => addNote.mutate()}
                >
                  Add note
                </button>
                {canEscalate && (
                  <button className="ghost-button" disabled={escalate.isPending} onClick={() => escalate.mutate()}>
                    Escalate
                  </button>
                )}
              </div>

              <div className="pill-row">
                <button className="ghost-button" onClick={() => closeCase.mutate('IN_REVIEW')}>In review</button>
                <button className="ghost-button" onClick={() => closeCase.mutate('CONFIRMED_FRAUD')}>Confirm fraud</button>
                <button className="ghost-button" onClick={() => closeCase.mutate('FALSE_POSITIVE')}>False positive</button>
                <button className="ghost-button" onClick={() => closeCase.mutate('CLOSED')}>Close</button>
              </div>

              {assessmentId && explainabilityLoading && (
                <div className="explainability-placeholder compact">
                  <span className="audit-status">Loading decision explainability…</span>
                </div>
              )}
              {assessmentId && displayNarrative && <DecisionExplainabilityPanel narrative={displayNarrative} />}

              <h4>Evidence timeline</h4>
              <div className="timeline-list">
                {detail.events.map((event) => (
                  <div key={event.eventId} className="timeline-item">
                    <div className="timeline-dot" />
                    <div>
                      <strong>{event.eventType}</strong>
                      <div className="muted">
                        {format(new Date(event.createdAt), 'yyyy-MM-dd HH:mm:ss')} · {event.actorId ?? 'system'}
                      </div>
                      {typeof event.payload.note === 'string' && event.payload.note && <p>{event.payload.note}</p>}
                      {typeof event.payload.status === 'string' && event.payload.status && (
                        <p>Status: {event.payload.status}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
