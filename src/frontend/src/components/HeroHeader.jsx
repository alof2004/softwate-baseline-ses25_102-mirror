import styles from '../pages/ClinicDashboardPage.module.css'

function HeroHeader() {
  return (
    <header className={styles.hero}>
      <div>
        <p className={styles.eyebrow}>Operations Dashboard</p>
        <h1>Patients and Appointments</h1>
        <p className={styles.heroSubtitle}>
          Clinical platform with full CRUD, live filters, and direct integration with the Spring
          backend.
        </p>
      </div>
      <div className={styles.swatches} aria-hidden="true">
        <span style={{ backgroundColor: '#c3c3c5' }} />
        <span style={{ backgroundColor: '#b2b6b4' }} />
        <span style={{ backgroundColor: '#a5afac' }} />
        <span style={{ backgroundColor: '#709fb6' }} />
        <span style={{ backgroundColor: '#447088' }} />
      </div>
    </header>
  )
}

export default HeroHeader
