import React from 'react';

interface SkeletonProps {
  height?: string | number;
  width?: string | number;
  className?: string;
  borderRadius?: string;
}

export const Skeleton: React.FC<SkeletonProps> = ({
  height = '1.25rem',
  width = '100%',
  className = '',
  borderRadius = 'var(--tpg-radius-sm)',
}) => {
  return (
    <div
      className={`placeholder-glow ${className}`}
      style={{
        display: 'inline-block',
        height,
        width,
        borderRadius,
        backgroundColor: 'var(--tpg-bg-subtle)',
      }}
    >
      <span
        className="placeholder col-12 h-100 rounded"
        style={{
          display: 'block',
          backgroundColor: 'var(--tpg-border-color)',
          opacity: 0.5,
        }}
      ></span>
    </div>
  );
};
