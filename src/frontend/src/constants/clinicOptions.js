export const SPECIALTIES = [
  'General Medicine',
  'Cardiology',
  'Dermatology',
  'Endocrinology',
  'Gastroenterology',
  'Gynecology',
  'Neurology',
  'Oncology',
  'Ophthalmology',
  'Orthopedics',
  'Pediatrics',
  'Psychiatry',
  'Pulmonology',
  'Rheumatology',
  'Urology',
]

export const STATUSES = [
  'Scheduled',
  'Completed',
  'Cancelled',
  'Rescheduled',
  'No-Show',
]

const STATUS_LABELS = {
  'No-Show': 'Did Not Attend',
}

export function toStatusLabel(status) {
  return STATUS_LABELS[status] ?? status
}
