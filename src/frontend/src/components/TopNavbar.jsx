import styles from '../pages/ClinicDashboardPage.module.css'

function TopNavbar({ onCreatePatient, onCreateAppointment }) {
  return (
    <nav className={styles.navbar}>
      <div className={styles.brandBlock}>
        <span className={styles.brandMark} />
        <div>
          <strong>SES Clinic</strong>
          <p>Operations Portal</p>
        </div>
      </div>

      <div className={styles.navLinks}>
        <a href="#patients">Patients</a>
        <a href="#appointments">Appointments</a>
      </div>

      <div className={styles.navActions}>
        <button
          type="button"
          className={`${styles.btn} ${styles.btnGhost}`}
          onClick={onCreatePatient}
        >
          New Patient
        </button>
        <button
          type="button"
          className={`${styles.btn} ${styles.btnPrimary}`}
          onClick={onCreateAppointment}
        >
          New Appointment
        </button>
      </div>
    </nav>
  )
}

export default TopNavbar
