import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { api } from '../../api/axiosClient';
import { NotificationItem, ApiResponse, Page } from '../../types';

interface NotificationState {
  unreadCount: number;
  notifications: NotificationItem[];
  isLoading: boolean;
  error: string | null;
}

const initialState: NotificationState = {
  unreadCount: 0,
  notifications: [],
  isLoading: false,
  error: null,
};

export const fetchUnreadCount = createAsyncThunk<number>(
  'notifications/fetchUnreadCount',
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.get<ApiResponse<number>>('/notifications/unread-count');
      return response.data?.data ?? 0;
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to fetch unread count';
      return rejectWithValue(message);
    }
  }
);

export const fetchNotifications = createAsyncThunk<NotificationItem[]>(
  'notifications/fetchNotifications',
  async (_, { rejectWithValue }) => {
    try {
      const response = await api.get<ApiResponse<Page<NotificationItem> | NotificationItem[]>>('/notifications');
      const data = response.data?.data;
      if (Array.isArray(data)) {
        return data;
      }
      if (data && 'content' in data) {
        return data.content;
      }
      return [];
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to fetch notifications';
      return rejectWithValue(message);
    }
  }
);

export const markAsRead = createAsyncThunk<number, number>(
  'notifications/markAsRead',
  async (id, { rejectWithValue }) => {
    try {
      await api.patch(`/notifications/${id}/read`);
      return id;
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to mark notification as read';
      return rejectWithValue(message);
    }
  }
);

export const markAllAsRead = createAsyncThunk<void>(
  'notifications/markAllAsRead',
  async (_, { rejectWithValue }) => {
    try {
      await api.patch('/notifications/read-all');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to mark all notifications as read';
      return rejectWithValue(message);
    }
  }
);

export const notificationSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    setUnreadCount: (state, action: PayloadAction<number>) => {
      state.unreadCount = action.payload;
    },
    resetNotifications: (state) => {
      state.notifications = [];
      state.unreadCount = 0;
      state.isLoading = false;
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // fetchUnreadCount
      .addCase(fetchUnreadCount.fulfilled, (state, action) => {
        state.unreadCount = action.payload;
      })
      // fetchNotifications
      .addCase(fetchNotifications.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchNotifications.fulfilled, (state, action) => {
        state.isLoading = false;
        state.notifications = action.payload;
      })
      .addCase(fetchNotifications.rejected, (state, action) => {
        state.isLoading = false;
        state.error = (action.payload as string) || 'Failed to fetch notifications';
      })
      // markAsRead
      .addCase(markAsRead.fulfilled, (state, action) => {
        const id = action.payload;
        const item = state.notifications.find((n) => n.id === id);
        if (item && !item.readAt) {
          item.readAt = new Date().toISOString();
          state.unreadCount = Math.max(0, state.unreadCount - 1);
        }
      })
      // markAllAsRead
      .addCase(markAllAsRead.fulfilled, (state) => {
        const now = new Date().toISOString();
        state.notifications.forEach((n) => {
          if (!n.readAt) {
            n.readAt = now;
          }
        });
        state.unreadCount = 0;
      });
  },
});

export const { setUnreadCount, resetNotifications } = notificationSlice.actions;
export default notificationSlice.reducer;
