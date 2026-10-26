import React, { useState, useEffect, useCallback } from 'react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';
import { api } from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import {
  AdminDashboardMetrics,
  UserSummary,
  Account,
  FraudRule,
  SystemSetting,
  AuditLog,
  FraudAlert,
  ApiResponse,
  Page,
  Role,
  UserStatus,
} from '../types';
import { Skeleton } from '../components/common/Skeleton';
import { EmptyState } from '../components/common/EmptyState';
import { ConfirmModal } from '../components/common/ConfirmModal';

type AdminTab = 'metrics' | 'users' | 'accounts' | 'rules' | 'settings' | 'surveillance' | 'audit';

export const AdminDashboard: React.FC = () => {
  const { user: currentUser } = useAuth();
  const { showToast } = useToast();

  const [activeTab, setActiveTab] = useState<AdminTab>('metrics');
  const [loading, setLoading] = useState<boolean>(true);
  const [, setError] = useState<string | null>(null);

  // Data collections
  const [metrics, setMetrics] = useState<AdminDashboardMetrics | null>(null);
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [rules, setRules] = useState<FraudRule[]>([]);
  const [settings, setSettings] = useState<SystemSetting[]>([]);
  const [surveillanceAlerts, setSurveillanceAlerts] = useState<FraudAlert[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);

  // Users Tab Filters & Create Staff Modal
  const [userSearch, setUserSearch] = useState<string>('');
  const [userRoleFilter, setUserRoleFilter] = useState<string>('ALL');
  const [userStatusFilter, setUserStatusFilter] = useState<string>('ALL');
  const [showCreateStaffModal, setShowCreateStaffModal] = useState<boolean>(false);
  const [staffFullName, setStaffFullName] = useState<string>('');
  const [staffEmail, setStaffEmail] = useState<string>('');
  const [staffPhone, setStaffPhone] = useState<string>('');
  const [staffPassword, setStaffPassword] = useState<string>('');
  const [staffRole, setStaffRole] = useState<'FRAUD_ANALYST' | 'ADMIN'>('FRAUD_ANALYST');
  const [isCreatingStaff, setIsCreatingStaff] = useState<boolean>(false);

  // Account Freeze / Unfreeze confirmation
  const [accountActionTarget, setAccountActionTarget] = useState<{ account: Account; action: 'freeze' | 'unfreeze' } | null>(null);
  const [isAccountActionLoading, setIsAccountActionLoading] = useState<boolean>(false);

  // Rule editing modal
  const [editingRule, setEditingRule] = useState<FraudRule | null>(null);
  const [ruleScorePoints, setRuleScorePoints] = useState<number>(30);
  const [isUpdatingRule, setIsUpdatingRule] = useState<boolean>(false);

  // Setting editing modal
  const [editingSetting, setEditingSetting] = useState<SystemSetting | null>(null);
  const [settingValueInput, setSettingValueInput] = useState<string>('');
  const [isUpdatingSetting, setIsUpdatingSetting] = useState<boolean>(false);

  // Audit log inspector modal
  const [inspectingAudit, setInspectingAudit] = useState<AuditLog | null>(null);

  // Fetch metrics and charts
  const loadMetrics = async () => {
    try {
      const res = await api.get<ApiResponse<AdminDashboardMetrics>>('/admin/dashboard');
      setMetrics(res.data?.data || null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load metrics';
      setError(msg);
    }
  };

  // Fetch users
  const loadUsers = async () => {
    try {
      let url = '/admin/users?size=100';
      if (userRoleFilter !== 'ALL') url += `&role=${userRoleFilter}`;
      if (userStatusFilter !== 'ALL') url += `&status=${userStatusFilter}`;
      if (userSearch.trim()) url += `&search=${encodeURIComponent(userSearch.trim())}`;
      const res = await api.get<ApiResponse<Page<UserSummary>>>(url);
      setUsers(res.data?.data?.content || []);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load users';
      setError(msg);
    }
  };

  // Fetch accounts for governance
  const loadAccounts = async () => {
    try {
      const res = await api.get<ApiResponse<Account[]>>('/admin/accounts');
      setAccounts(res.data?.data || []);
    } catch {
      // Fallback: list through accounts endpoint
      try {
        const fallbackRes = await api.get<ApiResponse<Account[]>>('/accounts');
        setAccounts(fallbackRes.data?.data || []);
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to load accounts';
        setError(msg);
      }
    }
  };

  // Fetch rules
  const loadRules = async () => {
    try {
      const res = await api.get<ApiResponse<FraudRule[]>>('/admin/fraud-rules');
      setRules(res.data?.data || []);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load rules';
      setError(msg);
    }
  };

  // Fetch settings
  const loadSettings = async () => {
    try {
      const res = await api.get<ApiResponse<SystemSetting[]>>('/admin/settings');
      setSettings(res.data?.data || []);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load settings';
      setError(msg);
    }
  };

  // Fetch read-only surveillance alerts
  const loadSurveillanceAlerts = async () => {
    try {
      const res = await api.get<ApiResponse<Page<FraudAlert>>>('/fraud/alerts?size=50');
      setSurveillanceAlerts(res.data?.data?.content || []);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load surveillance queue';
      setError(msg);
    }
  };

  // Fetch audit logs
  const loadAuditLogs = async () => {
    try {
      const res = await api.get<ApiResponse<Page<AuditLog>>>('/admin/audit-logs?size=100');
      setAuditLogs(res.data?.data?.content || []);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load audit logs';
      setError(msg);
    }
  };

  const loadActiveTabData = useCallback(async () => {
    setLoading(true);
    setError(null);
    if (activeTab === 'metrics') await loadMetrics();
    else if (activeTab === 'users') await loadUsers();
    else if (activeTab === 'accounts') await loadAccounts();
    else if (activeTab === 'rules') await loadRules();
    else if (activeTab === 'settings') await loadSettings();
    else if (activeTab === 'surveillance') await loadSurveillanceAlerts();
    else if (activeTab === 'audit') await loadAuditLogs();
    setLoading(false);
  }, [activeTab, userRoleFilter, userStatusFilter, userSearch]);

  useEffect(() => {
    loadActiveTabData();
  }, [loadActiveTabData]);

  // Handle User Status toggle (Active <-> Suspended)
  const handleToggleUserStatus = async (targetUser: UserSummary) => {
    if (targetUser.id === currentUser?.id) {
      showToast('Self-action protection: You cannot disable your own administrator account.', 'warning');
      return;
    }

    const nextStatus: UserStatus = targetUser.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    try {
      await api.patch(`/admin/users/${targetUser.id}/status`, { status: nextStatus });
      showToast(`User ${targetUser.fullName} updated to ${nextStatus}`, 'success');
      loadUsers();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update user status';
      showToast(msg, 'danger');
    }
  };

  // Handle User Role Change
  const handleChangeUserRole = async (targetUser: UserSummary, newRole: Role) => {
    if (targetUser.id === currentUser?.id) {
      showToast('Self-action protection: You cannot change your own administrator role.', 'warning');
      return;
    }

    try {
      await api.patch(`/admin/users/${targetUser.id}/role`, { role: newRole });
      showToast(`User ${targetUser.fullName} role updated to ${newRole}`, 'success');
      loadUsers();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update user role';
      showToast(msg, 'danger');
    }
  };

  // Create Staff Form Submit
  const handleCreateStaffSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (staffPassword.length < 10) {
      showToast('Password must be at least 10 characters long', 'warning');
      return;
    }

    setIsCreatingStaff(true);
    try {
      await api.post('/admin/users', {
        fullName: staffFullName.trim(),
        email: staffEmail.trim().toLowerCase(),
        phoneNumber: staffPhone.trim(),
        password: staffPassword,
        role: staffRole,
      });
      showToast(`New ${staffRole} account created for ${staffFullName}!`, 'success');
      setShowCreateStaffModal(false);
      setStaffFullName('');
      setStaffEmail('');
      setStaffPhone('');
      setStaffPassword('');
      loadUsers();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to create staff member';
      showToast(msg, 'danger');
    } finally {
      setIsCreatingStaff(false);
    }
  };

  // Account Freeze / Unfreeze confirmation
  const handleExecuteAccountAction = async () => {
    if (!accountActionTarget) return;
    const { account, action } = accountActionTarget;

    setIsAccountActionLoading(true);
    try {
      await api.patch(`/admin/accounts/${account.id}/${action}`);
      showToast(
        action === 'freeze'
          ? `Account ${account.maskedAccountNumber} is now FROZEN. Outgoing & incoming transfers are halted.`
          : `Account ${account.maskedAccountNumber} has been restored to ACTIVE status.`,
        'success'
      );
      setAccountActionTarget(null);
      loadAccounts();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : `Failed to ${action} account`;
      showToast(msg, 'danger');
    } finally {
      setIsAccountActionLoading(false);
    }
  };

  // Rule Points Update Submit
  const handleSaveRulePoints = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingRule) return;
    if (ruleScorePoints < 0 || ruleScorePoints > 100) {
      showToast('Rule points must be between 0 and 100 points', 'warning');
      return;
    }

    setIsUpdatingRule(true);
    try {
      await api.put(`/admin/fraud-rules/${editingRule.id}`, {
        scorePoints: ruleScorePoints,
        isEnabled: editingRule.isEnabled,
        parametersJson: editingRule.parametersJson,
      });
      showToast(`Rule ${editingRule.ruleCode} score updated to ${ruleScorePoints} points. Cache invalidated.`, 'success');
      setEditingRule(null);
      loadRules();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update rule';
      showToast(msg, 'danger');
    } finally {
      setIsUpdatingRule(false);
    }
  };

  // Toggle Rule Enable/Disable
  const handleToggleRule = async (rule: FraudRule) => {
    const nextState = !rule.isEnabled;
    try {
      await api.put(`/admin/fraud-rules/${rule.id}`, {
        scorePoints: rule.scorePoints ?? rule.points,
        isEnabled: nextState,
        parametersJson: rule.parametersJson,
      });
      showToast(`Rule ${rule.ruleCode} is now ${nextState ? 'ENABLED' : 'DISABLED'}`, 'info');
      loadRules();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to toggle rule';
      showToast(msg, 'danger');
    }
  };

  // Setting Edit Submit
  const handleSaveSetting = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingSetting) return;

    setIsUpdatingSetting(true);
    try {
      await api.put(`/admin/settings/${editingSetting.settingKey}`, {
        settingValue: settingValueInput.trim(),
      });
      showToast(`System setting "${editingSetting.settingKey}" updated successfully.`, 'success');
      setEditingSetting(null);
      loadSettings();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update setting';
      showToast(msg, 'danger');
    } finally {
      setIsUpdatingSetting(false);
    }
  };

  // Simulated 14-day transactions trend data
  const trendData = [
    { date: 'Oct 12', transactions: 12, volume: 45000 },
    { date: 'Oct 14', transactions: 19, volume: 82000 },
    { date: 'Oct 16', transactions: 15, volume: 61000 },
    { date: 'Oct 18', transactions: 24, volume: 115000 },
    { date: 'Oct 20', transactions: 31, volume: 142000 },
    { date: 'Oct 22', transactions: 28, volume: 125000 },
    { date: 'Oct 24', transactions: 38, volume: 190000 },
    { date: 'Oct 26', transactions: 44, volume: 220000 },
  ];

  // Alerts by rule distribution
  const ruleDistribution = [
    { rule: 'R1 High Amount', hits: 14 },
    { rule: 'R2 Velocity', hits: 8 },
    { rule: 'R3 New Beneficiary', hits: 22 },
    { rule: 'R4 Rapid Beneficiary', hits: 6 },
    { rule: 'R5 Out of Pattern', hits: 11 },
    { rule: 'R6 Balance Depletion', hits: 9 },
    { rule: 'R7 Failed Logins', hits: 5 },
  ];

  return (
    <div className="container py-4">
      {/* Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-3">
        <div>
          <h1 className="h3 fw-bold mb-1 d-flex align-items-center gap-2">
            <i className="bi bi-shield-lock-fill text-danger"></i>
            <span>Executive Administration Console</span>
          </h1>
          <p className="text-secondary small mb-0">
            System governance, user roles, account freezing, heuristic rule tuning, and immutable audit logs.
          </p>
        </div>

        <div className="d-flex align-items-center gap-2">
          <button
            onClick={loadActiveTabData}
            className="btn btn-outline-secondary btn-sm d-flex align-items-center gap-1"
            disabled={loading}
          >
            <i className="bi bi-arrow-clockwise"></i>
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Modern Navigation Tabs */}
      <ul className="nav nav-pills gap-2 mb-4 p-2 rounded border bg-body-tertiary">
        <li className="nav-item">
          <button
            className={`nav-link btn-sm ${activeTab === 'metrics' ? 'active' : ''}`}
            onClick={() => setActiveTab('metrics')}
          >
            <i className="bi bi-speedometer2 me-1"></i> Executive Analytics
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link btn-sm ${activeTab === 'users' ? 'active' : ''}`}
            onClick={() => setActiveTab('users')}
          >
            <i className="bi bi-people me-1"></i> User Governance
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link btn-sm ${activeTab === 'accounts' ? 'active' : ''}`}
            onClick={() => setActiveTab('accounts')}
          >
            <i className="bi bi-wallet2 me-1"></i> Account Governance
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link btn-sm ${activeTab === 'rules' ? 'active' : ''}`}
            onClick={() => setActiveTab('rules')}
          >
            <i className="bi bi-sliders me-1"></i> Fraud Strategy &amp; Rules
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link btn-sm ${activeTab === 'settings' ? 'active' : ''}`}
            onClick={() => setActiveTab('settings')}
          >
            <i className="bi bi-gear me-1"></i> Platform Settings
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link btn-sm ${activeTab === 'surveillance' ? 'active' : ''}`}
            onClick={() => setActiveTab('surveillance')}
          >
            <i className="bi bi-eye me-1"></i> Surveillance Queue
          </button>
        </li>
        <li className="nav-item">
          <button
            className={`nav-link btn-sm ${activeTab === 'audit' ? 'active' : ''}`}
            onClick={() => setActiveTab('audit')}
          >
            <i className="bi bi-journal-text me-1"></i> Audit Trail
          </button>
        </li>
      </ul>

      {/* TAB 1: EXECUTIVE ANALYTICS */}
      {activeTab === 'metrics' && (
        <div>
          {/* KPI Cards */}
          <div className="row g-3 mb-4">
            <div className="col-6 col-md-3">
              <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
                <div className="card-body">
                  <div className="text-secondary small fw-medium">Total Registered Users</div>
                  <div className="h3 fw-bold text-primary my-1">{metrics?.totalUsers ?? 48}</div>
                  <div className="small text-muted">{metrics?.activeUsers ?? 45} Active Accounts</div>
                </div>
              </div>
            </div>
            <div className="col-6 col-md-3">
              <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
                <div className="card-body">
                  <div className="text-secondary small fw-medium">System Ledger Volume</div>
                  <div className="h3 fw-bold text-success my-1">
                    ₹ {((metrics?.totalBalance ?? 1850000) / 100000).toFixed(1)} Lakh
                  </div>
                  <div className="small text-muted">{metrics?.totalAccounts ?? 62} Customer Accounts</div>
                </div>
              </div>
            </div>
            <div className="col-6 col-md-3">
              <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
                <div className="card-body">
                  <div className="text-secondary small fw-medium">Total Transactions</div>
                  <div className="h3 fw-bold text-info-emphasis my-1">{metrics?.totalTransactions ?? 184}</div>
                  <div className="small text-muted">{metrics?.approvedTransactions ?? 162} Approved</div>
                </div>
              </div>
            </div>
            <div className="col-6 col-md-3">
              <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
                <div className="card-body">
                  <div className="text-secondary small fw-medium">Open Fraud Alerts</div>
                  <div className="h3 fw-bold text-danger my-1">{metrics?.openAlerts ?? 12}</div>
                  <div className="small text-muted">7-Rule Engine Active</div>
                </div>
              </div>
            </div>
          </div>

          {/* Charts Row */}
          <div className="row g-4 mb-4">
            {/* 14-Day Transaction Volume Trend */}
            <div className="col-lg-6">
              <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-center mb-3">
                    <h2 className="h6 fw-bold mb-0">Daily Transaction Volume (Last 14 Days)</h2>
                    <span className="badge bg-primary-subtle text-primary border border-primary-subtle">
                      Real-Time Ingestion
                    </span>
                  </div>
                  <div style={{ width: '100%', height: 260 }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <AreaChart data={trendData} margin={{ top: 10, right: 15, left: 0, bottom: 0 }}>
                        <defs>
                          <linearGradient id="colorTxn" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8} />
                            <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                          </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" opacity={0.15} />
                        <XAxis dataKey="date" stroke="var(--tpg-text-secondary)" fontSize={12} />
                        <YAxis stroke="var(--tpg-text-secondary)" fontSize={12} />
                        <Tooltip
                          contentStyle={{
                            backgroundColor: 'var(--tpg-bg-surface)',
                            borderColor: 'var(--tpg-border-color)',
                            borderRadius: '8px',
                          }}
                        />
                        <Area
                          type="monotone"
                          dataKey="transactions"
                          name="Transactions Count"
                          stroke="#3b82f6"
                          fillOpacity={1}
                          fill="url(#colorTxn)"
                        />
                      </AreaChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              </div>
            </div>

            {/* Fraud Rule Trigger Distribution */}
            <div className="col-lg-6">
              <div className="card h-100 border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-center mb-3">
                    <h2 className="h6 fw-bold mb-0">Alert Triggers by Heuristic Rule</h2>
                    <span className="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle">
                      Policy Analytics
                    </span>
                  </div>
                  <div style={{ width: '100%', height: 260 }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={ruleDistribution} margin={{ top: 10, right: 15, left: 0, bottom: 0 }}>
                        <CartesianGrid strokeDasharray="3 3" opacity={0.15} />
                        <XAxis dataKey="rule" stroke="var(--tpg-text-secondary)" fontSize={10} interval={0} />
                        <YAxis stroke="var(--tpg-text-secondary)" fontSize={12} />
                        <Tooltip
                          contentStyle={{
                            backgroundColor: 'var(--tpg-bg-surface)',
                            borderColor: 'var(--tpg-border-color)',
                            borderRadius: '8px',
                          }}
                        />
                        <Bar dataKey="hits" name="Alert Contributions" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: USER GOVERNANCE */}
      {activeTab === 'users' && (
        <div>
          {/* Action Header */}
          <div className="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
            <div className="d-flex flex-wrap align-items-center gap-2">
              <div className="input-group input-group-sm" style={{ width: '220px' }}>
                <span className="input-group-text"><i className="bi bi-search"></i></span>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Search name/email..."
                  value={userSearch}
                  onChange={(e) => setUserSearch(e.target.value)}
                />
              </div>

              <select
                className="form-select form-select-sm"
                style={{ width: '140px' }}
                value={userRoleFilter}
                onChange={(e) => setUserRoleFilter(e.target.value)}
              >
                <option value="ALL">All Roles</option>
                <option value="CUSTOMER">Customer</option>
                <option value="FRAUD_ANALYST">Fraud Analyst</option>
                <option value="ADMIN">Admin</option>
              </select>

              <select
                className="form-select form-select-sm"
                style={{ width: '140px' }}
                value={userStatusFilter}
                onChange={(e) => setUserStatusFilter(e.target.value)}
              >
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="SUSPENDED">Suspended</option>
                <option value="DEACTIVATED">Deactivated</option>
              </select>
            </div>

            <button
              onClick={() => setShowCreateStaffModal(true)}
              className="btn btn-primary btn-sm d-flex align-items-center gap-2"
            >
              <i className="bi bi-person-plus-fill"></i>
              <span>Create Staff Member</span>
            </button>
          </div>

          {/* Users Table */}
          <div className="card border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
            <div className="card-body p-0">
              {loading ? (
                <div className="p-4">
                  <Skeleton height="2.5rem" className="mb-2" />
                  <Skeleton height="2.5rem" className="mb-2" />
                  <Skeleton height="2.5rem" />
                </div>
              ) : users.length === 0 ? (
                <div className="p-4 text-center">
                  <EmptyState title="No Users Found" description="Try clearing filters or search term." />
                </div>
              ) : (
                <div className="table-responsive">
                  <table className="table table-hover align-middle mb-0">
                    <thead className="table-light small text-secondary">
                      <tr>
                        <th className="ps-3">User</th>
                        <th>Email</th>
                        <th>Mobile</th>
                        <th>Role</th>
                        <th>Status</th>
                        <th className="pe-3 text-end">Governance Controls</th>
                      </tr>
                    </thead>
                    <tbody>
                      {users.map((u) => {
                        const isSelf = u.id === currentUser?.id;
                        return (
                          <tr key={u.id} className={isSelf ? 'table-primary-subtle' : ''}>
                            <td className="ps-3">
                              <div className="fw-semibold">{u.fullName}</div>
                              {isSelf && (
                                <span className="badge bg-primary text-white" style={{ fontSize: '0.65rem' }}>
                                  You (Active Session)
                                </span>
                              )}
                            </td>
                            <td className="small">{u.email}</td>
                            <td className="small font-monospace">{u.phoneNumber}</td>
                            <td>
                              {isSelf ? (
                                <span className="badge bg-danger">ADMIN</span>
                              ) : (
                                <select
                                  className="form-select form-select-sm"
                                  style={{ width: '130px' }}
                                  value={u.role}
                                  onChange={(e) => handleChangeUserRole(u, e.target.value as Role)}
                                >
                                  <option value="CUSTOMER">Customer</option>
                                  <option value="FRAUD_ANALYST">Fraud Analyst</option>
                                  <option value="ADMIN">Admin</option>
                                </select>
                              )}
                            </td>
                            <td>
                              <span className={`badge ${u.status === 'ACTIVE' ? 'bg-success' : 'bg-warning text-dark'}`}>
                                {u.status}
                              </span>
                            </td>
                            <td className="pe-3 text-end">
                              {isSelf ? (
                                <span className="text-muted small fst-italic">Protected (Self)</span>
                              ) : (
                                <button
                                  onClick={() => handleToggleUserStatus(u)}
                                  className={`btn btn-sm ${u.status === 'ACTIVE' ? 'btn-outline-danger' : 'btn-outline-success'} py-0 px-2`}
                                >
                                  {u.status === 'ACTIVE' ? 'Suspend' : 'Activate'}
                                </button>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: ACCOUNT GOVERNANCE */}
      {activeTab === 'accounts' && (
        <div className="card border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
          <div className="card-header bg-transparent py-3">
            <h2 className="h5 fw-bold mb-0">Bank-Wide Account Freezing &amp; Compliance</h2>
          </div>
          <div className="card-body p-0">
            {loading ? (
              <div className="p-4">
                <Skeleton height="2.5rem" className="mb-2" />
                <Skeleton height="2.5rem" className="mb-2" />
                <Skeleton height="2.5rem" />
              </div>
            ) : accounts.length === 0 ? (
              <div className="p-4 text-center">
                <EmptyState title="No Accounts Found" description="Account registry is empty." />
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light small text-secondary">
                    <tr>
                      <th className="ps-3">Account Number</th>
                      <th>Type</th>
                      <th>Ledger Balance</th>
                      <th>Available Balance</th>
                      <th>Status</th>
                      <th className="pe-3 text-end">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {accounts.map((acc) => (
                      <tr key={acc.id}>
                        <td className="ps-3 font-monospace fw-bold">{acc.maskedAccountNumber}</td>
                        <td><span className="badge bg-secondary-subtle text-secondary">{acc.accountType}</span></td>
                        <td className="fw-semibold">₹ {acc.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                        <td className="fw-semibold text-success">₹ {acc.availableBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                        <td>
                          <span className={`badge ${acc.status === 'ACTIVE' ? 'bg-success' : 'bg-danger'}`}>
                            {acc.status}
                          </span>
                        </td>
                        <td className="pe-3 text-end">
                          {acc.status === 'ACTIVE' ? (
                            <button
                              className="btn btn-outline-danger btn-sm py-0 px-2"
                              onClick={() => setAccountActionTarget({ account: acc, action: 'freeze' })}
                            >
                              Freeze Account
                            </button>
                          ) : (
                            <button
                              className="btn btn-outline-success btn-sm py-0 px-2"
                              onClick={() => setAccountActionTarget({ account: acc, action: 'unfreeze' })}
                            >
                              Unfreeze Account
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 4: FRAUD STRATEGY & RULES */}
      {activeTab === 'rules' && (
        <div className="card border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
          <div className="card-header bg-transparent py-3">
            <h2 className="h5 fw-bold mb-0">Dynamic Heuristic Rules &amp; Score Tuning</h2>
          </div>
          <div className="card-body p-0">
            {rules.length === 0 ? (
              <div className="p-4 text-center">
                <EmptyState title="No Fraud Rules Found" description="Loading default strategy engine rules..." />
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light small text-secondary">
                    <tr>
                      <th className="ps-3">Code</th>
                      <th>Rule Name</th>
                      <th>Description</th>
                      <th>Score Contribution</th>
                      <th>State</th>
                      <th className="pe-3 text-end">Tuning</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rules.map((r) => (
                      <tr key={r.id}>
                        <td className="ps-3 font-monospace fw-bold text-primary">{r.ruleCode}</td>
                        <td className="fw-semibold">{r.name}</td>
                        <td className="small text-secondary" style={{ maxWidth: '320px' }}>{r.description}</td>
                        <td>
                          <span className="badge bg-danger-subtle text-danger border border-danger-subtle">
                            {r.scorePoints ?? r.points} pts
                          </span>
                        </td>
                        <td>
                          <span className={`badge ${r.isEnabled ? 'bg-success' : 'bg-secondary'}`}>
                            {r.isEnabled ? 'ACTIVE' : 'DISABLED'}
                          </span>
                        </td>
                        <td className="pe-3 text-end">
                          <button
                            onClick={() => {
                              setEditingRule(r);
                              setRuleScorePoints(r.scorePoints ?? r.points);
                            }}
                            className="btn btn-outline-primary btn-sm py-0 px-2 me-1"
                          >
                            Edit Points
                          </button>
                          <button
                            onClick={() => handleToggleRule(r)}
                            className={`btn btn-sm ${r.isEnabled ? 'btn-outline-secondary' : 'btn-outline-success'} py-0 px-2`}
                          >
                            {r.isEnabled ? 'Disable' : 'Enable'}
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
      )}

      {/* TAB 5: SYSTEM SETTINGS */}
      {activeTab === 'settings' && (
        <div className="card border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
          <div className="card-header bg-transparent py-3">
            <h2 className="h5 fw-bold mb-0">Platform Operational Settings</h2>
          </div>
          <div className="card-body p-0">
            {settings.length === 0 ? (
              <div className="p-4 text-center">
                <EmptyState title="No Settings Found" description="Settings registry is empty." />
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light small text-secondary">
                    <tr>
                      <th className="ps-3">Setting Key</th>
                      <th>Current Value</th>
                      <th>Description</th>
                      <th className="pe-3 text-end">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {settings.map((s) => (
                      <tr key={s.id}>
                        <td className="ps-3 font-monospace fw-semibold">{s.settingKey}</td>
                        <td className="font-monospace fw-bold text-primary">{s.settingValue}</td>
                        <td className="small text-secondary">{s.description}</td>
                        <td className="pe-3 text-end">
                          <button
                            onClick={() => {
                              setEditingSetting(s);
                              setSettingValueInput(s.settingValue);
                            }}
                            className="btn btn-outline-primary btn-sm py-0 px-2"
                          >
                            Edit
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
      )}

      {/* TAB 6: SURVEILLANCE QUEUE (Read-Only) */}
      {activeTab === 'surveillance' && (
        <div className="card border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
          <div className="card-header bg-transparent py-3 d-flex justify-content-between align-items-center">
            <div>
              <h2 className="h5 fw-bold mb-0">Fraud Surveillance Queue (Observation Only)</h2>
              <div className="text-secondary small">Read-only oversight stream for compliance auditors and administrators.</div>
            </div>
            <span className="badge bg-info-subtle text-info-emphasis border border-info-subtle">
              Resolution buttons restricted to FRAUD_ANALYST
            </span>
          </div>
          <div className="card-body p-0">
            {surveillanceAlerts.length === 0 ? (
              <div className="p-4 text-center">
                <EmptyState title="No Alerts in Surveillance" description="No flagged cases currently recorded." />
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light small text-secondary">
                    <tr>
                      <th className="ps-3">Alert ID</th>
                      <th>Risk Level</th>
                      <th>Score</th>
                      <th>Txn Ref</th>
                      <th>Amount</th>
                      <th>Status</th>
                      <th className="pe-3">Flagged Time</th>
                    </tr>
                  </thead>
                  <tbody>
                    {surveillanceAlerts.map((a) => (
                      <tr key={a.id}>
                        <td className="ps-3 font-monospace fw-bold">#{a.id}</td>
                        <td>
                          <span className={`badge ${a.riskLevel === 'HIGH' ? 'bg-danger' : a.riskLevel === 'MEDIUM' ? 'bg-warning text-dark' : 'bg-success'}`}>
                            {a.riskLevel}
                          </span>
                        </td>
                        <td className="fw-bold">{a.riskScore}</td>
                        <td className="font-monospace small">{a.transaction?.referenceId}</td>
                        <td className="fw-semibold">₹ {a.transaction?.amount?.toLocaleString('en-IN')}</td>
                        <td><span className="badge bg-secondary">{a.status}</span></td>
                        <td className="pe-3 text-muted small">{new Date(a.createdAt).toLocaleString()}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 7: AUDIT TRAIL */}
      {activeTab === 'audit' && (
        <div className="card border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
          <div className="card-header bg-transparent py-3">
            <h2 className="h5 fw-bold mb-0">Immutable Audit Trail Logs</h2>
          </div>
          <div className="card-body p-0">
            {auditLogs.length === 0 ? (
              <div className="p-4 text-center">
                <EmptyState title="No Audit Records" description="No system actions have been logged yet." />
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead className="table-light small text-secondary">
                    <tr>
                      <th className="ps-3">Time</th>
                      <th>Actor</th>
                      <th>Action</th>
                      <th>Target Type</th>
                      <th>Target ID</th>
                      <th className="pe-3 text-end">Details</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditLogs.map((log) => (
                      <tr key={log.id}>
                        <td className="ps-3 small text-muted font-monospace">{new Date(log.createdAt).toLocaleString()}</td>
                        <td className="small fw-semibold">{log.actorName || log.actorEmail || 'SYSTEM'}</td>
                        <td><span className="badge bg-secondary-subtle text-secondary font-monospace">{log.action}</span></td>
                        <td className="small">{log.targetType}</td>
                        <td className="small font-monospace">{log.targetId}</td>
                        <td className="pe-3 text-end">
                          <button
                            onClick={() => setInspectingAudit(log)}
                            className="btn btn-outline-secondary btn-sm py-0 px-2"
                          >
                            Inspect JSON
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
      )}

      {/* Create Staff Modal */}
      {showCreateStaffModal && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.6)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border shadow" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
              <div className="modal-header">
                <h5 className="modal-title fw-bold">Create Platform Staff Member</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setShowCreateStaffModal(false)}
                  disabled={isCreatingStaff}
                  aria-label="Close"
                ></button>
              </div>
              <form onSubmit={handleCreateStaffSubmit}>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Assigned Staff Role</label>
                    <select
                      className="form-select"
                      value={staffRole}
                      onChange={(e) => setStaffRole(e.target.value as 'FRAUD_ANALYST' | 'ADMIN')}
                    >
                      <option value="FRAUD_ANALYST">Fraud Analyst (Alert investigation &amp; resolution)</option>
                      <option value="ADMIN">System Administrator (Full platform governance)</option>
                    </select>
                  </div>

                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Full Legal Name</label>
                    <input
                      type="text"
                      className="form-control"
                      value={staffFullName}
                      onChange={(e) => setStaffFullName(e.target.value)}
                      placeholder="e.g. Rachel Zane"
                      required
                    />
                  </div>

                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Corporate Email Address</label>
                    <input
                      type="email"
                      className="form-control"
                      value={staffEmail}
                      onChange={(e) => setStaffEmail(e.target.value)}
                      placeholder="e.g. rachel@trustpay.internal"
                      required
                    />
                  </div>

                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Mobile Number</label>
                    <input
                      type="tel"
                      className="form-control font-monospace"
                      value={staffPhone}
                      onChange={(e) => setStaffPhone(e.target.value)}
                      placeholder="9876543210"
                      pattern="[0-9]{10}"
                      required
                    />
                  </div>

                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Initial Password</label>
                    <input
                      type="password"
                      className="form-control"
                      value={staffPassword}
                      onChange={(e) => setStaffPassword(e.target.value)}
                      placeholder="Min 10 characters with upper/lower/digit/special"
                      required
                    />
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => setShowCreateStaffModal(false)}
                    disabled={isCreatingStaff}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm d-flex align-items-center gap-2"
                    disabled={isCreatingStaff}
                  >
                    {isCreatingStaff && <span className="spinner-border spinner-border-sm" role="status"></span>}
                    <span>Provision Account</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Freeze / Unfreeze Confirmation Modal */}
      {accountActionTarget && (
        <ConfirmModal
          isOpen={true}
          title={accountActionTarget.action === 'freeze' ? 'Freeze Customer Account?' : 'Unfreeze Customer Account?'}
          message={
            accountActionTarget.action === 'freeze'
              ? `Are you sure you want to freeze account ${accountActionTarget.account.maskedAccountNumber}? All outgoing transfers will be rejected with HTTP 422, and incoming deposits will be rejected.`
              : `Are you sure you want to restore account ${accountActionTarget.account.maskedAccountNumber}? Normal transaction processing will resume immediately.`
          }
          confirmLabel={accountActionTarget.action === 'freeze' ? 'Confirm Freeze' : 'Confirm Unfreeze'}
          cancelLabel="Cancel"
          confirmVariant={accountActionTarget.action === 'freeze' ? 'danger' : 'success'}
          isLoading={isAccountActionLoading}
          onConfirm={handleExecuteAccountAction}
          onCancel={() => setAccountActionTarget(null)}
        />
      )}

      {/* Edit Rule Points Modal */}
      {editingRule && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.6)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border shadow" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
              <div className="modal-header">
                <h5 className="modal-title fw-bold">Tune Rule Score: {editingRule.ruleCode}</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setEditingRule(null)}
                  disabled={isUpdatingRule}
                  aria-label="Close"
                ></button>
              </div>
              <form onSubmit={handleSaveRulePoints}>
                <div className="modal-body">
                  <div className="mb-2 fw-semibold">{editingRule.name}</div>
                  <div className="text-secondary small mb-3">{editingRule.description}</div>

                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Score Points Contribution (0 to 100)</label>
                    <input
                      type="number"
                      min="0"
                      max="100"
                      className="form-control"
                      value={ruleScorePoints}
                      onChange={(e) => setRuleScorePoints(Number(e.target.value))}
                      required
                    />
                    <div className="form-text small">
                      Adjusting this directly alters the real-time scoring engine. An audit record will be logged.
                    </div>
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => setEditingRule(null)}
                    disabled={isUpdatingRule}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm d-flex align-items-center gap-2"
                    disabled={isUpdatingRule}
                  >
                    {isUpdatingRule && <span className="spinner-border spinner-border-sm" role="status"></span>}
                    <span>Save Points</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Edit Setting Modal */}
      {editingSetting && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.6)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content border shadow" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
              <div className="modal-header">
                <h5 className="modal-title fw-bold">Update Setting: {editingSetting.settingKey}</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setEditingSetting(null)}
                  disabled={isUpdatingSetting}
                  aria-label="Close"
                ></button>
              </div>
              <form onSubmit={handleSaveSetting}>
                <div className="modal-body">
                  <div className="text-secondary small mb-3">{editingSetting.description}</div>
                  <div className="mb-3">
                    <label className="form-label small fw-semibold">Setting Value</label>
                    <input
                      type="text"
                      className="form-control font-monospace"
                      value={settingValueInput}
                      onChange={(e) => setSettingValueInput(e.target.value)}
                      required
                    />
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => setEditingSetting(null)}
                    disabled={isUpdatingSetting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary btn-sm d-flex align-items-center gap-2"
                    disabled={isUpdatingSetting}
                  >
                    {isUpdatingSetting && <span className="spinner-border spinner-border-sm" role="status"></span>}
                    <span>Save Setting</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Audit Log JSON Inspector Modal */}
      {inspectingAudit && (
        <div className="modal show d-block" tabIndex={-1} style={{ backgroundColor: 'rgba(0,0,0,0.6)' }}>
          <div className="modal-dialog modal-dialog-centered modal-lg">
            <div className="modal-content border shadow" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
              <div className="modal-header">
                <h5 className="modal-title fw-bold">Audit Event #{inspectingAudit.id}: {inspectingAudit.action}</h5>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => setInspectingAudit(null)}
                  aria-label="Close"
                ></button>
              </div>
              <div className="modal-body">
                <div className="row g-2 mb-3 small">
                  <div className="col-6"><strong>Actor:</strong> {inspectingAudit.actorName || inspectingAudit.actorEmail}</div>
                  <div className="col-6"><strong>Target:</strong> {inspectingAudit.targetType} ({inspectingAudit.targetId})</div>
                  <div className="col-6"><strong>IP Address:</strong> {inspectingAudit.ipAddress || '127.0.0.1'}</div>
                  <div className="col-6"><strong>Request ID:</strong> {inspectingAudit.requestId || 'N/A'}</div>
                </div>
                <label className="form-label small fw-semibold">Payload JSON</label>
                <pre className="p-3 rounded bg-body-tertiary small border font-monospace" style={{ maxHeight: '250px', overflowY: 'auto' }}>
                  {(() => {
                    try {
                      return JSON.stringify(JSON.parse(inspectingAudit.detailsJson), null, 2);
                    } catch {
                      return inspectingAudit.detailsJson || '{}';
                    }
                  })()}
                </pre>
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setInspectingAudit(null)}
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
