import React, {
  isValidElement,
  useLayoutEffect,
  useRef,
  useState,
  type CSSProperties,
  type ReactNode,
} from 'react'
import { NavLink } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'
import { cn } from '../../utils/cn'

export interface BranchedMenuChild {
  value: string
  label: string
  to?: string
  icon?: ReactNode
  badge?: string
}

export interface BranchedMenuItem {
  label: string
  value?: string
  to?: string
  icon?: ReactNode
  badge?: string
  children?: BranchedMenuChild[]
}

export interface BranchedMenuProps {
  items?: BranchedMenuItem[]
  defaultOpen?: number | number[] | 'all' | 'none' | 'active'
  defaultActive?: string
  active?: string
  onSelect?: (value: string, item: BranchedMenuChild | BranchedMenuItem) => void
  onToggle?: (index: number, open: boolean) => void
  color?: string
  accentColor?: string
  lineColor?: string
  width?: number | string
  rowHeight?: number
  indent?: number
  trunk?: number
  radius?: number
  lineWidth?: number
  fontSize?: number
  drawDuration?: number
  foldDuration?: number
  className?: string
}

const PAD = 6
const MARK = 16

const renderIcon = (icon: ReactNode) => {
  if (isValidElement(icon)) {
    return icon
  }
  return null
}

const isKidActiveMatch = (kid: BranchedMenuChild, targetActive: string) => {
  if (kid.value === targetActive) return true
  if (
    kid.to &&
    (kid.to === targetActive ||
      (kid.to !== '/' && targetActive.startsWith(kid.to + '/')))
  ) {
    return true
  }
  return false
}

const findActiveSection = (
  itemList: BranchedMenuItem[],
  targetActive: string
) => {
  return itemList.findIndex((it) => {
    if (it.children) {
      return it.children.some((kid) => isKidActiveMatch(kid, targetActive))
    }
    const val = it.value ?? it.to ?? it.label
    return (
      val === targetActive ||
      (it.to && (it.to === targetActive || targetActive.startsWith(it.to + '/')))
    )
  })
}

const toSet = (
  open: number | number[] | 'all' | 'none' | 'active' | undefined,
  totalItems: number,
  activeIdx = -1
): Set<number> => {
  if (open === 'all') {
    return new Set(Array.from({ length: totalItems }, (_, i) => i))
  }
  if (open === 'active') {
    return activeIdx >= 0 ? new Set([activeIdx]) : new Set()
  }
  if (open === 'none') {
    return new Set()
  }
  if (Array.isArray(open)) {
    return new Set(open)
  }
  if (typeof open === 'number' && open >= 0) {
    return new Set([open])
  }
  return new Set()
}

export const BranchedMenu: React.FC<BranchedMenuProps> = ({
  items = [],
  defaultOpen = 'active',
  defaultActive = '',
  active: controlledActive,
  onSelect,
  onToggle,
  color = '#171717',
  accentColor = '#dc2626',
  lineColor = '#e5e7eb',
  width = '100%',
  rowHeight = 36,
  indent = 38,
  trunk = 14,
  radius = 9,
  lineWidth = 1.5,
  fontSize = 13,
  drawDuration = 350,
  foldDuration = 260,
  className = '',
}) => {
  const [internalActive, setInternalActive] = useState(() => {
    if (defaultActive) return defaultActive
    const first = items.find((it) => it.children && it.children.length > 0)
    return first?.children?.[0]?.value ?? ''
  })

  const active = controlledActive !== undefined ? controlledActive : internalActive
  const activeSection = findActiveSection(items, active)

  const [open, setOpen] = useState<Set<number>>(() =>
    toSet(defaultOpen, items.length, activeSection)
  )

  const [prevActive, setPrevActive] = useState(active)
  if (active !== prevActive) {
    setPrevActive(active)
    if (defaultOpen === 'active' && activeSection >= 0) {
      setOpen(new Set([activeSection]))
    }
  }

  const navRef = useRef<HTMLElement>(null)
  const heads = useRef<(HTMLButtonElement | HTMLAnchorElement | null)[]>([])
  const markerRef = useRef<HTMLSpanElement>(null)

  const markerShown =
    activeSection >= 0 &&
    (items[activeSection]?.children ? open.has(activeSection) : true)

  useLayoutEffect(() => {
    const place = (glide: boolean) => {
      const m = markerRef.current
      const el = heads.current[activeSection]
      if (!m) return
      const on = markerShown && Boolean(el)
      if (!glide) m.style.transition = 'none'
      if (on && el) {
        m.style.top = `${el.offsetTop + (el.offsetHeight - MARK) / 2}px`
      }
      m.toggleAttribute('data-on', Boolean(on))
      if (!glide) {
        void m.offsetHeight
        m.style.transition = ''
      }
    }

    place(true)

    let first = true
    const ro = new ResizeObserver(() => {
      if (first) {
        first = false
        return
      }
      place(false)
    })

    if (navRef.current) ro.observe(navRef.current)
    return () => ro.disconnect()
  }, [activeSection, markerShown, items, fontSize, rowHeight, open])

  const select = (value: string, item: BranchedMenuChild | BranchedMenuItem) => {
    if (controlledActive === undefined) {
      setInternalActive(value)
    }
    onSelect?.(value, item)
  }

  const toggle = (i: number) => {
    setOpen((prev) => {
      const next = new Set(prev)
      const isOpen = !next.has(i)
      if (isOpen) next.add(i)
      else next.delete(i)
      onToggle?.(i, isOpen)
      return next
    })
  }

  const r = Math.min(radius, rowHeight / 2 - 2)
  const endX = indent - 8
  const rowY = (k: number) => PAD + k * rowHeight + rowHeight / 2
  const branch = (k: number) =>
    `M ${trunk} ${rowY(k) - r} A ${r} ${r} 0 0 0 ${trunk + r} ${rowY(k)} H ${endX}`
  const reach = (k: number) =>
    `M ${trunk} 0 V ${rowY(k) - r} A ${r} ${r} 0 0 0 ${trunk + r} ${rowY(k)} H ${endX}`
  const length = (k: number) =>
    rowY(k) - r + (Math.PI * r) / 2 + (endX - trunk - r)

  const isKidActive = (kid: BranchedMenuChild) => isKidActiveMatch(kid, active)

  return (
    <nav
      ref={navRef}
      className={cn(
        'relative flex w-full flex-col pl-3.5 leading-[1.2] select-none',
        'before:absolute before:top-2.5 before:bottom-2 before:left-0 before:w-[2px] before:rounded-[1px]',
        'before:[background:linear-gradient(to_bottom,var(--bm-line)_0%,var(--bm-line)_70%,transparent_100%)]',
        'before:content-[""]',
        className
      )}
      style={
        {
          '--bm-w': typeof width === 'number' ? `${width}px` : width,
          '--bm-ink': color,
          '--bm-accent': accentColor,
          '--bm-line': lineColor,
          '--bm-font': `${fontSize}px`,
          '--bm-row': `${rowHeight}px`,
          '--bm-indent': `${indent}px`,
          '--bm-line-w': lineWidth,
          '--bm-draw': `${drawDuration}ms`,
          '--bm-muted': `color-mix(in srgb, ${color} 55%, transparent)`,
          '--bm-fold': `${foldDuration}ms`,
        } as CSSProperties
      }
    >
      {/* Gliding Active Section Indicator on the continuous left spine */}
      <span
        ref={markerRef}
        className={cn(
          'absolute -top-px left-0 z-10 h-4 w-[2px] rounded-[1px] opacity-0 pointer-events-none',
          '[background:var(--bm-accent)]',
          '[transition:top_220ms_cubic-bezier(0.23,1,0.32,1),opacity_150ms_ease]',
          'data-[on]:opacity-100 motion-reduce:[transition:opacity_150ms_ease]'
        )}
        aria-hidden="true"
      />

      {items.map((item, i) => {
        const kids = item.children
        const isOpen = kids ? open.has(i) : false
        const leafValue = item.value ?? item.to ?? item.label
        const leafActive =
          !kids &&
          (leafValue === active ||
            (item.to && (item.to === active || (item.to !== '/' && active.startsWith(item.to + '/')))))
        const sectionActive =
          kids?.some((k) => isKidActive(k)) ?? leafActive
        const bodyH = kids ? PAD * 2 + kids.length * rowHeight : 0

        return (
          <div
            key={item.value ?? item.label}
            className="group/section flex flex-col mb-1"
            data-open={isOpen ? '' : undefined}
          >
            {kids ? (
              /* Section Header with Expand/Collapse */
              <button
                ref={(el) => {
                  heads.current[i] = el
                }}
                type="button"
                className={cn(
                  'm-0 flex w-full items-center justify-between cursor-pointer border-0 bg-transparent py-2 px-1 text-left outline-none rounded-lg',
                  '[transition:color_200ms_ease,background-color_150ms_ease]',
                  'hover:bg-neutral-100/70',
                  sectionActive
                    ? 'font-bold text-neutral-900'
                    : 'font-semibold text-neutral-600 hover:text-neutral-900'
                )}
                aria-expanded={isOpen}
                data-active={sectionActive ? '' : undefined}
                onClick={() => toggle(i)}
              >
                <span className="flex items-center gap-2 min-w-0 pr-1">
                  {item.icon ? (
                    <span
                      className={cn(
                        'inline-flex shrink-0 transition-colors duration-150',
                        sectionActive ? 'text-red-600' : 'text-neutral-400 group-hover/section:text-neutral-600'
                      )}
                      aria-hidden="true"
                    >
                      {renderIcon(item.icon)}
                    </span>
                  ) : null}
                  <span className="text-[11px] font-bold tracking-wider uppercase truncate">
                    {item.label}
                  </span>
                </span>
                <ChevronRight
                  className={cn(
                    'w-3.5 h-3.5 shrink-0 text-neutral-400 transition-transform duration-200',
                    isOpen && 'rotate-90 text-neutral-600'
                  )}
                  aria-hidden="true"
                />
              </button>
            ) : item.to ? (
              /* Top-level Leaf NavLink */
              <NavLink
                ref={(el) => {
                  heads.current[i] = el
                }}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    'm-0 flex w-full items-center gap-2.5 cursor-pointer border-0 bg-transparent py-2 px-1.5 text-left outline-none rounded-lg text-sm font-medium',
                    '[transition:color_200ms_ease,background-color_150ms_ease]',
                    isActive || leafActive
                      ? 'font-semibold text-red-600 bg-red-50/60'
                      : 'text-neutral-700 hover:bg-neutral-100/70 hover:text-neutral-900'
                  )
                }
                onClick={() => select(leafValue, item)}
              >
                {item.icon ? (
                  <span
                    className={cn(
                      'inline-flex shrink-0 transition-colors duration-150',
                      leafActive ? 'text-red-600' : 'text-neutral-400'
                    )}
                    aria-hidden="true"
                  >
                    {renderIcon(item.icon)}
                  </span>
                ) : null}
                <span className="truncate">{item.label}</span>
              </NavLink>
            ) : (
              /* Top-level Leaf Button */
              <button
                ref={(el) => {
                  heads.current[i] = el
                }}
                type="button"
                className={cn(
                  'm-0 flex w-full items-center gap-2.5 cursor-pointer border-0 bg-transparent py-2 px-1.5 text-left outline-none rounded-lg text-sm font-medium',
                  '[transition:color_200ms_ease,background-color_150ms_ease]',
                  leafActive
                    ? 'font-semibold text-red-600 bg-red-50/60'
                    : 'text-neutral-700 hover:bg-neutral-100/70 hover:text-neutral-900'
                )}
                data-active={leafActive ? '' : undefined}
                onClick={() => select(leafValue, item)}
              >
                {item.icon ? (
                  <span
                    className={cn(
                      'inline-flex shrink-0 transition-colors duration-150',
                      leafActive ? 'text-red-600' : 'text-neutral-400'
                    )}
                    aria-hidden="true"
                  >
                    {renderIcon(item.icon)}
                  </span>
                ) : null}
                <span className="truncate">{item.label}</span>
              </button>
            )}

            {/* Collapsible Sub-tree with Animated SVG Branches */}
            {kids ? (
              <div
                className="grid [transition:grid-template-rows_var(--bm-fold)_cubic-bezier(0.23,1,0.32,1)] motion-reduce:transition-none"
                style={{ gridTemplateRows: isOpen ? '1fr' : '0fr' }}
              >
                <div className="min-h-0 overflow-hidden">
                  <div className="relative box-border py-1.5" style={{ height: bodyH }}>
                    {/* SVG Branch Tree */}
                    <svg
                      className="pointer-events-none absolute top-0 left-0 overflow-visible transition-opacity duration-200"
                      style={{ opacity: isOpen ? 1 : 0 }}
                      width={indent}
                      height={bodyH}
                      aria-hidden="true"
                    >
                      {/* Vertical Trunk */}
                      <path
                        className="fill-none [stroke:var(--bm-line)] [stroke-width:var(--bm-line-w)] [stroke-linecap:round] [stroke-linejoin:round]"
                        d={`M ${trunk} 0 V ${rowY(kids.length - 1) - r}`}
                      />
                      {/* Inactive Branches */}
                      {kids.map((kid, k) => (
                        <path
                          key={`branch-${kid.value}`}
                          className="fill-none [stroke:var(--bm-line)] [stroke-width:var(--bm-line-w)] [stroke-linecap:round] [stroke-linejoin:round]"
                          d={branch(k)}
                        />
                      ))}
                      {/* Active Branch Trace Line with Animated Dashoffset */}
                      {kids.map((kid, k) => {
                        const kidActive = isKidActive(kid)
                        return (
                          <path
                            key={`reach-${kid.value}`}
                            className="fill-none [stroke:var(--bm-accent)] [stroke-width:var(--bm-line-w)] [stroke-linecap:round] [stroke-linejoin:round] [transition:stroke-dashoffset_var(--bm-draw)_cubic-bezier(0.23,1,0.32,1)] motion-reduce:transition-none"
                            d={reach(k)}
                            style={{
                              strokeDasharray: length(k),
                              strokeDashoffset: kidActive ? 0 : length(k),
                            }}
                          />
                        )
                      })}
                    </svg>

                    {/* Child Navigation Items */}
                    {kids.map((kid) => {
                      const kidActive = isKidActive(kid)
                      const childTo = kid.to || kid.value

                      return (
                        <NavLink
                          key={kid.value}
                          to={childTo}
                          className={({ isActive }) =>
                            cn(
                              'm-0 box-border flex w-full cursor-pointer items-center gap-2.5 border-0 bg-transparent py-0 pr-2 text-left outline-none rounded-r-lg group/item',
                              '[height:var(--bm-row)] [padding-left:var(--bm-indent)]',
                              '[transition:color_180ms_ease,background-color_150ms_ease]',
                              'hover:bg-neutral-50/70',
                              isActive || kidActive
                                ? 'font-semibold text-red-600'
                                : 'text-neutral-600 hover:text-neutral-900'
                            )
                          }
                          aria-current={kidActive ? 'page' : undefined}
                          data-active={kidActive ? '' : undefined}
                          tabIndex={isOpen ? 0 : -1}
                          onClick={() => select(kid.value, kid)}
                        >
                          {kid.icon ? (
                            <span
                              className={cn(
                                'inline-flex flex-none transition-colors duration-150',
                                kidActive
                                  ? 'text-red-600'
                                  : 'text-neutral-400 group-hover/item:text-neutral-700'
                              )}
                              aria-hidden="true"
                            >
                              {renderIcon(kid.icon)}
                            </span>
                          ) : null}
                          <span className="truncate whitespace-nowrap text-[13px]">
                            {kid.label}
                          </span>
                          {kid.badge ? (
                            <span className="ml-auto px-1.5 py-0.5 text-[10px] font-semibold rounded-full bg-red-50 text-red-600 border border-red-200">
                              {kid.badge}
                            </span>
                          ) : null}
                        </NavLink>
                      )
                    })}
                  </div>
                </div>
              </div>
            ) : null}
          </div>
        )
      })}
    </nav>
  )
}

export default BranchedMenu
