const integerFormatterPt = new Intl.NumberFormat('pt-PT')

const dateFormatterPt = new Intl.DateTimeFormat('pt-PT', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
})

export function formatIntegerPt(value) {
  const numericValue = Number(value)
  if (Number.isNaN(numericValue)) {
    return '0'
  }
  return integerFormatterPt.format(numericValue)
}

export function formatDatePt(value) {
  if (!value) {
    return 'N/A'
  }

  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return dateFormatterPt.format(date)
}

export function formatPhonePt(value) {
  if (!value) {
    return 'N/A'
  }

  const digits = String(value).replace(/\D/g, '')

  if (digits.startsWith('351') && digits.length >= 12) {
    const local = digits.slice(-9)
    return `+351 ${local.slice(0, 3)} ${local.slice(3, 6)} ${local.slice(6)}`
  }

  if (digits.length === 9 && digits.startsWith('9')) {
    return `+351 ${digits.slice(0, 3)} ${digits.slice(3, 6)} ${digits.slice(6)}`
  }

  return value
}
