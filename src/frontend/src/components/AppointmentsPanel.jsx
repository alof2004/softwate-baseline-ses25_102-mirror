import { SPECIALTIES, STATUSES, toStatusLabel } from '../constants/clinicOptions'
import styles from '../pages/ClinicDashboardPage.module.css'
import { toDateTimeDisplay } from '../utils/dateTime'

const STATUS_CLASS_BY_VALUE = {
  scheduled: 'statusScheduled',
  completed: 'statusCompleted',
  cancelled: 'statusCancelled',
  rescheduled: 'statusRescheduled',
  'no-show': 'statusNoShow',
}

function AppointmentsPanel({
  appointmentFilters,
  setAppointmentFilters,
  appointmentRows,
  busyAction,
  onApplyFilters,
  onClearFilters,
  onCreate,
  onStartEdit,
  onDelete,
}) {
  return (
    <section id="appointments" className={styles.panel}>
      <div className={styles.panelHeading}>
        <h2>Appointments</h2>
        <p>Manage appointment records and filter directly through the backend.</p>
      </div>

      <div className={styles.panelActions}>
        <button
          type="button"
          className={`${styles.btn} ${styles.btnPrimary}`}
          onClick={onCreate}
          disabled={busyAction}
        >
          New Appointment
        </button>
      </div>

      <form className={styles.filterForm} onSubmit={onApplyFilters}>
        <label>
          Patient Name
          <input
            name="patientName"
            value={appointmentFilters.patientName}
            onChange={(event) =>
              setAppointmentFilters((current) => ({
                ...current,
                patientName: event.target.value,
              }))
            }
            placeholder="Filter by patient name"
          />
        </label>
        <label>
          Date
          <input
            type="date"
            name="date"
            value={appointmentFilters.date}
            onChange={(event) =>
              setAppointmentFilters((current) => ({
                ...current,
                date: event.target.value,
              }))
            }
          />
        </label>
        <label>
          Status
          <select
            name="status"
            value={appointmentFilters.status}
            onChange={(event) =>
              setAppointmentFilters((current) => ({
                ...current,
                status: event.target.value,
              }))
            }
          >
            <option value="">All</option>
            {STATUSES.map((status) => (
              <option key={status} value={status}>
                {toStatusLabel(status)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Specialty
          <select
            name="specialty"
            value={appointmentFilters.specialty}
            onChange={(event) =>
              setAppointmentFilters((current) => ({
                ...current,
                specialty: event.target.value,
              }))
            }
          >
            <option value="">All</option>
            {SPECIALTIES.map((specialty) => (
              <option key={specialty} value={specialty}>
                {specialty}
              </option>
            ))}
          </select>
        </label>
        <div className={styles.buttonRow}>
          <button type="submit" className={`${styles.btn} ${styles.btnPrimary}`} disabled={busyAction}>
            Apply Filters
          </button>
          <button
            type="button"
            className={`${styles.btn} ${styles.btnGhost}`}
            onClick={onClearFilters}
            disabled={busyAction}
          >
            Reset
          </button>
        </div>
      </form>

      <div className={styles.tableWrap}>
        <table>
          <thead>
            <tr>
              <th>Date and Time</th>
              <th>Patient</th>
              <th>Specialty</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {appointmentRows.length === 0 && (
              <tr>
                <td colSpan="5">No appointments found for current filters.</td>
              </tr>
            )}
            {appointmentRows.map((appointment) => {
              const statusClassName = STATUS_CLASS_BY_VALUE[appointment.statusClass]
              const statusClass = statusClassName ? styles[statusClassName] : ''

              return (
                <tr key={appointment.id}>
                  <td>{toDateTimeDisplay(appointment.dateTime)}</td>
                  <td>{appointment.patientName}</td>
                  <td>{appointment.specialty}</td>
                  <td>
                    <span className={`${styles.statusPill} ${statusClass}`.trim()}>
                      {appointment.statusLabel}
                    </span>
                  </td>
                  <td className={styles.actionsCell}>
                    <button
                      type="button"
                      className={`${styles.btn} ${styles.btnGhost}`}
                      onClick={() => onStartEdit(appointment)}
                      disabled={busyAction}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className={`${styles.btn} ${styles.btnDanger}`}
                      onClick={() => onDelete(appointment)}
                      disabled={busyAction}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </section>
  )
}

export default AppointmentsPanel
