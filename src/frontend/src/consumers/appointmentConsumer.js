import { requestApi, toQueryString } from './httpConsumer'

export function fetchAppointments(filters = {}) {
  const query = toQueryString(filters)
  return requestApi(`/appointments${query}`)
}

export function createAppointment(patientId, payload) {
  return requestApi(`/appointments?patientId=${encodeURIComponent(patientId)}`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateAppointment(appointmentId, payload) {
  return requestApi(`/appointments/${appointmentId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deleteAppointment(appointmentId) {
  return requestApi(`/appointments/${appointmentId}`, {
    method: 'DELETE',
  })
}
