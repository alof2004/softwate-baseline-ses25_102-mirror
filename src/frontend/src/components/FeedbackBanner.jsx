import styles from '../pages/ClinicDashboardPage.module.css'

function FeedbackBanner({ feedback }) {
  if (!feedback.message) {
    return null
  }

  const toneClass = feedback.type === 'success' ? styles.feedbackSuccess : styles.feedbackError
  return <p className={`${styles.feedback} ${toneClass}`}>{feedback.message}</p>
}

export default FeedbackBanner
