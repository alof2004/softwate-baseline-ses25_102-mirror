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
  // eslint-disable-next-line security/detect-object-injection -- STATUS_LABELS is a closed constant map; status values come from the API enum
  return STATUS_LABELS[status] ?? status
}
