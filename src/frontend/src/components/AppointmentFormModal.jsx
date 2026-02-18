import { SPECIALTIES, STATUSES, toStatusLabel } from '../constants/clinicOptions'
import styles from '../pages/ClinicDashboardPage.module.css'

function AppointmentFormModal({
  appointmentForm,
  setAppointmentForm,
  patients,
  busyAction,
  onSubmit,
  onCancel,
}) {
  const editing = Boolean(appointmentForm.id)

  return (
    <form className={styles.modalForm} onSubmit={onSubmit}>
      <div className={styles.formGrid}>
        <label>
          Patient
          <select
            name="patientId"
            value={appointmentForm.patientId}
            onChange={(event) =>
              setAppointmentForm((current) => ({
                ...current,
                patientId: event.target.value,
              }))
            }
            required
          >
            <option value="">Select patient</option>
            {patients.map((patient) => (
              <option key={patient.id} value={patient.id}>
                {patient.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Date and Time
          <input
            type="datetime-local"
            name="dateTime"
            value={appointmentForm.dateTime}
            onChange={(event) =>
              setAppointmentForm((current) => ({
                ...current,
                dateTime: event.target.value,
              }))
            }
            required
          />
        </label>
        <label>
          Specialty
          <select
            name="specialty"
            value={appointmentForm.specialty}
            onChange={(event) =>
              setAppointmentForm((current) => ({
                ...current,
                specialty: event.target.value,
              }))
            }
            required
          >
            {SPECIALTIES.map((specialty) => (
              <option key={specialty} value={specialty}>
                {specialty}
              </option>
            ))}
          </select>
        </label>
        <label>
          Status
          <select
            name="status"
            value={appointmentForm.status}
            onChange={(event) =>
              setAppointmentForm((current) => ({
                ...current,
                status: event.target.value,
              }))
            }
            required
          >
            {STATUSES.map((status) => (
              <option key={status} value={status}>
                {toStatusLabel(status)}
              </option>
            ))}
          </select>
        </label>
      </div>
      <div className={styles.buttonRow}>
        <button type="submit" className={`${styles.btn} ${styles.btnPrimary}`} disabled={busyAction}>
          {editing ? 'Update Appointment' : 'Create Appointment'}
        </button>
        <button
          type="button"
          className={`${styles.btn} ${styles.btnGhost}`}
          onClick={onCancel}
          disabled={busyAction}
        >
          Cancel
        </button>
      </div>
    </form>
  )
}

export default AppointmentFormModal
