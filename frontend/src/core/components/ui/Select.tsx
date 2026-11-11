import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown, Check, Loader2 } from 'lucide-react';
import { cn } from '../../utils/cn';

export interface SelectOption<T = string | number> {
  value: T;
  label: string;
  icon?: React.ReactNode;
  badge?: string;
  description?: string;
  disabled?: boolean;
}

export interface SelectProps<T = string | number> {
  value: T;
  onChange: (value: T) => void;
  options: SelectOption<T>[];
  placeholder?: string;
  label?: string;
  error?: string;
  disabled?: boolean;
  prefixIcon?: React.ReactNode;
  className?: string;
  size?: 'sm' | 'md' | 'lg';
  fullWidth?: boolean;
  isLoading?: boolean;
  loadingText?: string;
  emptyText?: string;
  onOpen?: () => void | Promise<void>;
  direction?: 'down' | 'up' | 'auto';
}

export const Select = <T extends string | number>({
  value,
  onChange,
  options,
  placeholder = 'Chọn một tùy chọn...',
  label,
  error,
  disabled = false,
  prefixIcon,
  className,
  size = 'md',
  fullWidth = true,
  isLoading = false,
  loadingText = 'Đang tải danh sách...',
  emptyText = 'Không có tùy chọn nào',
  onOpen,
  direction = 'auto',
}: SelectProps<T>) => {
  const [isOpen, setIsOpen] = useState(false);
  const [openUpward, setOpenUpward] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const selectedOption = options.find((opt) => opt.value === value);

  // Close when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  const sizeClasses = {
    sm: 'h-9 px-3 text-xs',
    md: 'h-11 px-3.5 text-xs font-semibold',
    lg: 'h-12 px-4 text-sm font-semibold',
  };

  const calculateDirection = () => {
    if (direction === 'up') return true;
    if (direction === 'down') return false;

    if (!containerRef.current) return false;
    const rect = containerRef.current.getBoundingClientRect();
    const spaceBelowWindow = window.innerHeight - rect.bottom;
    const spaceAboveWindow = rect.top;

    let scrollParent = containerRef.current.parentElement;
    let spaceBelowParent = spaceBelowWindow;
    let spaceAboveParent = spaceAboveWindow;

    while (scrollParent && scrollParent !== document.body) {
      const style = window.getComputedStyle(scrollParent);
      if (
        style.overflowY === 'auto' ||
        style.overflowY === 'scroll' ||
        style.overflowY === 'hidden'
      ) {
        const parentRect = scrollParent.getBoundingClientRect();
        spaceBelowParent = parentRect.bottom - rect.bottom;
        spaceAboveParent = rect.top - parentRect.top;
        break;
      }
      scrollParent = scrollParent.parentElement;
    }

    const spaceBelow = Math.min(spaceBelowWindow, spaceBelowParent);
    const spaceAbove = Math.max(spaceAboveWindow, spaceAboveParent);

    // If less than 260px below and more room above, open upward
    return spaceBelow < 260 && spaceAbove > spaceBelow;
  };

  const handleToggle = () => {
    if (disabled) return;
    if (!isOpen) {
      setOpenUpward(calculateDirection());
      onOpen?.();
      setIsOpen(true);
    } else {
      setIsOpen(false);
    }
  };

  const handleSelect = (option: SelectOption<T>) => {
    if (option.disabled) return;
    onChange(option.value);
    setIsOpen(false);
  };

  return (
    <div
      ref={containerRef}
      className={cn('relative inline-block text-left', fullWidth && 'w-full', className)}
    >
      {label && (
        <label className="block text-xs font-bold text-neutral-700 uppercase mb-1 tracking-wider">
          {label}
        </label>
      )}

      {/* Trigger Button */}
      <button
        type="button"
        disabled={disabled}
        onClick={handleToggle}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        className={cn(
          'w-full flex items-center justify-between gap-2.5 rounded-xl border transition-all duration-200 cursor-pointer text-left',
          'bg-neutral-50 hover:bg-white focus:bg-white',
          sizeClasses[size],
          error
            ? 'border-red-500 focus:ring-2 focus:ring-red-500/20'
            : isOpen
            ? 'border-red-600 ring-2 ring-red-500/20 bg-white'
            : 'border-neutral-300 hover:border-neutral-400',
          disabled && 'opacity-50 cursor-not-allowed bg-neutral-100 hover:bg-neutral-100'
        )}
      >
        <div className="flex items-center gap-2 truncate">
          {prefixIcon && <span className="text-neutral-400 shrink-0">{prefixIcon}</span>}
          {selectedOption ? (
            <span className="flex items-center gap-2 truncate text-neutral-900">
              {selectedOption.icon && <span className="shrink-0">{selectedOption.icon}</span>}
              <span className="truncate">{selectedOption.label}</span>
              {selectedOption.badge && (
                <span className="px-1.5 py-0.5 text-[10px] font-bold rounded-md bg-neutral-200 text-neutral-700">
                  {selectedOption.badge}
                </span>
              )}
            </span>
          ) : (
            <span className="text-neutral-400 font-normal truncate">{placeholder}</span>
          )}
        </div>

        <div className="flex items-center gap-1 shrink-0">
          {isLoading && <Loader2 className="w-3.5 h-3.5 animate-spin text-red-600" />}
          <ChevronDown
            className={cn(
              'w-4 h-4 text-neutral-400 shrink-0 transition-transform duration-200',
              isOpen && 'rotate-180 text-red-600'
            )}
          />
        </div>
      </button>

      {/* Dropdown Menu Popover */}
      {isOpen && !disabled && (
        <div
          role="listbox"
          className={cn(
            'absolute z-50 w-full bg-white rounded-xl border border-neutral-200 shadow-xl p-1.5 max-h-60 overflow-y-auto focus:outline-none animate-in fade-in zoom-in-95 duration-150',
            openUpward ? 'bottom-full mb-1.5' : 'top-full mt-1.5'
          )}
        >
          {isLoading ? (
            <div className="p-3 text-center text-xs text-neutral-500 font-medium flex items-center justify-center gap-2">
              <Loader2 className="w-4 h-4 animate-spin text-red-600" />
              <span>{loadingText}</span>
            </div>
          ) : options.length === 0 ? (
            <div className="p-3 text-center text-xs text-neutral-400">{emptyText}</div>
          ) : (
            options.map((opt) => {
              const isSelected = opt.value === value;
              return (
                <div
                  key={String(opt.value)}
                  role="option"
                  aria-selected={isSelected}
                  onClick={() => handleSelect(opt)}
                  className={cn(
                    'flex items-center justify-between gap-2 px-3 py-2.5 rounded-lg text-xs font-semibold cursor-pointer transition-colors',
                    opt.disabled && 'opacity-40 cursor-not-allowed hover:bg-transparent',
                    !opt.disabled && isSelected
                      ? 'bg-red-50 text-red-700 font-bold'
                      : !opt.disabled && 'text-neutral-800 hover:bg-neutral-100'
                  )}
                >
                  <div className="flex items-center gap-2 truncate">
                    {opt.icon && <span className="shrink-0 text-neutral-500">{opt.icon}</span>}
                    <div className="truncate">
                      <div className="truncate">{opt.label}</div>
                      {opt.description && (
                        <div className="text-[11px] text-neutral-500 font-normal truncate">
                          {opt.description}
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-1.5 shrink-0">
                    {opt.badge && (
                      <span className="px-1.5 py-0.5 text-[10px] font-bold rounded-md bg-neutral-100 text-neutral-600">
                        {opt.badge}
                      </span>
                    )}
                    {isSelected && <Check className="w-4 h-4 text-red-600 shrink-0" />}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {error && <p className="mt-1 text-[11px] text-red-600 font-medium">{error}</p>}
    </div>
  );
};

export default Select;
