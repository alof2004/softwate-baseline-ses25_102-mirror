const dateTimeFormatterPt = new Intl.DateTimeFormat('pt-PT', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

export function toDateTimeInput(value) {
  if (!value) {
    return ''
  }
  return value.slice(0, 16)
}

export function toDateTimeDisplay(value) {
  if (!value) {
    return 'N/A'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ').slice(0, 16)
  }

  return dateTimeFormatterPt.format(date).replace(',', '')
}
