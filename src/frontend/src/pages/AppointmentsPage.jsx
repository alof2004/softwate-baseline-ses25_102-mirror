import { useCallback, useEffect, useMemo, useState } from 'react'
import AppointmentFormModal from '../components/AppointmentFormModal'
import AppointmentsPanel from '../components/AppointmentsPanel'
import ConfirmActionModal from '../components/ConfirmActionModal'
import FeedbackBanner from '../components/FeedbackBanner'
import Modal from '../components/Modal'
import {
  createAppointment,
  deleteAppointment,
  fetchAppointments,
  updateAppointment,
} from '../consumers/appointmentConsumer'
import { fetchPatients } from '../consumers/patientConsumer'
import { SPECIALTIES, STATUSES, toStatusLabel } from '../constants/clinicOptions'
import { toDateTimeInput } from '../utils/dateTime'
import { formatIntegerPt } from '../utils/locale'
import styles from './AppointmentsPage.module.css'

const EMPTY_APPOINTMENT_FORM = {
  id: null,
  patientId: '',
  dateTime: '',
  specialty: SPECIALTIES[0],
  status: STATUSES[0],
}

const EMPTY_FILTERS = {
  patientName: '',
  date: '',
  status: '',
  specialty: '',
}

function AppointmentsPage() {
  const [patients, setPatients] = useState([])
  const [appointments, setAppointments] = useState([])
  const [appointmentForm, setAppointmentForm] = useState({ ...EMPTY_APPOINTMENT_FORM })
  const [appointmentFilters, setAppointmentFilters] = useState({ ...EMPTY_FILTERS })
  const [appointmentModalOpen, setAppointmentModalOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [pageLoading, setPageLoading] = useState(true)
  const [busyAction, setBusyAction] = useState(false)
  const [feedback, setFeedback] = useState({ type: '', message: '' })

  const appointmentOwnerById = useMemo(() => {
    const lookup = new Map()
    patients.forEach((patient) => {
      const linkedAppointments = patient.appointments ?? []
      linkedAppointments.forEach((appointment) => {
        lookup.set(appointment.id, {
          id: patient.id,
          name: patient.name,
        })
      })
    })
    return lookup
  }, [patients])

  const appointmentRows = useMemo(() => {
    return appointments.map((appointment) => {
      const owner = appointmentOwnerById.get(appointment.id)
      return {
        ...appointment,
        patientId: appointment.patient?.id ?? owner?.id ?? null,
        patientName: appointment.patient?.name ?? owner?.name ?? 'Unknown patient',
        statusLabel: toStatusLabel(appointment.status ?? 'Unknown'),
        statusClass: (appointment.status ?? 'unknown')
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, '-'),
      }
    })
  }, [appointments, appointmentOwnerById])

  const specialtyCount = useMemo(() => {
    const uniqueSpecialties = new Set(
      appointmentRows.map((appointment) => appointment.specialty?.toLowerCase()).filter(Boolean),
    )
    return uniqueSpecialties.size
  }, [appointmentRows])

  const scheduledCount = useMemo(() => {
    return appointmentRows.filter(
      (appointment) => appointment.status?.toLowerCase() === 'scheduled',
    ).length
  }, [appointmentRows])

  const loadData = useCallback(async (filtersToUse = EMPTY_FILTERS, withLoader = false) => {
    if (withLoader) {
      setPageLoading(true)
    }

    try {
      const [patientData, appointmentData] = await Promise.all([
        fetchPatients(),
        fetchAppointments(filtersToUse),
      ])

      setPatients(patientData)
      setAppointments(appointmentData)
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
    void loadData(EMPTY_FILTERS, true)
  }, [loadData])

  function openCreateAppointmentModal() {
    setAppointmentForm({ ...EMPTY_APPOINTMENT_FORM })
    setAppointmentModalOpen(true)
  }

  function closeAppointmentModal() {
    if (busyAction) {
      return
    }
    setAppointmentModalOpen(false)
    setAppointmentForm({ ...EMPTY_APPOINTMENT_FORM })
  }

  function startAppointmentEdit(appointment) {
    const patientId = appointment.patientId ?? appointment.patient?.id ?? null
    setAppointmentForm({
      id: appointment.id,
      patientId: patientId ? String(patientId) : '',
      dateTime: toDateTimeInput(appointment.dateTime),
      specialty: appointment.specialty ?? SPECIALTIES[0],
      status: appointment.status ?? STATUSES[0],
    })
    setAppointmentModalOpen(true)
  }

  async function handleAppointmentSubmit(event) {
    event.preventDefault()
    if (!appointmentForm.patientId) {
      setFeedback({
        type: 'error',
        message: 'Select a patient for this appointment.',
      })
      return
    }

    setBusyAction(true)
    try {
      const payload = {
        dateTime: appointmentForm.dateTime,
        specialty: appointmentForm.specialty.trim(),
        status: appointmentForm.status.trim(),
      }

      const editing = Boolean(appointmentForm.id)
      if (editing) {
        await updateAppointment(appointmentForm.id, {
          ...payload,
          patient: { id: Number(appointmentForm.patientId) },
        })
      } else {
        await createAppointment(appointmentForm.patientId, payload)
      }

      setAppointmentModalOpen(false)
      setAppointmentForm({ ...EMPTY_APPOINTMENT_FORM })
      await loadData(appointmentFilters)
      setFeedback({
        type: 'success',
        message: editing
          ? 'Appointment updated successfully.'
          : 'Appointment created successfully.',
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

  function requestDeleteAppointment(appointment) {
    setDeleteTarget(appointment)
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
      await deleteAppointment(deleteTarget.id)
      if (appointmentForm.id === deleteTarget.id) {
        setAppointmentModalOpen(false)
        setAppointmentForm({ ...EMPTY_APPOINTMENT_FORM })
      }
      setDeleteTarget(null)
      await loadData(appointmentFilters)
      setFeedback({ type: 'success', message: 'Appointment deleted.' })
    } catch (error) {
      setFeedback({
        type: 'error',
        message: error.message,
      })
    } finally {
      setBusyAction(false)
    }
  }

  async function applyFilters(event) {
    event.preventDefault()
    setBusyAction(true)
    try {
      setFeedback({ type: '', message: '' })
      await loadData(appointmentFilters)
    } finally {
      setBusyAction(false)
    }
  }

  async function clearFilters() {
    const resetFilters = { ...EMPTY_FILTERS }
    setAppointmentFilters(resetFilters)
    setBusyAction(true)
    try {
      setFeedback({ type: '', message: '' })
      await loadData(resetFilters)
    } finally {
      setBusyAction(false)
    }
  }

  return (
    <section className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <p className={styles.kicker}>Appointments</p>
          <h1>Appointments Management</h1>
          <p className={styles.subtitle}>Manage appointment records, filters, and status updates.</p>
        </div>
        <div className={styles.summaryCards}>
          <article className={styles.summaryCard}>
            <p>Total Appointments</p>
            <strong>{formatIntegerPt(appointmentRows.length)}</strong>
          </article>
          <article className={styles.summaryCard}>
            <p>Specialties</p>
            <strong>{formatIntegerPt(specialtyCount)}</strong>
          </article>
          <article className={styles.summaryCard}>
            <p>Scheduled</p>
            <strong>{formatIntegerPt(scheduledCount)}</strong>
          </article>
        </div>
      </header>

      <FeedbackBanner feedback={feedback} />

      {pageLoading ? (
        <div className={styles.loadingState}>Loading appointments from backend...</div>
      ) : (
        <AppointmentsPanel
          appointmentFilters={appointmentFilters}
          setAppointmentFilters={setAppointmentFilters}
          appointmentRows={appointmentRows}
          busyAction={busyAction}
          onApplyFilters={applyFilters}
          onClearFilters={clearFilters}
          onCreate={openCreateAppointmentModal}
          onStartEdit={startAppointmentEdit}
          onDelete={requestDeleteAppointment}
        />
      )}

      <Modal
        isOpen={appointmentModalOpen}
        title={appointmentForm.id ? 'Edit Appointment' : 'New Appointment'}
        onClose={closeAppointmentModal}
      >
        <AppointmentFormModal
          appointmentForm={appointmentForm}
          setAppointmentForm={setAppointmentForm}
          patients={patients}
          busyAction={busyAction}
          onSubmit={handleAppointmentSubmit}
          onCancel={closeAppointmentModal}
        />
      </Modal>

      <Modal isOpen={Boolean(deleteTarget)} title="Confirm Deletion" onClose={closeDeleteModal} maxWidth="460px">
        <ConfirmActionModal
          message="Are you sure you want to delete this appointment?"
          busyAction={busyAction}
          confirmLabel="Delete"
          onConfirm={confirmDelete}
          onCancel={closeDeleteModal}
        />
      </Modal>
    </section>
  )
}

export default AppointmentsPage
