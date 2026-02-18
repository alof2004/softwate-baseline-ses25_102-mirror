import { requestApi } from './httpConsumer'

export function fetchPatients() {
  return requestApi('/patients')
}

export function createPatient(payload) {
  return requestApi('/patients', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updatePatient(patientId, payload) {
  return requestApi(`/patients/${patientId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function deletePatient(patientId) {
  return requestApi(`/patients/${patientId}`, { method: 'DELETE' })
}
