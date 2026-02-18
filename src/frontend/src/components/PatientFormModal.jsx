import styles from '../pages/ClinicDashboardPage.module.css'

function PatientFormModal({
  patientForm,
  setPatientForm,
  busyAction,
  onSubmit,
  onCancel,
}) {
  const editing = Boolean(patientForm.id)

  return (
    <form className={styles.modalForm} onSubmit={onSubmit}>
      <div className={styles.formGrid}>
        <label>
          Name
          <input
            name="name"
            value={patientForm.name}
            onChange={(event) =>
              setPatientForm((current) => ({
                ...current,
                name: event.target.value,
              }))
            }
            required
          />
        </label>
        <label>
          Date of Birth
          <input
            type="date"
            name="dateOfBirth"
            value={patientForm.dateOfBirth}
            onChange={(event) =>
              setPatientForm((current) => ({
                ...current,
                dateOfBirth: event.target.value,
              }))
            }
            required
          />
        </label>
        <label>
          Phone Number
          <input
            name="phoneNumber"
            placeholder="+351 912 345 678"
            value={patientForm.phoneNumber}
            onChange={(event) =>
              setPatientForm((current) => ({
                ...current,
                phoneNumber: event.target.value,
              }))
            }
            required
          />
        </label>
        <label>
          Email
          <input
            type="email"
            name="email"
            value={patientForm.email}
            onChange={(event) =>
              setPatientForm((current) => ({
                ...current,
                email: event.target.value,
              }))
            }
            required
          />
        </label>
      </div>
      <div className={styles.buttonRow}>
        <button type="submit" className={`${styles.btn} ${styles.btnPrimary}`} disabled={busyAction}>
          {editing ? 'Update Patient' : 'Create Patient'}
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

export default PatientFormModal
