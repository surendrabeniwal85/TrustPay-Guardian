import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { FraudAlert, FraudStats, ApiResponse, Page } from '../types';
import { Skeleton } from '../components/common/Skeleton';
import { EmptyState } from '../components/common/EmptyState';
import { ConfirmModal } from '../components/common/ConfirmModal';

export const AnalystDashboard: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();
  const isReadOnlyAdmin = user?.role === 'ADMIN';

  const [alerts, setAlerts] = useState<FraudAlert[]>([]);
  const [stats, setStats] = useState<FraudStats | null>(null);
  const [selectedAlert, setSelectedAlert] = useState<FraudAlert | null>(null);

  // Filters & Sorting
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');
  const [sortOrder, setSortOrder] = useState<'SEVERITY_THEN_AGE' | 'NEWEST_FIRST'>('SEVERITY_THEN_AGE');

  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Resolution State
  const [decisionNotes, setDecisionNotes] = useState<string>('');
  const [actionLoading, setActionLoading] = useState<boolean>(false);
  const [confirmDecision, setConfirmDecision] = useState<'RELEASE' | 'CONFIRM_BLOCK' | null>(null);

  const fetchStats = async () => {
    try {
      const res = await api.get<ApiResponse<FraudStats>>('/fraud/stats');
      if (res.data?.data) {
        setStats(res.data.data);
      }
    } catch {
      // Background stats failure silent fallback
    }
  };

  const fetchAlerts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      let url = '/fraud/alerts?size=100';
      if (statusFilter !== 'ALL') {
        url += `&status=${statusFilter}`;
      }
      if (severityFilter !== 'ALL') {
        url += `&riskLevel=${severityFilter}`;
      }

      const [alertsRes] = await Promise.all([
        api.get<ApiResponse<Page<FraudAlert>>>(url),
        fetchStats(),
      ]);

      const items = alertsRes.data?.data?.content || [];
      setAlerts(items);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch fraud alert queue';
      setError(msg);
      showToast(msg, 'danger');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, severityFilter, showToast]);

  useEffect(() => {
    fetchAlerts();
  }, [fetchAlerts]);

  // Open alert detail
  const handleOpenInspection = async (alert: FraudAlert) => {
    try {
      const res = await api.get<ApiResponse<FraudAlert>>(`/fraud/alerts/${alert.id}`);
      setSelectedAlert(res.data?.data || alert);
    } catch {
      setSelectedAlert(alert);
    }
    setDecisionNotes('');
  };

  // Submit Decision
  const handleExecuteDecision = async () => {
    if (!selectedAlert || !confirmDecision) return;
    if (!decisionNotes.trim()) {
      showToast('Investigation outcome notes are mandatory before taking action.', 'warning');
      return;
    }

    setActionLoading(true);
    try {
      await api.post<ApiResponse<FraudAlert>>(`/fraud/alerts/${selectedAlert.id}/decision`, {
        decision: confirmDecision,
        decisionNotes: decisionNotes.trim(),
      });

      const actionLabel = confirmDecision === 'RELEASE' ? 'Released to recipient' : 'Blocked & funds refunded';
      showToast(`Alert #${selectedAlert.id} successfully resolved: ${actionLabel}`, 'success');

      setSelectedAlert(null);
      setConfirmDecision(null);
      setDecisionNotes('');
      fetchAlerts();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Decision execution failed';
      showToast(msg, 'danger');
    } finally {
      setActionLoading(false);
    }
  };

  // Calculate filtered and sorted alerts
  const sortedAlerts = [...alerts].sort((a, b) => {
    if (sortOrder === 'SEVERITY_THEN_AGE') {
      const severityRank: Record<string, number> = { HIGH: 3, MEDIUM: 2, LOW: 1 };
      const rankDiff = (severityRank[b.riskLevel] || 0) - (severityRank[a.riskLevel] || 0);
      if (rankDiff !== 0) return rankDiff;
    }
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
  });

  const getSeverityBadge = (level: string) => {
    switch (level) {
      case 'HIGH':
        return <span className="badge bg-danger">HIGH RISK</span>;
      case 'MEDIUM':
        return <span className="badge bg-warning text-dark">MEDIUM RISK</span>;
      case 'LOW':
        return <span className="badge bg-success">LOW RISK</span>;
      default:
        return <span className="badge bg-secondary">{level}</span>;
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'OPEN':
        return <span className="badge bg-danger-subtle text-danger border border-danger-subtle">Open</span>;
      case 'IN_REVIEW':
        return <span className="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle">In Review</span>;
      case 'RESOLVED_RELEASED':
        return <span className="badge bg-success-subtle text-success border border-success-subtle">Released</span>;
      case 'RESOLVED_BLOCKED':
        return <span className="badge bg-dark text-white">Blocked</span>;
      default:
        return <span className="badge bg-secondary">{status}</span>;
    }
  };

  // Derived KPI metrics
  const totalResolved = (stats?.resolvedReleased || 0) + (stats?.resolvedBlocked || 0);
  const fpRate = totalResolved > 0
    ? (((stats?.resolvedReleased || 0) / totalResolved) * 100).toFixed(1)
    : '0.0';

  return (
    <div className="container py-4">
      {/* Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-3">
        <div>
          <div className="d-flex align-items-center gap-2">
            <h1 className="h3 fw-bold mb-1">Fraud Investigation Console</h1>
            {isReadOnlyAdmin && (
              <span className="badge bg-info-subtle text-info-emphasis border border-info-subtle">
                Admin Surveillance (Read-Only)
              </span>
            )}
          </div>
          <p className="text-secondary small mb-0">
            Real-time heuristic risk scoring, explainable rule forensics, and analyst triage queue.
          </p>
        </div>

        <button
          onClick={fetchAlerts}
          className="btn btn-outline-secondary btn-sm d-flex align-items-center gap-2"
          disabled={loading}
        >
          <i className="bi bi-arrow-clockwise"></i>
          <span>Refresh Queue</span>
        </button>
      </div>

      {/* KPI Cards */}
      <div className="row g-3 mb-4">
        <div className="col-6 col-md-3">
          <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
            <div className="card-body">
              <div className="text-secondary small fw-medium">Open Alerts</div>
              <div className="h3 fw-bold text-danger my-1">
                {stats ? stats.openAlerts : alerts.filter((a) => a.status === 'OPEN').length}
              </div>
              <div className="small text-muted">Awaiting initial analyst triage</div>
            </div>
          </div>
        </div>

        <div className="col-6 col-md-3">
          <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
            <div className="card-body">
              <div className="text-secondary small fw-medium">Average Risk Score</div>
              <div className="h3 fw-bold text-warning-emphasis my-1">
                {stats?.averageRiskScore ? stats.averageRiskScore.toFixed(1) : '45.0'} / 100
              </div>
              <div className="small text-muted">Across active flagged transfers</div>
            </div>
          </div>
        </div>

        <div className="col-6 col-md-3">
          <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
            <div className="card-body">
              <div className="text-secondary small fw-medium">Resolved Decisions</div>
              <div className="h3 fw-bold text-success my-1">
                {totalResolved}
              </div>
              <div className="small text-muted">
                {stats ? `${stats.resolvedReleased} released, ${stats.resolvedBlocked} blocked` : 'Triaged cases'}
              </div>
            </div>
          </div>
        </div>

        <div className="col-6 col-md-3">
          <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
            <div className="card-body">
              <div className="text-secondary small fw-medium">False Positive Rate</div>
              <div className="h3 fw-bold text-primary my-1">
                {fpRate}%
              </div>
              <div className="small text-muted">Legitimate transfers released</div>
            </div>
          </div>
        </div>
      </div>

      {/* Filter and Sort Toolbar */}
      <div className="card border shadow-sm mb-4" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
        <div className="card-body py-3">
          <div className="row g-2 align-items-center">
            {/* Status Filter */}
            <div className="col-6 col-md-3">
              <label className="form-label small text-secondary mb-1">Status Filter</label>
              <select
                className="form-select form-select-sm"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="ALL">All Alert Statuses</option>
                <option value="OPEN">Open (Pending Triage)</option>
                <option value="IN_REVIEW">In Review</option>
                <option value="RESOLVED_RELEASED">Resolved (Released)</option>
                <option value="RESOLVED_BLOCKED">Resolved (Blocked)</option>
              </select>
            </div>

            {/* Severity Filter */}
            <div className="col-6 col-md-3">
              <label className="form-label small text-secondary mb-1">Severity Filter</label>
              <select
                className="form-select form-select-sm"
                value={severityFilter}
                onChange={(e) => setSeverityFilter(e.target.value)}
              >
                <option value="ALL">All Severities</option>
                <option value="HIGH">High (Score &ge; 70)</option>
                <option value="MEDIUM">Medium (Score 40–69)</option>
                <option value="LOW">Low (Score &lt; 40)</option>
              </select>
            </div>

            {/* Sorting */}
            <div className="col-12 col-md-4">
              <label className="form-label small text-secondary mb-1">Queue Ordering</label>
              <select
                className="form-select form-select-sm"
                value={sortOrder}
                onChange={(e) => setSortOrder(e.target.value as 'SEVERITY_THEN_AGE' | 'NEWEST_FIRST')}
              >
                <option value="SEVERITY_THEN_AGE">Sort: Severity (High to Low), then Age</option>
                <option value="NEWEST_FIRST">Sort: Newest First</option>
              </select>
            </div>

            {/* Total Count */}
            <div className="col-12 col-md-2 text-md-end pt-md-3">
              <span className="badge bg-secondary-subtle text-secondary">
                {sortedAlerts.length} Cases Loaded
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Alert Queue Table */}
      <div className="card border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
        <div className="card-body p-0">
          {loading ? (
            <div className="p-4">
              <Skeleton height="3rem" className="mb-2" />
              <Skeleton height="3rem" className="mb-2" />
              <Skeleton height="3rem" />
            </div>
          ) : error ? (
            <div className="p-4">
              <EmptyState
                icon="bi-exclamation-triangle"
                title="Error Loading Alerts"
                description={error}
                action={
                  <button onClick={fetchAlerts} className="btn btn-outline-primary btn-sm">
                    Retry
                  </button>
                }
              />
            </div>
          ) : sortedAlerts.length === 0 ? (
            <div className="p-5 text-center">
              <EmptyState
                icon="bi-shield-check"
                title="No Fraud Alerts Matching Filter"
                description="The triage queue is clean for this criteria. New flagged transactions will appear instantly."
              />
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0">
                <thead className="table-light small text-secondary">
                  <tr>
                    <th className="ps-3">Alert ID</th>
                    <th>Severity</th>
                    <th>Risk Score</th>
                    <th>Txn Reference</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Flagged At</th>
                    <th className="pe-3 text-end">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedAlerts.map((a) => (
                    <tr
                      key={a.id}
                      onClick={() => handleOpenInspection(a)}
                      style={{ cursor: 'pointer' }}
                    >
                      <td className="ps-3 font-monospace fw-bold text-primary">#{a.id}</td>
                      <td>{getSeverityBadge(a.riskLevel)}</td>
                      <td>
                        <div className="d-flex align-items-center gap-2">
                          <div className="progress flex-grow-1" style={{ width: '60px', height: '6px' }}>
                            <div
                              className={`progress-bar ${
                                a.riskScore >= 70 ? 'bg-danger' : a.riskScore >= 40 ? 'bg-warning' : 'bg-success'
                              }`}
                              style={{ width: `${Math.min(100, a.riskScore)}%` }}
                            ></div>
                          </div>
                          <span className="fw-bold small">{a.riskScore}</span>
                        </div>
                      </td>
                      <td className="font-monospace small">
                        {a.transaction?.referenceId || 'N/A'}
                      </td>
                      <td className="fw-semibold">
                        ₹ {a.transaction?.amount ? a.transaction.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 }) : '0.00'}
                      </td>
                      <td>{getStatusBadge(a.status)}</td>
                      <td className="small text-muted">
                        {new Date(a.createdAt).toLocaleDateString([], {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </td>
                      <td className="pe-3 text-end">
                        <button
                          className="btn btn-outline-primary btn-sm py-0 px-2"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleOpenInspection(a);
                          }}
                        >
                          Inspect <i className="bi bi-chevron-right"></i>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Alert Forensics & Resolution Modal */}
      {selectedAlert && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.65)' }}>
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content border shadow" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
              <div className="modal-header">
                <div>
                  <h5 className="modal-title fw-bold mb-0 d-flex align-items-center gap-2">
                    <span>Alert #{selectedAlert.id} Forensics Dossier</span>
                    {getSeverityBadge(selectedAlert.riskLevel)}
                    {getStatusBadge(selectedAlert.status)}
                  </h5>
                  <span className="font-monospace text-secondary small">
                    Txn Ref: {selectedAlert.transaction?.referenceId}
                  </span>
                </div>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setSelectedAlert(null)}
                  disabled={actionLoading}
                  aria-label="Close"
                ></button>
              </div>

              <div className="modal-body">
                {/* Risk Gauge Header */}
                <div className="p-3 rounded bg-body-tertiary border mb-3">
                  <div className="d-flex justify-content-between align-items-center mb-1">
                    <span className="fw-semibold small">Calculated Heuristic Risk Score</span>
                    <span className="h4 fw-bold mb-0">
                      {selectedAlert.riskScore} <span className="text-secondary fs-6">/ 100</span>
                    </span>
                  </div>

                  {/* Meter with threshold indicators */}
                  <div className="position-relative my-2">
                    <div className="progress" style={{ height: '14px', borderRadius: '7px' }}>
                      <div
                        className={`progress-bar ${
                          selectedAlert.riskScore >= 70
                            ? 'bg-danger'
                            : selectedAlert.riskScore >= 40
                            ? 'bg-warning'
                            : 'bg-success'
                        }`}
                        style={{ width: `${Math.min(100, selectedAlert.riskScore)}%` }}
                      ></div>
                    </div>
                  </div>

                  <div className="d-flex justify-content-between text-secondary" style={{ fontSize: '0.75rem' }}>
                    <span>0: Low Risk</span>
                    <span>40: Review Threshold</span>
                    <span>70: Block Threshold</span>
                    <span>100: Max Score</span>
                  </div>
                </div>

                {/* Transaction & Counterparty Details */}
                <div className="row g-3 mb-3">
                  <div className="col-md-6">
                    <div className="p-3 rounded border h-100">
                      <h6 className="fw-bold mb-2 small text-secondary text-uppercase">Transaction Summary</h6>
                      <ul className="list-group list-group-flush small bg-transparent">
                        <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                          <span className="text-secondary">Amount:</span>
                          <span className="fw-bold fs-6">
                            ₹ {selectedAlert.transaction?.amount ? selectedAlert.transaction.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 }) : '0.00'}
                          </span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                          <span className="text-secondary">Source Account:</span>
                          <span className="font-monospace">
                            {selectedAlert.transaction?.sourceMaskedAccountNumber || 'N/A'}
                          </span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                          <span className="text-secondary">Recipient Account:</span>
                          <span className="font-monospace">
                            {selectedAlert.transaction?.destinationMaskedAccountNumber || 'N/A'}
                          </span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                          <span className="text-secondary">Created Timestamp:</span>
                          <span className="text-muted">
                            {new Date(selectedAlert.createdAt).toLocaleString()}
                          </span>
                        </li>
                      </ul>
                    </div>
                  </div>

                  <div className="col-md-6">
                    <div className="p-3 rounded border h-100">
                      <h6 className="fw-bold mb-2 small text-secondary text-uppercase">Account Security Context</h6>
                      <ul className="list-group list-group-flush small bg-transparent">
                        <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                          <span className="text-secondary">Held Balance Status:</span>
                          <span className="fw-semibold text-warning-emphasis">
                            Reserved from available balance
                          </span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                          <span className="text-secondary">Idempotency Checked:</span>
                          <span className="text-success fw-semibold">YES &bull; Verified</span>
                        </li>
                        <li className="list-group-item d-flex justify-content-between px-0 bg-transparent">
                          <span className="text-secondary">Locking Order:</span>
                          <span className="font-monospace">Ascending ID</span>
                        </li>
                        {selectedAlert.decisionNotes && (
                          <li className="list-group-item px-0 bg-transparent pt-2">
                            <span className="text-secondary d-block">Resolution Notes:</span>
                            <span className="fst-italic">{selectedAlert.decisionNotes}</span>
                          </li>
                        )}
                      </ul>
                    </div>
                  </div>
                </div>

                {/* Rule-Hit Breakdown Table */}
                <div className="mb-3">
                  <h6 className="fw-bold mb-2 small text-secondary text-uppercase">
                    Rule-Hit Breakdown ({selectedAlert.hits?.length || 0} Triggers)
                  </h6>
                  {!selectedAlert.hits || selectedAlert.hits.length === 0 ? (
                    <div className="p-3 border rounded text-secondary small bg-body-tertiary">
                      No individual rule score contributions recorded for this alert.
                    </div>
                  ) : (
                    <div className="table-responsive border rounded">
                      <table className="table table-sm mb-0">
                        <thead className="table-light small text-secondary">
                          <tr>
                            <th className="ps-2">Rule Code</th>
                            <th>Rule Name</th>
                            <th>Score Contribution</th>
                            <th className="pe-2">Reason Explanation</th>
                          </tr>
                        </thead>
                        <tbody className="small">
                          {selectedAlert.hits.map((h, i) => (
                            <tr key={i}>
                              <td className="ps-2 font-monospace fw-bold text-primary">{h.ruleCode}</td>
                              <td className="fw-medium">{h.ruleName}</td>
                              <td>
                                <span className="badge bg-danger-subtle text-danger border border-danger-subtle">
                                  +{h.scoreContribution} pts
                                </span>
                              </td>
                              <td className="pe-2 text-secondary">{h.reasonExplanation}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>

                {/* Investigation Outcome Notes (Mandatory for Resolution) */}
                {!isReadOnlyAdmin && (selectedAlert.status === 'OPEN' || selectedAlert.status === 'IN_REVIEW') ? (
                  <div>
                    <label className="form-label small fw-semibold">
                      Analyst Investigation Notes <span className="text-danger">*</span>
                    </label>
                    <textarea
                      className="form-control form-control-sm"
                      rows={3}
                      placeholder="Detail the forensic findings, KYC verification, phone call confirmation, or fraud markers before deciding..."
                      value={decisionNotes}
                      onChange={(e) => setDecisionNotes(e.target.value)}
                    ></textarea>
                    <div className="form-text small">
                      Mandatory audit trail. Recorded permanently with your user ID and IP address.
                    </div>
                  </div>
                ) : isReadOnlyAdmin ? (
                  <div className="alert alert-info py-2 px-3 small mb-0">
                    <i className="bi bi-info-circle me-1"></i>
                    Administrator Surveillance Mode: Read-only observation. Per PRD Section 9, resolution decisions can only be submitted by users with the <code>FRAUD_ANALYST</code> role.
                  </div>
                ) : (
                  <div className="alert alert-secondary py-2 px-3 small mb-0">
                    <i className="bi bi-check-circle me-1"></i>
                    This case has already been resolved ({selectedAlert.status}). Decision notes: "{selectedAlert.decisionNotes || 'None'}"
                  </div>
                )}
              </div>

              <div className="modal-footer d-flex justify-content-between">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setSelectedAlert(null)}
                  disabled={actionLoading}
                >
                  Close Dossier
                </button>

                {!isReadOnlyAdmin && (selectedAlert.status === 'OPEN' || selectedAlert.status === 'IN_REVIEW') && (
                  <div className="d-flex gap-2">
                    <button
                      type="button"
                      className="btn btn-outline-danger btn-sm d-flex align-items-center gap-1"
                      onClick={() => setConfirmDecision('CONFIRM_BLOCK')}
                      disabled={actionLoading || !decisionNotes.trim()}
                    >
                      <i className="bi bi-x-circle"></i>
                      <span>Confirm Fraud Block</span>
                    </button>
                    <button
                      type="button"
                      className="btn btn-success btn-sm d-flex align-items-center gap-1"
                      onClick={() => setConfirmDecision('RELEASE')}
                      disabled={actionLoading || !decisionNotes.trim()}
                    >
                      <i className="bi bi-check-circle"></i>
                      <span>Release Transfer</span>
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal for Release or Confirm Block */}
      {confirmDecision && (
        <ConfirmModal
          isOpen={true}
          title={confirmDecision === 'RELEASE' ? 'Release Held Transfer?' : 'Confirm Fraud Block?'}
          message={
            confirmDecision === 'RELEASE'
              ? `Are you sure you want to approve Alert #${selectedAlert?.id}? Funds will be deducted from sender and instantly credited to recipient. The customer will receive an alert notification.`
              : `Are you sure you want to permanently block Alert #${selectedAlert?.id}? Reserved funds will be released back to the sender account, and an alert notification will be dispatched.`
          }
          confirmLabel={confirmDecision === 'RELEASE' ? 'Release Transfer' : 'Confirm Block'}
          cancelLabel="Back to Review"
          confirmVariant={confirmDecision === 'RELEASE' ? 'success' : 'danger'}
          isLoading={actionLoading}
          onConfirm={handleExecuteDecision}
          onCancel={() => setConfirmDecision(null)}
        />
      )}
    </div>
  );
};
