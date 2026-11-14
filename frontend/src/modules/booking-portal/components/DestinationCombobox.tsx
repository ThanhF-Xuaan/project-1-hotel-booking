import React, { useState, useRef, useEffect, useMemo } from 'react';
import { MapPin, Building2, Search, X, ChevronDown } from 'lucide-react';
import { cn } from '../../../core/utils/cn';
import type { DestinationSuggestionDto } from '../types/portal.types';

export interface DestinationComboboxProps {
  destinations: DestinationSuggestionDto[];
  value: string; // e.g. "HOTEL:1" or "REGION:6"
  onChange: (key: string) => void;
  placeholder?: string;
  className?: string;
  error?: string;
  disabled?: boolean;
}

/**
 * Hàm loại bỏ dấu tiếng Việt để so khớp chuỗi tìm kiếm không phân biệt dấu
 */
const removeVietnameseTones = (str: string): string => {
  if (!str) return '';
  return str
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim();
};

export const DestinationCombobox: React.FC<DestinationComboboxProps> = ({
  destinations,
  value,
  onChange,
  placeholder = 'Nhập thành phố hoặc tên khách sạn...',
  className,
  error,
  disabled = false,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listboxRef = useRef<HTMLDivElement>(null);

  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState<string | null>(null);
  const [activeIndex, setActiveIndex] = useState<number>(-1);

  // Tìm điểm đến hiện tại đang được chọn dựa trên value ("TYPE:ID")
  const selectedDestination = useMemo(() => {
    if (!value) return null;
    const [type, idStr] = value.split(':');
    const id = Number(idStr);
    return destinations.find((d) => d.type === type && d.id === id) || null;
  }, [destinations, value]);

  // Giá trị text hiển thị trên ô input: khi đang gõ thì dùng searchQuery, khi không gõ thì hiển thị tên đã chọn
  const displayValue =
    searchQuery !== null ? searchQuery : selectedDestination ? selectedDestination.title : '';

  // Lọc danh sách điểm đến/khách sạn khớp với từ khóa nhập liệu (Accent-insensitive)
  const filteredDestinations = useMemo(() => {
    const rawQuery = (searchQuery ?? '').trim();
    if (!rawQuery) {
      return destinations;
    }

    const normQuery = removeVietnameseTones(rawQuery);
    return destinations.filter((d) => {
      const normTitle = removeVietnameseTones(d.title || '');
      const normSub = removeVietnameseTones(d.subTitle || '');
      return normTitle.includes(normQuery) || normSub.includes(normQuery);
    });
  }, [destinations, searchQuery]);

  // Tách thành 2 nhóm: Khu vực / Thành phố & Khách sạn cơ sở
  const regionMatches = useMemo(
    () => filteredDestinations.filter((d) => d.type === 'REGION'),
    [filteredDestinations]
  );
  const hotelMatches = useMemo(
    () => filteredDestinations.filter((d) => d.type === 'HOTEL'),
    [filteredDestinations]
  );

  // Danh sách phẳng theo thứ tự hiển thị để hỗ trợ phím mũi tên
  const flattenedOptions = useMemo(() => {
    return [...regionMatches, ...hotelMatches];
  }, [regionMatches, hotelMatches]);

  // Đóng dropdown khi click bên ngoài
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
        setSearchQuery(null);
        setActiveIndex(-1);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  const handleSelectOption = (item: DestinationSuggestionDto) => {
    const key = `${item.type}:${item.id}`;
    onChange(key);
    setSearchQuery(null);
    setIsOpen(false);
    setActiveIndex(-1);
    if (inputRef.current) {
      inputRef.current.blur();
    }
  };

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    setSearchQuery('');
    setIsOpen(true);
    setActiveIndex(-1);
    if (inputRef.current) {
      inputRef.current.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (disabled) return;

    if (!isOpen) {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp' || e.key === 'Enter') {
        e.preventDefault();
        setIsOpen(true);
        return;
      }
    }

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIndex((prev) => (prev < flattenedOptions.length - 1 ? prev + 1 : 0));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIndex((prev) => (prev > 0 ? prev - 1 : flattenedOptions.length - 1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (activeIndex >= 0 && activeIndex < flattenedOptions.length) {
        handleSelectOption(flattenedOptions[activeIndex]);
      } else if (flattenedOptions.length === 1) {
        handleSelectOption(flattenedOptions[0]);
      }
    } else if (e.key === 'Escape') {
      e.preventDefault();
      setIsOpen(false);
      setSearchQuery(null);
      setActiveIndex(-1);
    }
  };

  // Cuộn item được chọn bằng phím vào tầm nhìn
  useEffect(() => {
    if (activeIndex >= 0 && listboxRef.current) {
      const activeEl = listboxRef.current.querySelector<HTMLElement>(`[data-index="${activeIndex}"]`);
      if (activeEl) {
        activeEl.scrollIntoView({ block: 'nearest' });
      }
    }
  }, [activeIndex]);

  return (
    <div ref={containerRef} className={cn('relative w-full', className)}>
      {/* Input container */}
      <div
        className={cn(
          'w-full h-11 px-3 bg-white rounded-xl border flex items-center gap-2 transition-all duration-200 cursor-text shadow-2xs',
          error
            ? 'border-red-500 focus-within:ring-2 focus-within:ring-red-500/20'
            : isOpen
            ? 'border-red-600 ring-2 ring-red-500/20'
            : 'border-neutral-300 hover:border-neutral-400',
          disabled && 'opacity-50 cursor-not-allowed bg-neutral-100'
        )}
        onClick={() => {
          if (!disabled) {
            setIsOpen(true);
            if (inputRef.current) {
              inputRef.current.focus();
            }
          }
        }}
      >
        {/* Leading icon */}
        <span className="shrink-0 text-red-600">
          {selectedDestination?.type === 'HOTEL' ? (
            <Building2 className="w-4 h-4 text-blue-600" />
          ) : (
            <MapPin className="w-4 h-4 text-red-600" />
          )}
        </span>

        {/* Text Input */}
        <input
          ref={inputRef}
          type="text"
          disabled={disabled}
          value={displayValue}
          placeholder={placeholder}
          onFocus={() => {
            setIsOpen(true);
            inputRef.current?.select();
          }}
          onChange={(e) => {
            setSearchQuery(e.target.value);
            if (!isOpen) setIsOpen(true);
            setActiveIndex(-1);
          }}
          onKeyDown={handleKeyDown}
          className="w-full bg-transparent text-sm font-semibold text-neutral-900 placeholder:text-neutral-400 placeholder:font-normal focus:outline-none truncate"
        />

        {/* Clear button (X) when has text */}
        {displayValue && !disabled && (
          <button
            type="button"
            onClick={handleClear}
            className="p-1 rounded-full text-neutral-400 hover:text-neutral-600 hover:bg-neutral-100 transition-colors shrink-0 cursor-pointer"
            title="Xóa tìm kiếm"
            aria-label="Xóa tìm kiếm"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        )}

        {/* Toggle chevron */}
        <span className="text-neutral-400 shrink-0 pointer-events-none">
          <ChevronDown
            className={cn(
              'w-4 h-4 transition-transform duration-200',
              isOpen && 'rotate-180 text-red-600'
            )}
          />
        </span>
      </div>

      {/* Dropdown Menu Popover */}
      {isOpen && !disabled && (
        <div
          ref={listboxRef}
          role="listbox"
          className="absolute z-50 left-0 right-0 top-full mt-1.5 min-w-[340px] max-w-md bg-white rounded-2xl border border-neutral-200 shadow-2xl p-2 max-h-80 overflow-y-auto focus:outline-none animate-in fade-in zoom-in-95 duration-150"
        >
          {flattenedOptions.length === 0 ? (
            <div className="p-4 text-center text-xs text-neutral-500 space-y-1">
              <Search className="w-5 h-5 text-neutral-300 mx-auto mb-1" />
              <p className="font-semibold text-neutral-700">Không tìm thấy điểm đến hoặc khách sạn</p>
              <p className="text-neutral-400">
                Không có kết quả nào khớp với &quot;<span className="text-neutral-800 font-bold">{displayValue}</span>&quot;.
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              {/* Nhóm Khu vực / Thành phố */}
              {regionMatches.length > 0 && (
                <div>
                  <div className="px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider text-neutral-400 flex items-center gap-1.5">
                    <MapPin className="w-3 h-3 text-red-500" />
                    <span>Điểm đến & Khu vực</span>
                  </div>
                  <div className="space-y-0.5 mt-0.5">
                    {regionMatches.map((r) => {
                      const flatIndex = flattenedOptions.findIndex(
                        (item) => item.type === r.type && item.id === r.id
                      );
                      const isSelected =
                        selectedDestination?.type === r.type && selectedDestination.id === r.id;
                      const isActive = activeIndex === flatIndex;

                      return (
                        <div
                          key={`REGION:${r.id}`}
                          data-index={flatIndex}
                          role="option"
                          aria-selected={isSelected}
                          onClick={() => handleSelectOption(r)}
                          onMouseEnter={() => setActiveIndex(flatIndex)}
                          className={cn(
                            'flex items-center justify-between gap-2 px-3 py-2.5 rounded-xl text-xs font-semibold cursor-pointer transition-colors',
                            isSelected
                              ? 'bg-red-50 text-red-700 font-bold'
                              : isActive
                              ? 'bg-neutral-100 text-neutral-900'
                              : 'text-neutral-800 hover:bg-neutral-50'
                          )}
                        >
                          <div className="flex items-center gap-2.5 min-w-0 flex-1">
                            <span className="p-1 rounded-lg bg-red-100/70 text-red-600 shrink-0">
                              <MapPin className="w-3.5 h-3.5" />
                            </span>
                            <div className="min-w-0 flex-1">
                              <p className="truncate font-bold text-neutral-900">{r.title}</p>
                              {r.subTitle && (
                                <p className="text-[11px] text-neutral-500 truncate">{r.subTitle}</p>
                              )}
                            </div>
                          </div>
                          <span className="px-2 py-0.5 text-[10px] font-bold rounded-md bg-neutral-100 text-neutral-600 shrink-0 border border-neutral-200">
                            Điểm đến
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Nhóm Khách sạn cơ sở */}
              {hotelMatches.length > 0 && (
                <div className={cn(regionMatches.length > 0 && 'pt-1 border-t border-neutral-100')}>
                  <div className="px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider text-neutral-400 flex items-center gap-1.5">
                    <Building2 className="w-3 h-3 text-blue-500" />
                    <span>Khách sạn cơ sở</span>
                  </div>
                  <div className="space-y-0.5 mt-0.5">
                    {hotelMatches.map((h) => {
                      const flatIndex = flattenedOptions.findIndex(
                        (item) => item.type === h.type && item.id === h.id
                      );
                      const isSelected =
                        selectedDestination?.type === h.type && selectedDestination.id === h.id;
                      const isActive = activeIndex === flatIndex;

                      return (
                        <div
                          key={`HOTEL:${h.id}`}
                          data-index={flatIndex}
                          role="option"
                          aria-selected={isSelected}
                          onClick={() => handleSelectOption(h)}
                          onMouseEnter={() => setActiveIndex(flatIndex)}
                          className={cn(
                            'flex items-center justify-between gap-2 px-3 py-2.5 rounded-xl text-xs font-semibold cursor-pointer transition-colors',
                            isSelected
                              ? 'bg-blue-50 text-blue-700 font-bold'
                              : isActive
                              ? 'bg-neutral-100 text-neutral-900'
                              : 'text-neutral-800 hover:bg-neutral-50'
                          )}
                        >
                          <div className="flex items-center gap-2.5 min-w-0 flex-1">
                            <span className="p-1 rounded-lg bg-blue-100/70 text-blue-600 shrink-0">
                              <Building2 className="w-3.5 h-3.5" />
                            </span>
                            <div className="min-w-0 flex-1">
                              <p className="truncate font-bold text-neutral-900">{h.title}</p>
                              {h.subTitle && (
                                <p className="text-[11px] text-neutral-500 truncate">{h.subTitle}</p>
                              )}
                            </div>
                          </div>
                          <span className="px-2 py-0.5 text-[10px] font-bold rounded-md bg-blue-50 text-blue-600 shrink-0 border border-blue-100">
                            Khách sạn
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default DestinationCombobox;
