import React from 'react'
import { cn } from '../../utils/cn'
import { Loader2 } from 'lucide-react'

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'outline'
  size?: 'sm' | 'md' | 'lg'
  isLoading?: boolean
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
}

export const Button: React.FC<ButtonProps> = ({
  children,
  className,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  disabled,
  leftIcon,
  rightIcon,
  ...props
}) => {
  const baseStyles =
    'inline-flex items-center justify-center font-medium rounded-xl transition-all duration-200 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed select-none focus:outline-none'

  const sizeStyles = {
    sm: 'h-9 px-3 text-sm gap-1.5',
    md: 'h-10 md:h-12 px-4 md:px-5 text-sm md:text-base gap-2',
    lg: 'h-12 md:h-14 px-6 text-base gap-2.5',
  }

  const variantStyles = {
    primary:
      'bg-red-600 hover:bg-red-700 active:bg-red-800 text-white shadow-sm focus:ring-2 focus:ring-red-600 focus:ring-offset-2',
    secondary:
      'bg-neutral-100 hover:bg-neutral-200 active:bg-neutral-300 text-neutral-900 border border-neutral-200 focus:ring-2 focus:ring-neutral-400 focus:ring-offset-2',
    outline:
      'border border-neutral-300 bg-transparent hover:bg-neutral-50 active:bg-neutral-100 text-neutral-800 focus:ring-2 focus:ring-red-600 focus:ring-offset-2',
    danger:
      'bg-red-600 hover:bg-red-700 active:bg-red-800 text-white shadow-sm focus:ring-2 focus:ring-red-600 focus:ring-offset-2',
    ghost:
      'bg-transparent hover:bg-neutral-100 active:bg-neutral-200 text-neutral-700 focus:ring-2 focus:ring-neutral-400',
  }

  return (
    <button
      className={cn(baseStyles, sizeStyles[size], variantStyles[variant], className)}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <Loader2 className="w-4 h-4 animate-spin" />
      ) : (
        <>
          {leftIcon && <span className="inline-flex shrink-0">{leftIcon}</span>}
          {children}
          {rightIcon && <span className="inline-flex shrink-0">{rightIcon}</span>}
        </>
      )}
    </button>
  )
}

export default Button
