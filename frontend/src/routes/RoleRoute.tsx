import React, { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Skeleton } from '../components/common/Skeleton';

interface RoleRouteProps {
  allowedRoles: string[];
  children?: ReactNode;
}

export const RoleRoute: React.FC<RoleRouteProps> = ({ allowedRoles, children }) => {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="container py-5 text-center">
        <div className="mx-auto" style={{ maxWidth: '400px' }}>
          <Skeleton height="2rem" className="mb-3" />
          <Skeleton height="1.25rem" width="80%" className="mb-2" />
          <Skeleton height="1.25rem" width="60%" />
        </div>
      </div>
    );
  }

  if (!user || !allowedRoles.includes(user.role)) {
    return <Navigate to="/403" replace />;
  }

  return children ? <>{children}</> : null;
};
