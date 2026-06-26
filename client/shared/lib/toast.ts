'use client'

import { toast } from 'react-toastify'

const defaultOptions = {
  closeButton: true,
} as const

export const appToast = {
  success(message: string) {
    toast.success(message, defaultOptions)
  },
  error(message: string) {
    toast.error(message, defaultOptions)
  },
  info(message: string) {
    toast.info(message, defaultOptions)
  },
  warning(message: string) {
    toast.warning(message, defaultOptions)
  },
}