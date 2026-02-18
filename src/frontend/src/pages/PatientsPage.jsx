import { useCallback, useEffect, useMemo, useState } from 'react'
import ConfirmActionModal from '../components/ConfirmActionModal'
import FeedbackBanner from '../components/FeedbackBanner'
import Modal from '../components/Modal'
import PatientFormModal from '../components/PatientFormModal'
import PatientsPanel from '../components/PatientsPanel'
import { createPatient, deletePatient, fetchPatients, updatePatient } from '../consumers/patientConsumer'
import { formatIntegerPt } from '../utils/locale'
import styles from './PatientsPage.module.css'

const EMPTY_PATIENT_FORM = {
  id: null,
  name: '',
  dateOfBirth: '',
  phoneNumber: '',
  email: '',
}

function PatientsPage() {
  const [patients, setPatients] = useState([])
  const [patientForm, setPatientForm] = useState({ ...EMPTY_PATIENT_FORM })
  const [patientModalOpen, setPatientModalOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [pageLoading, setPageLoading] = useState(true)
  const [busyAction, setBusyAction] = useState(false)
  const [feedback, setFeedback] = useState({ type: '', message: '' })

  const totalAppointments = useMemo(
    () => patients.reduce((count, patient) => count + (patient.appointments ?? []).length, 0),
    [patients],
  )

  const loadPatients = useCallback(async (withLoader = false) => {
    if (withLoader) {
      setPageLoading(true)
    }

    try {
      const patientData = await fetchPatients()
      setPatients(patientData)
    } catch (error) {
      setFeedback({
        type: 'error',
        message: error.message,
      })
    } finally {
      if (withLoader) {
        setPageLoading(false)
      }
    }
  }, [])

  useEffect(() => {
    void loadPatients(true)
  }, [loadPatients])

  function openCreatePatientModal() {
    setPatientForm({ ...EMPTY_PATIENT_FORM })
    setPatientModalOpen(true)
  }

  function closePatientModal() {
    if (busyAction) {
      return
    }
    setPatientModalOpen(false)
    setPatientForm({ ...EMPTY_PATIENT_FORM })
  }

  function startPatientEdit(patient) {
    setPatientForm({
      id: patient.id,
      name: patient.name,
      dateOfBirth: patient.dateOfBirth,
      phoneNumber: patient.phoneNumber,
      email: patient.email,
    })
    setPatientModalOpen(true)
  }

  async function handlePatientSubmit(event) {
    event.preventDefault()

    setBusyAction(true)
    try {
      const payload = {
        name: patientForm.name.trim(),
        dateOfBirth: patientForm.dateOfBirth,
        phoneNumber: patientForm.phoneNumber.trim(),
        email: patientForm.email.trim(),
      }

      const editing = Boolean(patientForm.id)
      if (editing) {
        await updatePatient(patientForm.id, payload)
      } else {
        await createPatient(payload)
      }

      setPatientModalOpen(false)
      setPatientForm({ ...EMPTY_PATIENT_FORM })
      await loadPatients()
      setFeedback({
        type: 'success',
        message: editing ? 'Patient updated successfully.' : 'Patient created successfully.',
      })
    } catch (error) {
      setFeedback({
        type: 'error',
        message: error.message,
      })
    } finally {
      setBusyAction(false)
    }
  }

  function requestDeletePatient(patient) {
    setDeleteTarget(patient)
  }

  function closeDeleteModal() {
    if (busyAction) {
      return
    }
    setDeleteTarget(null)
  }

  async function confirmDelete() {
    if (!deleteTarget) {
      return
    }

    setBusyAction(true)
    try {
      await deletePatient(deleteTarget.id)
      if (patientForm.id === deleteTarget.id) {
        setPatientModalOpen(false)
        setPatientForm({ ...EMPTY_PATIENT_FORM })
      }
      setDeleteTarget(null)
      await loadPatients()
      setFeedback({ type: 'success', message: 'Patient deleted.' })
    } catch (error) {
      setFeedback({
        type: 'error',
        message: error.message,
      })
    } finally {
      setBusyAction(false)
    }
  }

  return (
    <section className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <p className={styles.kicker}>Patients</p>
          <h1>Patient Management</h1>
          <p className={styles.subtitle}>Handle patient registration, updates, and removals.</p>
        </div>
        <div className={styles.summaryCards}>
          <article className={styles.summaryCard}>
            <p>Total Patients</p>
            <strong>{formatIntegerPt(patients.length)}</strong>
          </article>
          <article className={styles.summaryCard}>
            <p>Total Linked Entries</p>
            <strong>{formatIntegerPt(totalAppointments)}</strong>
          </article>
        </div>
      </header>

      <FeedbackBanner feedback={feedback} />

      {pageLoading ? (
        <div className={styles.loadingState}>Loading patients from backend...</div>
      ) : (
        <PatientsPanel
          patients={patients}
          busyAction={busyAction}
          onCreate={openCreatePatientModal}
          onStartEdit={startPatientEdit}
          onDelete={requestDeletePatient}
        />
      )}

      <Modal
        isOpen={patientModalOpen}
        title={patientForm.id ? 'Edit Patient' : 'New Patient'}
        onClose={closePatientModal}
      >
        <PatientFormModal
          patientForm={patientForm}
          setPatientForm={setPatientForm}
          busyAction={busyAction}
          onSubmit={handlePatientSubmit}
          onCancel={closePatientModal}
        />
      </Modal>

      <Modal isOpen={Boolean(deleteTarget)} title="Confirm Deletion" onClose={closeDeleteModal} maxWidth="460px">
        <ConfirmActionModal
          message={deleteTarget ? `Are you sure you want to delete patient "${deleteTarget.name}"?` : ''}
          busyAction={busyAction}
          confirmLabel="Delete"
          onConfirm={confirmDelete}
          onCancel={closeDeleteModal}
        />
      </Modal>
    </section>
  )
}

export default PatientsPage
