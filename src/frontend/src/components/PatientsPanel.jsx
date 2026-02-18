import styles from '../pages/ClinicDashboardPage.module.css'
import { formatDatePt, formatIntegerPt, formatPhonePt } from '../utils/locale'

function PatientsPanel({
  patients,
  busyAction,
  onCreate,
  onStartEdit,
  onDelete,
}) {
  return (
    <section id="patients" className={styles.panel}>
      <div className={styles.panelHeading}>
        <h2>Patients</h2>
        <p>Create, edit, and remove patient records.</p>
      </div>

      <div className={styles.panelActions}>
        <button
          type="button"
          className={`${styles.btn} ${styles.btnPrimary}`}
          onClick={onCreate}
          disabled={busyAction}
        >
          New Patient
        </button>
      </div>

      <div className={styles.tableWrap}>
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Date of Birth</th>
              <th>Phone</th>
              <th>Email</th>
              <th>Appointments</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {patients.length === 0 && (
              <tr>
                <td colSpan="6">No patients found.</td>
              </tr>
            )}
            {patients.map((patient) => (
              <tr key={patient.id}>
                <td>{patient.name}</td>
                <td>{formatDatePt(patient.dateOfBirth)}</td>
                <td>{formatPhonePt(patient.phoneNumber)}</td>
                <td>{patient.email}</td>
                <td>{formatIntegerPt((patient.appointments ?? []).length)}</td>
                <td className={styles.actionsCell}>
                  <button
                    type="button"
                    className={`${styles.btn} ${styles.btnGhost}`}
                    onClick={() => onStartEdit(patient)}
                    disabled={busyAction}
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    className={`${styles.btn} ${styles.btnDanger}`}
                    onClick={() => onDelete(patient)}
                    disabled={busyAction}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

export default PatientsPanel
