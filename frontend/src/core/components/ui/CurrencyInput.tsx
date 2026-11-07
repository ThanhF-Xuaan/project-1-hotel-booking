import React, {
  forwardRef,
  useState,
  useEffect,
  useLayoutEffect,
  useRef,
  useImperativeHandle,
} from 'react'
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
  maxDigits?: number
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
      maxDigits = 13,
      onKeyDown,
      error,
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

    // Thông báo lỗi khi nhập quá giới hạn maxDigits
    const [limitError, setLimitError] = useState<string>('')

    // Lưu vị trí số chữ số trước con trỏ để phục hồi đồng bộ trong useLayoutEffect
    const cursorDigitsRef = useRef<number | null>(null)

    // Phục hồi vị trí con trỏ ngay sau khi React commit DOM (trước khi browser paint)
    useLayoutEffect(() => {
      if (
        cursorDigitsRef.current !== null &&
        internalInputRef.current === document.activeElement
      ) {
        const targetDigits = cursorDigitsRef.current
        cursorDigitsRef.current = null
        const newPos = getCursorPosFromDigitCount(displayValue, targetDigits)
        internalInputRef.current?.setSelectionRange(newPos, newPos)
      }
    }, [displayValue])

    // Đồng bộ khi value từ props bên ngoài thay đổi (ví dụ reset form, nạp dữ liệu edit)
    useEffect(() => {
      const inputEl = internalInputRef.current
      const numProp = parseCurrency(value)
      const numCurrent = parseCurrency(inputEl?.value)

      // Nếu người dùng đang focus gõ thì không can thiệp nếu giá trị số khớp nhau
      if (inputEl && inputEl === document.activeElement) {
        if (numProp === numCurrent) {
          return
        }
      }

      if (numProp === 0 && (value === 0 || value === '0' || value === '' || value === undefined)) {
        setDisplayValue((prev) => (prev !== '' ? '' : prev))
      } else {
        const clean = String(numProp).slice(0, maxDigits)
        const formatted = formatCurrencyInput(Number(clean))
        setDisplayValue((prev) => (prev !== formatted ? formatted : prev))
      }
    }, [value, maxDigits])

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const raw = e.target.value
      const inputEl = internalInputRef.current

      // Đếm số chữ số trước con trỏ
      const selStart = inputEl ? inputEl.selectionStart || 0 : raw.length
      const rawBeforeCursor = raw.slice(0, selStart)
      let digitsBeforeCursor = rawBeforeCursor.replace(/[^\d]/g, '').length

      // Lấy toàn bộ chữ số và khống chế tối đa maxDigits
      let cleanDigits = raw.replace(/[^\d]/g, '')
      if (cleanDigits.length > maxDigits) {
        cleanDigits = cleanDigits.slice(0, maxDigits)
        digitsBeforeCursor = Math.min(digitsBeforeCursor, maxDigits)
        setLimitError('Không được nhập quá 13 số')
      } else {
        setLimitError('')
      }

      const num = cleanDigits ? parseCurrency(cleanDigits) : 0
      const formatted = cleanDigits
        ? num === 0
          ? '0'
          : formatCurrencyInput(cleanDigits)
        : ''

      // Lưu số lượng chữ số trước con trỏ để useLayoutEffect phục hồi
      cursorDigitsRef.current = digitsBeforeCursor

      setDisplayValue(formatted)
      onChangeValue?.(num)
    }

    // Xử lý bàn phím: Backspace, Delete, dấu chấm / phẩy, giới hạn maxDigits
    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
      onKeyDown?.(e)
      if (e.defaultPrevented) return

      const inputEl = internalInputRef.current
      if (!inputEl) return

      const selStart = inputEl.selectionStart ?? 0
      const selEnd = inputEl.selectionEnd ?? 0

      // 1. Chặn nhập thêm số nếu đã đạt tối đa maxDigits và không có bôi đen vùng chọn
      const isDigit = /^[0-9]$/.test(e.key)
      if (isDigit) {
        const currentDigits = inputEl.value.replace(/[^\d]/g, '').length
        const selectedDigits = inputEl.value
          .slice(selStart, selEnd)
          .replace(/[^\d]/g, '').length
        if (currentDigits - selectedDigits >= maxDigits) {
          e.preventDefault()
          setLimitError('Không được nhập quá 13 số')
          return
        }
      }

      // 2. Chặn nhập dấu chấm / phẩy gây nhảy số (tiền VNĐ là số nguyên)
      if (e.key === '.' || e.key === ',') {
        e.preventDefault()
        // Nếu con trỏ đang đứng ngay trước dấu chấm, nhảy con trỏ qua dấu chấm
        if (inputEl.value[selStart] === '.') {
          inputEl.setSelectionRange(selStart + 1, selStart + 1)
        }
        return
      }

      // 3. Xử lý đồng nhất cho cả Backspace và Delete:
      // Xóa chữ số phía trước con trỏ chuột (ngăn việc bấm Delete lại xóa ký tự phía sau con trỏ)
      if (e.key === 'Backspace' || e.key === 'Delete') {
        e.preventDefault()
        setLimitError('')
        const curVal = inputEl.value

        let nextVal: string
        let targetDigits: number

        if (selStart !== selEnd) {
          // Xóa vùng đang bôi đen chọn
          nextVal = curVal.slice(0, selStart) + curVal.slice(selEnd)
          targetDigits = curVal.slice(0, selStart).replace(/[^\d]/g, '').length
        } else if (selStart > 0) {
          if (curVal[selStart - 1] === '.') {
            // Đứng sau dấu chấm '.', xóa chữ số đứng trước dấu chấm
            nextVal = curVal.slice(0, selStart - 2) + curVal.slice(selStart - 1)
            targetDigits = curVal.slice(0, selStart - 2).replace(/[^\d]/g, '').length
          } else {
            // Xóa chữ số đứng ngay trước con trỏ
            nextVal = curVal.slice(0, selStart - 1) + curVal.slice(selStart)
            targetDigits = curVal.slice(0, selStart - 1).replace(/[^\d]/g, '').length
          }
        } else {
          // Con trỏ đang ở đầu ô nhập (vị trí 0), không có gì để xóa
          return
        }

        const cleanDigits = nextVal.replace(/[^\d]/g, '').slice(0, maxDigits)
        const num = cleanDigits ? parseCurrency(cleanDigits) : 0
        const formatted = num > 0 ? formatCurrencyInput(cleanDigits) : ''

        cursorDigitsRef.current = targetDigits
        setDisplayValue(formatted)
        onChangeValue?.(num)
      }
    }

    // 13 số + 4 dấu chấm = 17 ký tự hiển thị
    const maxFormattedLength = maxDigits + Math.floor((maxDigits - 1) / 3)
    const effectiveError = error || limitError

    return (
      <Input
        ref={internalInputRef}
        type="text"
        inputMode="numeric"
        placeholder={placeholder}
        value={displayValue}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        maxLength={maxFormattedLength}
        error={effectiveError}
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
