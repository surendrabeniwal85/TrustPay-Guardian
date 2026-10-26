import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useCountUp } from '../hooks/useCountUp';

describe('useCountUp hook', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('initializes at start value when active is false', () => {
    const { result } = renderHook(() =>
      useCountUp({ end: 100, start: 0, active: false })
    );
    expect(result.current).toBe(0);
  });

  it('animates towards end value when active is true', () => {
    const { result } = renderHook(() =>
      useCountUp({ end: 100, start: 0, active: true, duration: 500 })
    );

    act(() => {
      vi.advanceTimersByTime(250);
    });

    // Should have progressed
    expect(result.current).toBeGreaterThan(0);

    act(() => {
      vi.advanceTimersByTime(500);
    });

    // Should reach end value
    expect(result.current).toBe(100);
  });
});
