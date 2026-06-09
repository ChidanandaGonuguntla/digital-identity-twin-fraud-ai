import type { ReactNode } from 'react';

export function ChartFrame({ height, children }: { height: number; children: ReactNode }) {
  return (
    <div style={{ width: '100%', minWidth: 0, height, minHeight: height, position: 'relative' }}>
      {children}
    </div>
  );
}
