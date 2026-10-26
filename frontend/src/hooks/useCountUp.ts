import { useState, useEffect } from 'react';

interface UseCountUpOptions {
  end: number;
  duration?: number; // milliseconds
  start?: number;
  active?: boolean;
  decimals?: number;
}

export const useCountUp = ({
  end,
  duration = 1500,
  start = 0,
  active = true,
  decimals = 0,
}: UseCountUpOptions): number => {
  const [count, setCount] = useState<number>(start);

  useEffect(() => {
    if (!active) {
      setCount(start);
      return;
    }

    // Check prefers-reduced-motion
    const prefersReducedMotion =
      typeof window !== 'undefined' &&
      window.matchMedia &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (prefersReducedMotion) {
      setCount(end);
      return;
    }

    let startTime: number | null = null;
    let animationFrameId: number;

    const step = (timestamp: number) => {
      if (!startTime) startTime = timestamp;
      const progress = Math.min((timestamp - startTime) / duration, 1);

      // Ease-out cubic calculation
      const easeOut = 1 - Math.pow(1 - progress, 3);
      const currentVal = start + (end - start) * easeOut;

      const factor = Math.pow(10, decimals);
      setCount(Math.round(currentVal * factor) / factor);

      if (progress < 1) {
        animationFrameId = requestAnimationFrame(step);
      } else {
        setCount(end);
      }
    };

    animationFrameId = requestAnimationFrame(step);

    return () => cancelAnimationFrame(animationFrameId);
  }, [end, duration, start, active, decimals]);

  return count;
};
