import React, { ReactNode } from 'react';

interface EmptyStateProps {
  icon?: string;
  title: string;
  description: string;
  action?: ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon = 'bi-inbox',
  title,
  description,
  action,
}) => {
  return (
    <div className="text-center py-5 px-3 border rounded-3 bg-body-tertiary my-3" style={{ borderColor: 'var(--tpg-border-color)' }}>
      <i className={`bi ${icon} display-4 text-secondary mb-3 d-block`}></i>
      <h3 className="h5 fw-semibold mb-2">{title}</h3>
      <p className="text-muted mx-auto mb-3" style={{ maxWidth: '400px' }}>
        {description}
      </p>
      {action && <div>{action}</div>}
    </div>
  );
};
