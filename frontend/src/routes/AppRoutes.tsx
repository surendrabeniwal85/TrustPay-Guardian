import React, { Suspense, lazy } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { RoleRoute } from './RoleRoute';
import { AppShell } from '../components/layout/AppShell';
import { Skeleton } from '../components/common/Skeleton';
import { useAuth } from '../context/AuthContext';

// Lazy-loaded route components
const LandingPage = lazy(() => import('../pages/LandingPage').then((m) => ({ default: m.LandingPage })));
const LoginPage = lazy(() => import('../pages/auth/LoginPage').then((m) => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import('../pages/auth/RegisterPage').then((m) => ({ default: m.RegisterPage })));
const CustomerDashboard = lazy(() => import('../pages/customer/CustomerDashboardPage').then((m) => ({ default: m.CustomerDashboardPage })));
const AccountsPage = lazy(() => import('../pages/customer/AccountsPage').then((m) => ({ default: m.AccountsPage })));
const TransferPage = lazy(() => import('../pages/customer/TransferPage').then((m) => ({ default: m.TransferPage })));
const BeneficiariesPage = lazy(() => import('../pages/customer/BeneficiariesPage').then((m) => ({ default: m.BeneficiariesPage })));
const TransactionsPage = lazy(() => import('../pages/customer/TransactionsPage').then((m) => ({ default: m.TransactionsPage })));
const NotificationsPage = lazy(() => import('../pages/customer/NotificationsPage').then((m) => ({ default: m.NotificationsPage })));
const ProfileSettingsPage = lazy(() => import('../pages/customer/ProfileSettingsPage').then((m) => ({ default: m.ProfileSettingsPage })));
const AnalystDashboard = lazy(() => import('../pages/AnalystDashboard').then((m) => ({ default: m.AnalystDashboard })));
const AdminDashboard = lazy(() => import('../pages/AdminDashboard').then((m) => ({ default: m.AdminDashboard })));
const ForbiddenPage = lazy(() => import('../pages/errors/ForbiddenPage').then((m) => ({ default: m.ForbiddenPage })));
const NotFoundPage = lazy(() => import('../pages/errors/NotFoundPage').then((m) => ({ default: m.NotFoundPage })));

const SuspenseFallback: React.FC = () => (
  <div className="container py-5 text-center">
    <div className="mx-auto" style={{ maxWidth: '400px' }}>
      <Skeleton height="2.5rem" className="mb-3" />
      <Skeleton height="1.25rem" width="80%" className="mb-2" />
      <Skeleton height="1.25rem" width="60%" />
    </div>
  </div>
);

// Helper component to redirect authenticated users to their specific dashboard
const DashboardRedirect: React.FC = () => {
  const { user, isAuthenticated, isLoading } = useAuth();

  if (isLoading) return <SuspenseFallback />;
  if (!isAuthenticated || !user) return <Navigate to="/login" replace />;

  if (user.role === 'ADMIN') return <Navigate to="/admin/dashboard" replace />;
  if (user.role === 'FRAUD_ANALYST') return <Navigate to="/analyst/dashboard" replace />;
  return <Navigate to="/app/dashboard" replace />;
};

export const AppRoutes: React.FC = () => {
  return (
    <Suspense fallback={<SuspenseFallback />}>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/dashboard" element={<DashboardRedirect />} />
        <Route path="/403" element={<ForbiddenPage />} />
        <Route path="/404" element={<NotFoundPage />} />

        {/* Customer Route Group under /app */}
        <Route
          path="/app"
          element={
            <ProtectedRoute>
              <RoleRoute allowedRoles={['CUSTOMER', 'FRAUD_ANALYST', 'ADMIN']}>
                <AppShell />
              </RoleRoute>
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/app/dashboard" replace />} />
          <Route path="dashboard" element={<CustomerDashboard />} />
          <Route path="accounts" element={<AccountsPage />} />
          <Route path="transfer" element={<TransferPage />} />
          <Route path="beneficiaries" element={<BeneficiariesPage />} />
          <Route path="history" element={<TransactionsPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="profile" element={<ProfileSettingsPage />} />
        </Route>

        {/* Analyst Route Group under /analyst */}
        <Route
          path="/analyst"
          element={
            <ProtectedRoute>
              <RoleRoute allowedRoles={['FRAUD_ANALYST', 'ADMIN']}>
                <AppShell />
              </RoleRoute>
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/analyst/dashboard" replace />} />
          <Route path="dashboard" element={<AnalystDashboard />} />
          <Route path="alerts" element={<AnalystDashboard />} />
        </Route>

        {/* Administrator Route Group under /admin */}
        <Route
          path="/admin"
          element={
            <ProtectedRoute>
              <RoleRoute allowedRoles={['ADMIN']}>
                <AppShell />
              </RoleRoute>
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/admin/dashboard" replace />} />
          <Route path="dashboard" element={<AdminDashboard />} />
          <Route path="users" element={<AdminDashboard />} />
          <Route path="fraud-rules" element={<AdminDashboard />} />
          <Route path="settings" element={<AdminDashboard />} />
          <Route path="audit-logs" element={<AdminDashboard />} />
        </Route>

        {/* Catch-all 404 */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
};
