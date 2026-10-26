import React, { useEffect, useCallback } from 'react';
import { useAppDispatch, useAppSelector } from '../../store';
import {
  fetchNotifications,
  markAsRead,
  markAllAsRead,
} from '../../store/slices/notificationSlice';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/common/EmptyState';

export const NotificationsPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { notifications, unreadCount, isLoading, error } = useAppSelector(
    (state) => state.notifications
  );

  const loadNotifications = useCallback(() => {
    dispatch(fetchNotifications());
  }, [dispatch]);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  const handleMarkAsRead = (id: number) => {
    dispatch(markAsRead(id));
  };

  const handleMarkAllRead = () => {
    dispatch(markAllAsRead());
  };

  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'TRANSFER_RECEIVED':
      case 'TRANSFER_RELEASED':
        return <i className="bi bi-arrow-down-left-circle-fill text-success fs-4"></i>;
      case 'TRANSFER_SENT':
        return <i className="bi bi-arrow-up-right-circle-fill text-primary fs-4"></i>;
      case 'TRANSFER_HELD':
        return <i className="bi bi-shield-exclamation text-warning fs-4"></i>;
      case 'TRANSFER_BLOCKED':
      case 'ACCOUNT_FROZEN':
        return <i className="bi bi-shield-x text-danger fs-4"></i>;
      case 'ACCOUNT_UNFROZEN':
      case 'ACCOUNT_CREATED':
        return <i className="bi bi-check-circle-fill text-success fs-4"></i>;
      case 'SECURITY_ALERT':
      default:
        return <i className="bi bi-bell-fill text-info fs-4"></i>;
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: '840px' }}>
      {/* Header */}
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
          <h1 className="h3 fw-bold mb-1">Security &amp; Account Notifications</h1>
          <p className="text-secondary small mb-0">
            Audit feed of balance changes, fraud risk decisions, and system alerts.
          </p>
        </div>
        {unreadCount > 0 && (
          <div>
            <button
              onClick={handleMarkAllRead}
              className="btn btn-outline-primary btn-sm d-flex align-items-center gap-2"
            >
              <i className="bi bi-check2-all"></i>
              <span>Mark All as Read ({unreadCount})</span>
            </button>
          </div>
        )}
      </div>

      {/* Notifications Card */}
      <div className="card border shadow-sm" style={{ backgroundColor: 'var(--tpg-bg-surface)' }}>
        <div className="card-body p-0">
          {isLoading && notifications.length === 0 ? (
            <div className="p-4">
              <Skeleton height="3.5rem" className="mb-2" />
              <Skeleton height="3.5rem" className="mb-2" />
              <Skeleton height="3.5rem" />
            </div>
          ) : error ? (
            <div className="p-4">
              <EmptyState
                icon="bi-exclamation-triangle"
                title="Could Not Load Notifications"
                description={error}
                action={
                  <button onClick={loadNotifications} className="btn btn-outline-primary btn-sm">
                    Try Again
                  </button>
                }
              />
            </div>
          ) : notifications.length === 0 ? (
            <div className="p-5 text-center">
              <EmptyState
                icon="bi-bell-slash"
                title="No Notifications"
                description="You are completely caught up! New transaction status updates and security alerts will appear here."
              />
            </div>
          ) : (
            <div className="list-group list-group-flush">
              {notifications.map((n) => {
                const isUnread = !n.readAt;
                return (
                  <div
                    key={n.id}
                    className={`list-group-item list-group-item-action p-3 d-flex align-items-start gap-3 transition-all ${
                      isUnread ? 'bg-primary-subtle' : ''
                    }`}
                  >
                    <div className="pt-1">{getNotificationIcon(n.type)}</div>
                    <div className="flex-grow-1">
                      <div className="d-flex justify-content-between align-items-center mb-1">
                        <strong className="h6 mb-0">{n.title}</strong>
                        <div className="d-flex align-items-center gap-2">
                          <span className="text-muted small">
                            {new Date(n.createdAt).toLocaleDateString([], {
                              month: 'short',
                              day: 'numeric',
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </span>
                          {isUnread && (
                            <button
                              onClick={() => handleMarkAsRead(n.id)}
                              className="btn btn-link btn-sm p-0 text-decoration-none text-muted"
                              title="Mark as read"
                            >
                              <i className="bi bi-check2"></i>
                            </button>
                          )}
                        </div>
                      </div>
                      <p className="text-secondary small mb-0">{n.message}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
