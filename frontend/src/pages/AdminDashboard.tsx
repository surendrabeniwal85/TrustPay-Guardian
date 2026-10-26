import React, { useState, useEffect, useCallback } from 'react';
import { api, getErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';
import {
  AdminDashboardMetrics,
  UserSummary,
  FraudRule,
  SystemSetting,
  AuditLog,
  ApiResponse,
  Page,
  Role,
  UserStatus,
} from '../types';

export const AdminDashboard: React.FC = () => {
  const { user: currentUser } = useAuth();
  const [activeTab, setActiveTab] = useState<'metrics' | 'users' | 'rules' | 'settings' | 'audit'>('metrics');

  const [metrics, setMetrics] = useState<AdminDashboardMetrics | null>(null);
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [rules, setRules] = useState<FraudRule[]>([]);
  const [settings, setSettings] = useState<SystemSetting[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);

  const [loading, setLoading] = useState<boolean>(true);
  const [actionLoading, setActionLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Rule editing state
  const [editingRuleId, setEditingRuleId] = useState<number | null>(null);
  const [rulePoints, setRulePoints] = useState<number>(30);
  const [ruleEnabled, setRuleEnabled] = useState<boolean>(true);

  // Setting editing state
  const [editingSettingId, setEditingSettingId] = useState<number | null>(null);
  const [settingValue, setSettingValue] = useState<string>('');

  const loadMetrics = async () => {
    try {
      const res = await api.get<ApiResponse<AdminDashboardMetrics>>('/admin/dashboard');
      setMetrics(res.data.data);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const loadUsers = async () => {
    try {
      const res = await api.get<ApiResponse<Page<UserSummary>>>('/admin/users?size=50');
      setUsers(res.data.data.content || []);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const loadRules = async () => {
    try {
      const res = await api.get<ApiResponse<FraudRule[]>>('/admin/fraud-rules');
      setRules(res.data.data || []);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const loadSettings = async () => {
    try {
      const res = await api.get<ApiResponse<SystemSetting[]>>('/admin/settings');
      setSettings(res.data.data || []);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const loadAuditLogs = async () => {
    try {
      const res = await api.get<ApiResponse<Page<AuditLog>>>('/admin/audit-logs?size=50');
      setAuditLogs(res.data.data.content || []);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const loadActiveTabData = useCallback(async () => {
    setLoading(true);
    setError(null);
    if (activeTab === 'metrics') await loadMetrics();
    else if (activeTab === 'users') await loadUsers();
    else if (activeTab === 'rules') await loadRules();
    else if (activeTab === 'settings') await loadSettings();
    else if (activeTab === 'audit') await loadAuditLogs();
    setLoading(false);
  }, [activeTab]);

  useEffect(() => {
    loadActiveTabData();
  }, [loadActiveTabData]);

  // Handle User Role Change
  const handleRoleChange = async (targetUserId: number, newRole: Role) => {
    setActionLoading(true);
    setError(null);
    try {
      await api.patch<ApiResponse<UserSummary>>(`/admin/users/${targetUserId}/role`, { role: newRole });
      setSuccessMsg('User role updated successfully.');
      await loadUsers();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  // Handle User Status Change
  const handleStatusChange = async (targetUserId: number, newStatus: UserStatus) => {
    setActionLoading(true);
    setError(null);
    try {
      await api.patch<ApiResponse<UserSummary>>(`/admin/users/${targetUserId}/status`, { status: newStatus });
      setSuccessMsg('User status updated successfully.');
      await loadUsers();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  // Save Rule Tuning
  const handleSaveRule = async (ruleId: number) => {
    setActionLoading(true);
    setError(null);
    try {
      await api.put<ApiResponse<FraudRule>>(`/admin/fraud-rules/${ruleId}`, {
        scorePoints: rulePoints,
        isEnabled: ruleEnabled,
      });
      setSuccessMsg('Fraud rule score points updated successfully.');
      setEditingRuleId(null);
      await loadRules();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  // Save System Setting
  const handleSaveSetting = async (settingId: number) => {
    setActionLoading(true);
    setError(null);
    try {
      await api.put<ApiResponse<SystemSetting>>(`/admin/settings/${settingId}`, {
        settingValue,
      });
      setSuccessMsg('System setting updated successfully.');
      setEditingSettingId(null);
      await loadSettings();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setActionLoading(false);
    }
  };

  const formatCurrency = (amt?: number) => {
    if (amt === undefined || amt === null) return '₹0.00';
    return `₹${amt.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  return (
    <div className="container py-4">
      {/* Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-2">
        <div>
          <h2 className="fw-bold mb-1 d-flex align-items-center gap-2">
            <i className="bi bi-sliders text-danger"></i>
            Platform Administration Console
          </h2>
          <p className="text-secondary mb-0 small">
            System metrics, user role governance, configurable fraud rules, and immutable audit logs
          </p>
        </div>
      </div>

      {/* Status Messages */}
      {error && (
        <div className="alert alert-danger alert-dismissible fade show small" role="alert">
          <i className="bi bi-exclamation-triangle-fill me-2"></i>
          <strong>Error:</strong> {error}
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

      {/* Navigation Tabs */}
      <ul className="nav nav-pills gap-2 mb-4">
        <li className="nav-item">
          <button
            onClick={() => setActiveTab('metrics')}
            className={`btn btn-sm rounded-pill px-3 ${activeTab === 'metrics' ? 'btn-primary' : 'btn-outline-secondary'}`}
          >
            <i className="bi bi-bar-chart-line me-1"></i> System Metrics
          </button>
        </li>
        <li className="nav-item">
          <button
            onClick={() => setActiveTab('users')}
            className={`btn btn-sm rounded-pill px-3 ${activeTab === 'users' ? 'btn-primary' : 'btn-outline-secondary'}`}
          >
            <i className="bi bi-people me-1"></i> User Management
          </button>
        </li>
        <li className="nav-item">
          <button
            onClick={() => setActiveTab('rules')}
            className={`btn btn-sm rounded-pill px-3 ${activeTab === 'rules' ? 'btn-primary' : 'btn-outline-secondary'}`}
          >
            <i className="bi bi-shield-check me-1"></i> Fraud Rules Tuning
          </button>
        </li>
        <li className="nav-item">
          <button
            onClick={() => setActiveTab('settings')}
            className={`btn btn-sm rounded-pill px-3 ${activeTab === 'settings' ? 'btn-primary' : 'btn-outline-secondary'}`}
          >
            <i className="bi bi-gear me-1"></i> System Settings
          </button>
        </li>
        <li className="nav-item">
          <button
            onClick={() => setActiveTab('audit')}
            className={`btn btn-sm rounded-pill px-3 ${activeTab === 'audit' ? 'btn-primary' : 'btn-outline-secondary'}`}
          >
            <i className="bi bi-journal-text me-1"></i> Immutable Audit Logs
          </button>
        </li>
      </ul>

      {/* Loading state indicator */}
      {loading && (
        <div className="text-center py-4">
          <div className="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
          <span className="small text-secondary">Loading dashboard data...</span>
        </div>
      )}

      {/* Tab 1: System Metrics */}
      {activeTab === 'metrics' && metrics && (
        <div className="row g-3">
          <div className="col-6 col-md-3">
            <div className="tpg-card p-3">
              <span className="small text-secondary text-uppercase fw-semibold">Total Users</span>
              <div className="fs-3 fw-bold text-primary">{metrics.totalUsers}</div>
              <small className="text-success">{metrics.activeUsers} active</small>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="tpg-card p-3">
              <span className="small text-secondary text-uppercase fw-semibold">Total Accounts</span>
              <div className="fs-3 fw-bold text-primary">{metrics.totalAccounts}</div>
              <small className="text-secondary">Simulated customer accounts</small>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="tpg-card p-3">
              <span className="small text-secondary text-uppercase fw-semibold">Total System Balance</span>
              <div className="fs-4 fw-bold text-success">{formatCurrency(metrics.totalBalance)}</div>
              <small className="text-secondary">Aggregate ledger</small>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="tpg-card p-3">
              <span className="small text-secondary text-uppercase fw-semibold">Active Fraud Rules</span>
              <div className="fs-3 fw-bold text-warning">{metrics.activeRules}</div>
              <small className="text-secondary">R1–R7 enabled</small>
            </div>
          </div>

          <div className="col-6 col-md-3">
            <div className="tpg-card p-3">
              <span className="small text-secondary text-uppercase fw-semibold">Total Transactions</span>
              <div className="fs-3 fw-bold text-primary">{metrics.totalTransactions}</div>
              <small className="text-secondary">Simulated volume</small>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="tpg-card p-3">
              <span className="small text-secondary text-uppercase fw-semibold">Approved Transfers</span>
              <div className="fs-3 fw-bold text-success">{metrics.approvedTransactions}</div>
              <small className="text-secondary">Passed risk checks</small>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="tpg-card p-3">
              <span className="small text-secondary text-uppercase fw-semibold">Held in Review</span>
              <div className="fs-3 fw-bold text-warning">{metrics.heldTransactions}</div>
              <small className="text-secondary">Reservation active</small>
            </div>
          </div>
          <div className="col-6 col-md-3">
            <div className="tpg-card p-3">
              <span className="small text-secondary text-uppercase fw-semibold">Blocked Transfers</span>
              <div className="fs-3 fw-bold text-danger">{metrics.blockedTransactions}</div>
              <small className="text-secondary">Critical risk rule hits</small>
            </div>
          </div>
        </div>
      )}

      {/* Tab 2: User Management */}
      {activeTab === 'users' && (
        <div className="tpg-card p-4">
          <h5 className="fw-bold mb-3">User Directory & Role Governance</h5>
          <div className="table-responsive">
            <table className="table table-hover align-middle small mb-0">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name & Email</th>
                  <th>Phone</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Failed Logins</th>
                  <th>Registered</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => {
                  const isSelf = u.id === currentUser?.id;
                  return (
                    <tr key={u.id}>
                      <td className="fw-bold">#{u.id}</td>
                      <td>
                        <div className="fw-semibold">{u.fullName}</div>
                        <div className="text-secondary font-monospace" style={{ fontSize: '0.75rem' }}>
                          {u.email}
                        </div>
                      </td>
                      <td>{u.phoneNumber}</td>
                      <td>
                        <select
                          disabled={isSelf || actionLoading}
                          className="form-select form-select-sm"
                          style={{ width: '130px' }}
                          value={u.role}
                          onChange={(e) => handleRoleChange(u.id, e.target.value as Role)}
                          title={isSelf ? 'Cannot modify own role (self-demotion prevented)' : undefined}
                        >
                          <option value="CUSTOMER">CUSTOMER</option>
                          <option value="FRAUD_ANALYST">ANALYST</option>
                          <option value="ADMIN">ADMIN</option>
                        </select>
                      </td>
                      <td>
                        <span
                          className={`badge ${
                            u.status === 'ACTIVE'
                              ? 'bg-success-subtle text-success'
                              : u.status === 'SUSPENDED'
                              ? 'bg-warning-subtle text-warning'
                              : 'bg-danger-subtle text-danger'
                          }`}
                        >
                          {u.status}
                        </span>
                      </td>
                      <td>
                        {u.failedLoginAttempts || 0}
                        {u.lockoutUntil && (
                          <span className="badge bg-danger ms-1">LOCKED</span>
                        )}
                      </td>
                      <td>
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : 'N/A'}
                      </td>
                      <td className="text-end">
                        <button
                          disabled={isSelf || actionLoading}
                          onClick={() => handleStatusChange(u.id, u.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE')}
                          className={`btn btn-sm rounded-pill ${
                            u.status === 'ACTIVE' ? 'btn-outline-warning' : 'btn-outline-success'
                          }`}
                          title={isSelf ? 'Cannot modify own account status' : undefined}
                        >
                          {u.status === 'ACTIVE' ? 'Suspend' : 'Activate'}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab 3: Fraud Rules Tuning */}
      {activeTab === 'rules' && (
        <div className="tpg-card p-4">
          <h5 className="fw-bold mb-3">Configurable Fraud Rule Weights (R1–R7)</h5>
          <p className="text-secondary small mb-3">
            Adjust score points (0–100) or toggle rules on/off in real-time. Changes are logged to the immutable audit trail.
          </p>
          <div className="table-responsive">
            <table className="table table-hover align-middle small mb-0">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Rule Name</th>
                  <th>Description</th>
                  <th>Score Points</th>
                  <th>Status</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {rules.map((rule) => {
                  const isEditing = editingRuleId === rule.id;
                  return (
                    <tr key={rule.id}>
                      <td className="fw-bold font-monospace text-primary">{rule.ruleCode}</td>
                      <td className="fw-semibold">{rule.name}</td>
                      <td className="text-secondary">{rule.description}</td>
                      <td style={{ width: '130px' }}>
                        {isEditing ? (
                          <input
                            type="number"
                            min="0"
                            max="100"
                            className="form-control form-control-sm"
                            value={rulePoints}
                            onChange={(e) => setRulePoints(Number(e.target.value))}
                          />
                        ) : (
                          <span className="badge bg-primary fs-6">{rule.points} pts</span>
                        )}
                      </td>
                      <td>
                        {isEditing ? (
                          <div className="form-check form-switch">
                            <input
                              className="form-check-input"
                              type="checkbox"
                              checked={ruleEnabled}
                              onChange={(e) => setRuleEnabled(e.target.checked)}
                            />
                          </div>
                        ) : (
                          <span className={`badge ${rule.isEnabled ? 'bg-success' : 'bg-secondary'}`}>
                            {rule.isEnabled ? 'ENABLED' : 'DISABLED'}
                          </span>
                        )}
                      </td>
                      <td className="text-end">
                        {isEditing ? (
                          <div className="d-flex justify-content-end gap-1">
                            <button
                              disabled={actionLoading}
                              onClick={() => handleSaveRule(rule.id)}
                              className="btn btn-success btn-sm rounded-pill px-3"
                            >
                              Save
                            </button>
                            <button
                              onClick={() => setEditingRuleId(null)}
                              className="btn btn-outline-secondary btn-sm rounded-pill"
                            >
                              Cancel
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() => {
                              setEditingRuleId(rule.id);
                              setRulePoints(rule.points);
                              setRuleEnabled(rule.isEnabled);
                            }}
                            className="btn btn-outline-primary btn-sm rounded-pill px-3"
                          >
                            Edit
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab 4: System Settings */}
      {activeTab === 'settings' && (
        <div className="tpg-card p-4">
          <h5 className="fw-bold mb-3">System Operating Settings</h5>
          <div className="table-responsive">
            <table className="table table-hover align-middle small mb-0">
              <thead>
                <tr>
                  <th>Setting Key</th>
                  <th>Current Value</th>
                  <th>Description</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {settings.map((s) => {
                  const isEditing = editingSettingId === s.id;
                  return (
                    <tr key={s.id}>
                      <td className="fw-bold font-monospace">{s.settingKey}</td>
                      <td style={{ width: '160px' }}>
                        {isEditing ? (
                          <input
                            type="text"
                            className="form-control form-control-sm"
                            value={settingValue}
                            onChange={(e) => setSettingValue(e.target.value)}
                          />
                        ) : (
                          <span className="badge bg-secondary-subtle text-body border font-monospace">
                            {s.settingValue}
                          </span>
                        )}
                      </td>
                      <td className="text-secondary">{s.description}</td>
                      <td className="text-end">
                        {isEditing ? (
                          <div className="d-flex justify-content-end gap-1">
                            <button
                              disabled={actionLoading}
                              onClick={() => handleSaveSetting(s.id)}
                              className="btn btn-success btn-sm rounded-pill px-3"
                            >
                              Save
                            </button>
                            <button
                              onClick={() => setEditingSettingId(null)}
                              className="btn btn-outline-secondary btn-sm rounded-pill"
                            >
                              Cancel
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() => {
                              setEditingSettingId(s.id);
                              setSettingValue(s.settingValue);
                            }}
                            className="btn btn-outline-primary btn-sm rounded-pill px-3"
                          >
                            Edit
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab 5: Immutable Audit Logs */}
      {activeTab === 'audit' && (
        <div className="tpg-card p-4">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <div>
              <h5 className="fw-bold mb-0">Immutable Audit Trail</h5>
              <small className="text-secondary">
                Database-enforced non-repudiation log of all admin, analyst, and security events
              </small>
            </div>
            <button
              onClick={loadAuditLogs}
              className="btn btn-outline-secondary btn-sm rounded-pill"
            >
              <i className="bi bi-arrow-clockwise me-1"></i> Refresh
            </button>
          </div>
          <div className="table-responsive">
            <table className="table table-hover align-middle small mb-0 font-monospace">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Actor</th>
                  <th>Action</th>
                  <th>Target</th>
                  <th>Payload</th>
                  <th>IP Address</th>
                </tr>
              </thead>
              <tbody>
                {auditLogs.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="text-center py-4 text-secondary">
                      No audit events recorded yet.
                    </td>
                  </tr>
                ) : (
                  auditLogs.map((log) => (
                    <tr key={log.id}>
                      <td>{new Date(log.createdAt).toLocaleString()}</td>
                      <td>
                        <span className="fw-bold">{log.actorName || 'SYSTEM'}</span>
                        {log.actorEmail && (
                          <span className="d-block text-secondary small">{log.actorEmail}</span>
                        )}
                      </td>
                      <td>
                        <span className="badge bg-secondary">{log.action}</span>
                      </td>
                      <td>
                        <span className="text-secondary">{log.targetType}</span> #{log.targetId}
                      </td>
                      <td style={{ maxWidth: '300px', overflowX: 'auto' }}>
                        <code>{log.detailsJson}</code>
                      </td>
                      <td className="text-secondary">{log.ipAddress || '127.0.0.1'}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
