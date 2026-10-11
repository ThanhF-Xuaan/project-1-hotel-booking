import React, { forwardRef, useState, useEffect, useRef, useImperativeHandle } from 'react'
import { Input, type InputProps } from './Input'
import {
  formatCurrencyInput,
  parseCurrency,
  getCursorPosFromDigitCount,
} from '../../utils/currency'

export interface CurrencyInputProps
  extends Omit<InputProps, 'value' | 'onChange' | 'type'> {
  value?: number | string
  onChangeValue?: (numericValue: number) => void
  suffix?: string
}

export const CurrencyInput = forwardRef<HTMLInputElement, CurrencyInputProps>(
  (
    {
      value,
      onChangeValue,
      suffix = 'đ',
      placeholder = '0',
      rightIcon,
      className,
      onKeyDown,
      ...props
    },
    ref
  ) => {
    const internalInputRef = useRef<HTMLInputElement>(null)
    useImperativeHandle(ref, () => internalInputRef.current as HTMLInputElement)

    // Giá trị hiển thị định dạng trong ô input
    const [displayValue, setDisplayValue] = useState<string>(() => {
      const num = parseCurrency(value)
      return num > 0 ? formatCurrencyInput(num) : ''
    })

    // Đồng bộ khi value bên ngoài thay đổi (ví dụ nạp số tiền ban đầu hoặc reset form)
    useEffect(() => {
      const num = parseCurrency(value)
      if (num === 0 && (value === 0 || value === '0' || value === '')) {
        // Nếu người dùng đang tập trung gõ thì giữ nguyên, ngược lại cập nhật
        if (internalInputRef.current !== document.activeElement) {
          setDisplayValue(value === 0 ? '0' : '')
        }
      } else {
        const formatted = formatCurrencyInput(num)
        if (formatted !== displayValue) {
          setDisplayValue(formatted)
        }
      }
    }, [value])

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const raw = e.target.value
      const inputEl = internalInputRef.current

      // Đếm số chữ số trước con trỏ để giữ vị trí chính xác sau khi thêm dấu chấm
      const selStart = inputEl ? inputEl.selectionStart || 0 : raw.length
      const rawBeforeCursor = raw.slice(0, selStart)
      const digitsBeforeCursor = rawBeforeCursor.replace(/[^\d]/g, '').length

      const num = parseCurrency(raw)
      const formatted = raw ? (num === 0 ? '0' : formatCurrencyInput(num)) : ''

      setDisplayValue(formatted)
      onChangeValue?.(num)

      // Cập nhật vị trí con trỏ sau khi React re-render
      requestAnimationFrame(() => {
        if (internalInputRef.current) {
          const newPos = getCursorPosFromDigitCount(formatted, digitsBeforeCursor)
          internalInputRef.current.setSelectionRange(newPos, newPos)
        }
      })
    }

    // Xử lý phím Backspace khi con trỏ đứng ngay sau dấu chấm
    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
      onKeyDown?.(e)
      if (e.defaultPrevented) return

      if (e.key === 'Backspace' && internalInputRef.current) {
        const inputEl = internalInputRef.current
        const selStart = inputEl.selectionStart
        const selEnd = inputEl.selectionEnd

        // Nếu không bôi đen vùng chọn và ký tự ngay trước con trỏ là dấu chấm '.'
        if (selStart !== null && selStart === selEnd && selStart > 1) {
          if (inputEl.value[selStart - 1] === '.') {
            e.preventDefault()
            const curVal = inputEl.value
            // Xóa chữ số đứng trước dấu chấm (vị trí selStart - 2)
            const nextVal = curVal.slice(0, selStart - 2) + curVal.slice(selStart - 1)
            const num = parseCurrency(nextVal)
            const formatted = nextVal ? (num === 0 ? '0' : formatCurrencyInput(num)) : ''

            setDisplayValue(formatted)
            onChangeValue?.(num)

            const targetDigits = curVal
              .slice(0, selStart - 2)
              .replace(/[^\d]/g, '').length

            requestAnimationFrame(() => {
              if (internalInputRef.current) {
                const newPos = getCursorPosFromDigitCount(formatted, targetDigits)
                internalInputRef.current.setSelectionRange(newPos, newPos)
              }
            })
          }
        }
      }
    }

    return (
      <Input
        ref={internalInputRef}
        type="text"
        inputMode="numeric"
        placeholder={placeholder}
        value={displayValue}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        className={className}
        rightIcon={
          rightIcon || (
            <span className="text-xs font-bold text-neutral-400 select-none pr-1">
              {suffix}
            </span>
          )
        }
        {...props}
      />
    )
  }
)

CurrencyInput.displayName = 'CurrencyInput'
export default CurrencyInput
