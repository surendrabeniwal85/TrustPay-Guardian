import React, { useState, useEffect, useCallback } from 'react';
import { api, getErrorMessage } from '../services/api';
import { FraudAlert, ApiResponse, Page } from '../types';

export const AnalystDashboard: React.FC = () => {
  const [alerts, setAlerts] = useState<FraudAlert[]>([]);
  const [selectedAlert, setSelectedAlert] = useState<FraudAlert | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [loading, setLoading] = useState<boolean>(true);
  const [actionLoading, setActionLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [decisionNotes, setDecisionNotes] = useState<string>('');

  const fetchAlerts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      let url = '/fraud/alerts?size=50';
      if (statusFilter !== 'ALL') {
        url += `&status=${statusFilter}`;
      }
      const res = await api.get<ApiResponse<Page<FraudAlert>>>(url);
      setAlerts(res.data.data.content || []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    fetchAlerts();
  }, [fetchAlerts]);

  const openInspection = async (alert: FraudAlert) => {
    try {
      const res = await api.get<ApiResponse<FraudAlert>>(`/fraud/alerts/${alert.id}`);
      setSelectedAlert(res.data.data);
      setDecisionNotes('');
    } catch {
      setSelectedAlert(alert);
    }
  };

  const handleDecision = async (decision: 'RELEASE' | 'CONFIRM_BLOCK') => {
    if (!selectedAlert) return;
    if (!decisionNotes.trim()) {
      setError('Investigation outcome notes are required before submitting a decision.');
      return;
    }

    setActionLoading(true);
    setError(null);
    setSuccessMsg(null);

    try {
      await api.post<ApiResponse<FraudAlert>>(`/fraud/alerts/${selectedAlert.id}/decision`, {
        decision,
        decisionNotes,
      });

      setSuccessMsg(
        decision === 'RELEASE'
          ? `Alert #${selectedAlert.id} approved & released. Funds posted to recipient account.`
          : `Alert #${selectedAlert.id} confirmed fraudulent & blocked. Reserved funds released back to sender.`
      );
      setSelectedAlert(null);
      await fetchAlerts();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const openCount = alerts.filter((a) => a.status === 'OPEN').length;
  const inReviewCount = alerts.filter((a) => a.status === 'IN_REVIEW').length;
  const releasedCount = alerts.filter((a) => a.status === 'RESOLVED_RELEASED').length;
  const blockedCount = alerts.filter((a) => a.status === 'RESOLVED_BLOCKED').length;

  return (
    <div className="container py-4">
      {/* Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-2">
        <div>
          <h2 className="fw-bold mb-1 d-flex align-items-center gap-2">
            <i className="bi bi-shield-shaded text-warning"></i>
            Fraud Investigation Console
          </h2>
          <p className="text-secondary mb-0 small">
            Real-time suspicious transaction triage, rule hit explainability, and analyst resolution
          </p>
        </div>
        <button
          onClick={fetchAlerts}
          className="btn btn-outline-secondary btn-sm rounded-pill px-3"
        >
          <i className="bi bi-arrow-clockwise me-1"></i> Refresh Queue
        </button>
      </div>

      {/* Status Messages */}
      {error && (
        <div className="alert alert-danger alert-dismissible fade show small" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>
          {error}
          <button type="button" className="btn-close" onClick={() => setError(null)}></button>
        </div>
      )}

      {successMsg && (
        <div className="alert alert-success alert-dismissible fade show small" role="alert">
          <i className="bi bi-check-circle-fill me-2"></i>
          {successMsg}
          <button type="button" className="btn-close" onClick={() => setSuccessMsg(null)}></button>
        </div>
      )}

      {/* Queue Metrics Cards */}
      <div className="row g-3 mb-4">
        <div className="col-6 col-md-3">
          <div className="tpg-card p-3">
            <small className="text-secondary text-uppercase fw-semibold">Open Alerts</small>
            <div className="fs-3 fw-bold text-warning">{openCount}</div>
            <small className="text-muted">Awaiting triage</small>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="tpg-card p-3">
            <small className="text-secondary text-uppercase fw-semibold">In Review</small>
            <div className="fs-3 fw-bold text-info">{inReviewCount}</div>
            <small className="text-muted">Assigned</small>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="tpg-card p-3">
            <small className="text-secondary text-uppercase fw-semibold">Released</small>
            <div className="fs-3 fw-bold text-success">{releasedCount}</div>
            <small className="text-muted">Approved</small>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="tpg-card p-3">
            <small className="text-secondary text-uppercase fw-semibold">Blocked</small>
            <div className="fs-3 fw-bold text-danger">{blockedCount}</div>
            <small className="text-muted">Confirmed fraud</small>
          </div>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="d-flex flex-wrap gap-2 mb-3">
        {['ALL', 'OPEN', 'IN_REVIEW', 'RESOLVED_RELEASED', 'RESOLVED_BLOCKED'].map((st) => (
          <button
            key={st}
            onClick={() => setStatusFilter(st)}
            className={`btn btn-sm rounded-pill px-3 ${
              statusFilter === st ? 'btn-primary' : 'btn-outline-secondary'
            }`}
          >
            {st.replace('_', ' ')}
          </button>
        ))}
      </div>

      {/* Alerts Table */}
      <div className="tpg-card p-4">
        <div className="table-responsive">
          <table className="table table-hover align-middle small mb-0">
            <thead>
              <tr>
                <th>Alert ID</th>
                <th>Created At</th>
                <th>Reference ID</th>
                <th>Amount</th>
                <th>Risk Score</th>
                <th>Level</th>
                <th>Status</th>
                <th className="text-end">Action</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} className="text-center py-4">
                    <span className="spinner-border spinner-border-sm me-2"></span> Loading alerts...
                  </td>
                </tr>
              ) : alerts.length === 0 ? (
                <tr>
                  <td colSpan={8} className="text-center py-4 text-secondary">
                    No fraud alerts found for the selected filter.
                  </td>
                </tr>
              ) : (
                alerts.map((alert) => (
                  <tr key={alert.id}>
                    <td className="fw-bold">#{alert.id}</td>
                    <td>
                      {new Date(alert.createdAt).toLocaleDateString([], {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </td>
                    <td className="font-monospace text-secondary">
                      {alert.transaction?.referenceId || 'N/A'}
                    </td>
                    <td className="fw-semibold">
                      ₹{alert.transaction?.amount?.toLocaleString() || '0'}
                    </td>
                    <td>
                      <span
                        className={`badge ${
                          alert.riskScore >= 70
                            ? 'bg-danger'
                            : alert.riskScore >= 40
                            ? 'bg-warning text-dark'
                            : 'bg-success'
                        }`}
                      >
                        {alert.riskScore} / 100
                      </span>
                    </td>
                    <td>
                      <span className="badge bg-secondary-subtle text-secondary">
                        {alert.riskLevel}
                      </span>
                    </td>
                    <td>
                      <span
                        className={`badge ${
                          alert.status === 'OPEN'
                            ? 'bg-warning text-dark'
                            : alert.status === 'RESOLVED_RELEASED'
                            ? 'bg-success'
                            : alert.status === 'RESOLVED_BLOCKED'
                            ? 'bg-danger'
                            : 'bg-info text-dark'
                        }`}
                      >
                        {alert.status}
                      </span>
                    </td>
                    <td className="text-end">
                      <button
                        onClick={() => openInspection(alert)}
                        className="btn btn-outline-primary btn-sm rounded-pill px-3"
                      >
                        Inspect →
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Alert Inspection & Resolution Modal */}
      {selectedAlert && (
        <div
          className="modal show d-block"
          tabIndex={-1}
          style={{ backgroundColor: 'rgba(0,0,0,0.6)' }}
        >
          <div className="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
            <div className="modal-content tpg-card">
              <div className="modal-header border-bottom border-secondary border-opacity-25">
                <div>
                  <h5 className="modal-title fw-bold">
                    Fraud Alert Investigation #{selectedAlert.id}
                  </h5>
                  <small className="text-secondary">
                    Transaction Ref: {selectedAlert.transaction?.referenceId}
                  </small>
                </div>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setSelectedAlert(null)}
                ></button>
              </div>

              <div className="modal-body">
                {/* Composite Risk Score Meter */}
                <div className="p-3 mb-4 rounded border border-secondary border-opacity-25 bg-body-tertiary">
                  <div className="d-flex justify-content-between align-items-center mb-1">
                    <span className="fw-semibold">Composite Risk Score</span>
                    <span className="fs-5 fw-bold text-primary">
                      {selectedAlert.riskScore} / 100
                    </span>
                  </div>
                  <div className="progress" style={{ height: '10px' }}>
                    <div
                      className={`progress-bar ${
                        selectedAlert.riskScore >= 70
                          ? 'bg-danger'
                          : selectedAlert.riskScore >= 40
                          ? 'bg-warning'
                          : 'bg-success'
                      }`}
                      role="progressbar"
                      style={{ width: `${selectedAlert.riskScore}%` }}
                      aria-valuenow={selectedAlert.riskScore}
                      aria-valuemin={0}
                      aria-valuemax={100}
                    ></div>
                  </div>
                  <div className="d-flex justify-content-between mt-1 text-muted" style={{ fontSize: '0.75rem' }}>
                    <span>0 (Low)</span>
                    <span>Review Threshold: 40</span>
                    <span>Block Threshold: 70</span>
                    <span>100 (Critical)</span>
                  </div>
                </div>

                {/* Transaction Information */}
                <h6 className="fw-semibold mb-2">Transaction Details</h6>
                <div className="row g-2 mb-4 small">
                  <div className="col-6">
                    <span className="text-secondary d-block">Source Account</span>
                    <strong className="font-monospace">
                      {selectedAlert.transaction?.sourceMaskedAccountNumber}
                    </strong>
                  </div>
                  <div className="col-6">
                    <span className="text-secondary d-block">Destination Account</span>
                    <strong className="font-monospace">
                      {selectedAlert.transaction?.destinationMaskedAccountNumber}
                    </strong>
                  </div>
                  <div className="col-6">
                    <span className="text-secondary d-block">Transfer Amount</span>
                    <strong className="fs-6 text-primary">
                      ₹{selectedAlert.transaction?.amount?.toLocaleString()}
                    </strong>
                  </div>
                  <div className="col-6">
                    <span className="text-secondary d-block">Timestamp</span>
                    <span>
                      {new Date(selectedAlert.createdAt).toLocaleString()}
                    </span>
                  </div>
                </div>

                {/* Rule Contribution Hits Table */}
                <h6 className="fw-semibold mb-2">
                  Explainable Rule Hits ({selectedAlert.hits?.length || 0})
                </h6>
                <div className="table-responsive mb-4">
                  <table className="table table-sm table-bordered align-middle small mb-0">
                    <thead className="table-light">
                      <tr>
                        <th>Rule</th>
                        <th>Score Contribution</th>
                        <th>Deterministic Explanation</th>
                      </tr>
                    </thead>
                    <tbody>
                      {!selectedAlert.hits || selectedAlert.hits.length === 0 ? (
                        <tr>
                          <td colSpan={3} className="text-center text-muted py-2">
                            No individual rule hits registered.
                          </td>
                        </tr>
                      ) : (
                        selectedAlert.hits.map((hit) => (
                          <tr key={hit.id}>
                            <td className="fw-bold font-monospace text-primary">
                              {hit.ruleCode}
                            </td>
                            <td>
                              <span className="badge bg-danger">+{hit.scoreContribution} pts</span>
                            </td>
                            <td>{hit.reasonExplanation}</td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Investigation Decision / Existing Decision Details */}
                {selectedAlert.status === 'OPEN' || selectedAlert.status === 'IN_REVIEW' ? (
                  <div>
                    <h6 className="fw-semibold mb-2">Analyst Decision & Findings</h6>
                    <div className="mb-3">
                      <label className="form-label small text-secondary">
                        Investigation Notes (Document OTP verification, customer interview, or observed anomaly):
                      </label>
                      <textarea
                        rows={3}
                        required
                        className="form-control small"
                        placeholder="e.g. Verified customer identity via registered phone. Transaction legitimate."
                        value={decisionNotes}
                        onChange={(e) => setDecisionNotes(e.target.value)}
                      ></textarea>
                    </div>
                  </div>
                ) : (
                  <div className="p-3 rounded border border-secondary border-opacity-25 bg-body-tertiary mb-3">
                    <div className="fw-semibold small mb-1">Resolution Outcome:</div>
                    <div className="small text-secondary mb-1">
                      <strong>Decision:</strong> {selectedAlert.status}
                    </div>
                    <div className="small text-secondary mb-1">
                      <strong>Investigator:</strong> {selectedAlert.assignedTo?.fullName || 'Analyst'}
                    </div>
                    <div className="small text-secondary">
                      <strong>Notes:</strong> {selectedAlert.decisionNotes || 'No notes provided'}
                    </div>
                  </div>
                )}
              </div>

              <div className="modal-footer border-top border-secondary border-opacity-25">
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm rounded-pill"
                  onClick={() => setSelectedAlert(null)}
                >
                  Close
                </button>

                {(selectedAlert.status === 'OPEN' || selectedAlert.status === 'IN_REVIEW') && (
                  <div className="d-flex gap-2">
                    <button
                      type="button"
                      disabled={actionLoading}
                      onClick={() => handleDecision('CONFIRM_BLOCK')}
                      className="btn btn-danger btn-sm rounded-pill px-3"
                    >
                      <i className="bi bi-x-octagon me-1"></i>
                      Confirm Fraud & Block
                    </button>
                    <button
                      type="button"
                      disabled={actionLoading}
                      onClick={() => handleDecision('RELEASE')}
                      className="btn btn-success btn-sm rounded-pill px-3"
                    >
                      <i className="bi bi-check2-circle me-1"></i>
                      Approve & Release Funds
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
