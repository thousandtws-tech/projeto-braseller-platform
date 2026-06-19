"use client"

import * as React from "react"
import { CalendarIcon, ChevronDownIcon } from "lucide-react"
import { format } from "date-fns"
import { ptBR } from "date-fns/locale"

import { Button } from "@/shared/ui/button"
import { Calendar } from "@/shared/ui/calendar"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/shared/ui/popover"
import { cn } from "@/shared/lib/utils"

type DatePickerProps = {
  id?: string
  name: string
  defaultValue?: string
  disabled?: boolean
  placeholder?: string
  required?: boolean
  displayFormat?: string
  className?: string
  buttonClassName?: string
}

function parseIsoDate(value?: string) {
  if (!value) return undefined

  const [year, month, day] = value.split("-").map(Number)
  if (!year || !month || !day) return undefined

  const date = new Date(year, month - 1, day)
  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day
  ) {
    return undefined
  }

  return date
}

function formatIsoDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")

  return `${year}-${month}-${day}`
}

function DatePicker({
  id,
  name,
  defaultValue = "",
  disabled = false,
  placeholder = "Selecionar data",
  required = false,
  displayFormat = "dd/MM/yyyy",
  className,
  buttonClassName,
}: DatePickerProps) {
  const [open, setOpen] = React.useState(false)
  const [value, setValue] = React.useState(defaultValue)
  const selectedDate = parseIsoDate(value)

  function handleSelect(date?: Date) {
    if (!date) return

    setValue(formatIsoDate(date))
    setOpen(false)
  }

  return (
    <div className={cn("min-w-38", className)}>
      <input
        id={id}
        name={name}
        type="hidden"
        value={value}
        required={required}
      />
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger
          render={
            <Button
              type="button"
              variant="outline"
              disabled={disabled}
              className={cn(
                "w-full justify-start font-normal",
                !selectedDate && "text-muted-foreground",
                buttonClassName
              )}
            >
              <CalendarIcon data-icon="inline-start" />
              <span className="truncate">
                {selectedDate
                  ? format(selectedDate, displayFormat, { locale: ptBR })
                  : placeholder}
              </span>
              <ChevronDownIcon data-icon="inline-end" className="ml-auto" />
            </Button>
          }
        />
        <PopoverContent align="start" className="w-auto p-0">
          <Calendar
            mode="single"
            selected={selectedDate}
            onSelect={handleSelect}
            defaultMonth={selectedDate}
            locale={ptBR}
          />
        </PopoverContent>
      </Popover>
    </div>
  )
}

export { DatePicker }
export default DatePicker
