import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { NotificationItem, ApiResponse } from '../types';

export const NotificationDropdown: React.FC = () => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [isOpen, setIsOpen] = useState<boolean>(false);

  const fetchUnreadCount = async () => {
    try {
      const res = await api.get<ApiResponse<number>>('/notifications/unread-count');
      setUnreadCount(res.data.data || 0);
    } catch {
      // Ignore background notification fetch errors
    }
  };

  const fetchNotifications = async () => {
    try {
      const res = await api.get<ApiResponse<NotificationItem[]>>('/notifications');
      setNotifications(res.data.data || []);
    } catch {
      // Ignore
    }
  };

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 15000);
    return () => clearInterval(interval);
  }, []);

  const handleToggle = () => {
    if (!isOpen) {
      fetchNotifications();
    }
    setIsOpen(!isOpen);
  };

  const markAsRead = async (id: number) => {
    try {
      await api.patch(`/notifications/${id}/read`);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, readAt: new Date().toISOString() } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {
      // Ignore
    }
  };

  return (
    <div className="dropdown position-relative">
      <button
        onClick={handleToggle}
        className="btn btn-outline-secondary btn-sm position-relative rounded-circle p-2"
        style={{ width: '38px', height: '38px' }}
        title="Notifications"
        aria-label="Notifications"
      >
        <i className="bi bi-bell-fill"></i>
        {unreadCount > 0 && (
          <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">
            {unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div
          className="dropdown-menu dropdown-menu-end show shadow-lg p-0 mt-2"
          style={{ width: '340px', maxHeight: '420px', overflowY: 'auto', zIndex: 1050 }}
        >
          <div className="p-3 border-bottom d-flex justify-content-between align-items-center bg-body-tertiary">
            <span className="fw-semibold">Notifications</span>
            {unreadCount > 0 && (
              <span className="badge bg-danger">{unreadCount} unread</span>
            )}
          </div>

          <div className="list-group list-group-flush">
            {notifications.length === 0 ? (
              <div className="p-4 text-center text-secondary small">
                <i className="bi bi-bell-slash fs-4 d-block mb-1"></i>
                No notifications yet.
              </div>
            ) : (
              notifications.map((item) => (
                <div
                  key={item.id}
                  className={`list-group-item list-group-item-action p-3 ${
                    !item.readAt ? 'bg-primary-subtle' : ''
                  }`}
                >
                  <div className="d-flex justify-content-between align-items-start mb-1">
                    <strong className="small">{item.title}</strong>
                    {!item.readAt && (
                      <button
                        onClick={() => markAsRead(item.id)}
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
        </div>
      )}
    </div>
  );
};
