import React, { useState, useRef, useEffect } from 'react';
import { MoreHorizontal } from 'lucide-react';
import { cn } from '../../../core/utils/cn';

export interface RoomActionItem {
  key: string;
  label: string;
  icon?: React.ReactNode;
  onClick: () => void;
  disabled?: boolean;
  danger?: boolean;
  warning?: boolean;
  isDivider?: boolean;
  title?: string;
}

export interface RoomActionDropdownProps {
  items: RoomActionItem[];
  label?: string;
  align?: 'left' | 'right';
  className?: string;
}

export const RoomActionDropdown: React.FC<RoomActionDropdownProps> = ({
  items,
  label,
  align = 'right',
  className,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleEscape);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen]);

  const activeItems = items.filter((item) => !item.disabled);
  if (activeItems.length === 0) {
    return null;
  }

  return (
    <div className={cn('relative inline-block text-left', className)} ref={containerRef}>
      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        className={cn(
          'inline-flex items-center justify-center gap-1.5 px-2.5 py-1.5 text-xs font-semibold rounded-lg border transition-all cursor-pointer select-none',
          isOpen
            ? 'bg-neutral-100 text-neutral-900 border-neutral-300 shadow-xs ring-2 ring-neutral-300'
            : 'bg-white text-neutral-700 border-neutral-200 hover:bg-neutral-50 hover:text-neutral-900 hover:border-neutral-300 shadow-xs'
        )}
        title="Thao tác khác"
        aria-expanded={isOpen}
      >
        <MoreHorizontal className="w-4 h-4 text-neutral-500" />
        {label && <span>{label}</span>}
      </button>

      {isOpen && (
        <div
          className={cn(
            'absolute top-full mt-1.5 z-40 min-w-[200px] bg-white rounded-xl shadow-xl border border-neutral-200/90 py-1.5 focus:outline-none animate-in fade-in zoom-in-95 duration-150',
            align === 'right' ? 'right-0' : 'left-0'
          )}
        >
          {items.map((item, idx) => {
            if (item.disabled) return null;

            if (item.isDivider) {
              return <div key={`divider-${idx}`} className="my-1 border-t border-neutral-100" />;
            }

            return (
              <button
                key={item.key}
                type="button"
                onClick={() => {
                  setIsOpen(false);
                  item.onClick();
                }}
                title={item.title}
                className={cn(
                  'w-full flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-left transition-colors cursor-pointer',
                  item.danger
                    ? 'text-red-600 hover:bg-red-50 hover:text-red-700'
                    : item.warning
                    ? 'text-amber-700 hover:bg-amber-50 hover:text-amber-800'
                    : 'text-neutral-700 hover:bg-neutral-50 hover:text-neutral-900'
                )}
              >
                {item.icon && (
                  <span
                    className={cn(
                      'shrink-0',
                      item.danger
                        ? 'text-red-500'
                        : item.warning
                        ? 'text-amber-500'
                        : 'text-neutral-400'
                    )}
                  >
                    {item.icon}
                  </span>
                )}
                <span className="flex-1 truncate">{item.label}</span>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default RoomActionDropdown;

