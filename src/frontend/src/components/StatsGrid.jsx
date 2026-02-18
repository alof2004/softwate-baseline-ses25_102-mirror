import styles from '../pages/ClinicDashboardPage.module.css'
import { formatIntegerPt } from '../utils/locale'

function StatsGrid({ patientsCount, appointmentsCount, scheduledCount, specialtyCount }) {
  return (
    <section className={styles.statsGrid}>
      <article className={styles.statCard}>
        <p>Patients</p>
        <strong>{formatIntegerPt(patientsCount)}</strong>
      </article>
      <article className={styles.statCard}>
        <p>Appointments</p>
        <strong>{formatIntegerPt(appointmentsCount)}</strong>
      </article>
      <article className={styles.statCard}>
        <p>Scheduled</p>
        <strong>{formatIntegerPt(scheduledCount)}</strong>
      </article>
      <article className={styles.statCard}>
        <p>Specialties</p>
        <strong>{formatIntegerPt(specialtyCount)}</strong>
      </article>
    </section>
  )
}

export default StatsGrid
