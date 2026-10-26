import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store';
import {
  fetchUnreadCount,
  fetchNotifications,
  markAsRead,
  markAllAsRead,
} from '../store/slices/notificationSlice';

export const NotificationDropdown: React.FC = () => {
  const dispatch = useAppDispatch();
  const { unreadCount, notifications, isLoading } = useAppSelector(
    (state) => state.notifications
  );
  const [isOpen, setIsOpen] = useState<boolean>(false);

  useEffect(() => {
    dispatch(fetchUnreadCount());
    // PRD requirement: Polling every 30 seconds
    const interval = setInterval(() => {
      dispatch(fetchUnreadCount());
    }, 30000);
    return () => clearInterval(interval);
  }, [dispatch]);

  const handleToggle = () => {
    if (!isOpen) {
      dispatch(fetchNotifications());
    }
    setIsOpen(!isOpen);
  };

  const handleMarkAsRead = (id: number, e: React.MouseEvent) => {
    e.stopPropagation();
    dispatch(markAsRead(id));
  };

  const handleMarkAllRead = (e: React.MouseEvent) => {
    e.stopPropagation();
    dispatch(markAllAsRead());
  };

  return (
    <div className="dropdown position-relative">
      <button
        onClick={handleToggle}
        className="btn btn-outline-secondary btn-sm position-relative rounded-circle p-2"
        style={{ width: '38px', height: '38px' }}
        title="Notifications"
        aria-label="Notifications"
        aria-expanded={isOpen}
      >
        <i className="bi bi-bell-fill"></i>
        {unreadCount > 0 && (
          <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div
          className="dropdown-menu dropdown-menu-end show shadow-lg p-0 mt-2"
          style={{ width: '350px', maxHeight: '440px', overflowY: 'auto', zIndex: 1050 }}
        >
          <div className="p-3 border-bottom d-flex justify-content-between align-items-center bg-body-tertiary">
            <div className="d-flex align-items-center gap-2">
              <span className="fw-semibold">Notifications</span>
              {unreadCount > 0 && (
                <span className="badge bg-danger">{unreadCount} new</span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                className="btn btn-link btn-sm p-0 text-decoration-none small"
              >
                Mark all read
              </button>
            )}
          </div>

          <div className="list-group list-group-flush">
            {isLoading && notifications.length === 0 ? (
              <div className="p-4 text-center text-secondary small">
                <div className="spinner-border spinner-border-sm text-primary mb-2" role="status"></div>
                <div>Checking notifications...</div>
              </div>
            ) : notifications.length === 0 ? (
              <div className="p-4 text-center text-secondary small">
                <i className="bi bi-bell-slash fs-4 d-block mb-1 text-muted"></i>
                No notifications right now.
              </div>
            ) : (
              notifications.slice(0, 5).map((item) => (
                <div
                  key={item.id}
                  className={`list-group-item list-group-item-action p-3 ${
                    !item.readAt ? 'bg-primary-subtle' : ''
                  }`}
                >
                  <div className="d-flex justify-content-between align-items-start mb-1">
                    <strong className="small text-truncate me-2" style={{ maxWidth: '240px' }}>
                      {item.title}
                    </strong>
                    {!item.readAt && (
                      <button
                        onClick={(e) => handleMarkAsRead(item.id, e)}
                        className="btn btn-link btn-sm p-0 text-decoration-none text-muted"
                        title="Mark as read"
                      >
                        <i className="bi bi-check2"></i>
                      </button>
                    )}
                  </div>
                  <p className="mb-1 small text-secondary">{item.message}</p>
                  <div className="text-muted" style={{ fontSize: '0.75rem' }}>
                    {new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="p-2 border-top text-center bg-body-tertiary">
            <Link
              to="/app/notifications"
              className="text-decoration-none small fw-semibold text-primary"
              onClick={() => setIsOpen(false)}
            >
              View all notifications <i className="bi bi-arrow-right"></i>
            </Link>
          </div>
        </div>
      )}
    </div>
  );
};
