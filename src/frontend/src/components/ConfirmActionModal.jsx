import styles from '../pages/ClinicDashboardPage.module.css'

function ConfirmActionModal({
  message,
  busyAction,
  confirmLabel = 'Confirm',
  onConfirm,
  onCancel,
}) {
  return (
    <div className={styles.confirmBox}>
      <p>{message}</p>
      <div className={styles.buttonRow}>
        <button
          type="button"
          className={`${styles.btn} ${styles.btnDanger}`}
          onClick={onConfirm}
          disabled={busyAction}
        >
          {confirmLabel}
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
    </div>
  )
}

export default ConfirmActionModal
