import { useCallback, useEffect, useMemo, useState } from 'react'
import AppointmentFormModal from '../components/AppointmentFormModal'
import AppointmentsPanel from '../components/AppointmentsPanel'
import ConfirmActionModal from '../components/ConfirmActionModal'
import FeedbackBanner from '../components/FeedbackBanner'
import HeroHeader from '../components/HeroHeader'
import Modal from '../components/Modal'
import PatientFormModal from '../components/PatientFormModal'
import PatientsPanel from '../components/PatientsPanel'
import StatsGrid from '../components/StatsGrid'
import TopNavbar from '../components/TopNavbar'
import {
  createAppointment,
  deleteAppointment,
  fetchAppointments,
  updateAppointment,
} from '../consumers/appointmentConsumer'
import {
  createPatient,
  deletePatient,
  fetchPatients,
  updatePatient,
} from '../consumers/patientConsumer'
import { SPECIALTIES, STATUSES, toStatusLabel } from '../constants/clinicOptions'
import { toDateTimeInput } from '../utils/dateTime'
import styles from './ClinicDashboardPage.module.css'

const EMPTY_PATIENT_FORM = {
  id: null,
  name: '',
  dateOfBirth: '',
  phoneNumber: '',
  email: '',
}

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

const EMPTY_DELETE_STATE = {
  open: false,
  type: '',
  target: null,
}

function ClinicDashboardPage() {
  const [patients, setPatients] = useState([])
  const [appointments, setAppointments] = useState([])
  const [patientForm, setPatientForm] = useState({ ...EMPTY_PATIENT_FORM })
  const [appointmentForm, setAppointmentForm] = useState({ ...EMPTY_APPOINTMENT_FORM })
  const [appointmentFilters, setAppointmentFilters] = useState({ ...EMPTY_FILTERS })
  const [patientModalOpen, setPatientModalOpen] = useState(false)
  const [appointmentModalOpen, setAppointmentModalOpen] = useState(false)
  const [deleteState, setDeleteState] = useState({ ...EMPTY_DELETE_STATE })
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

  const scheduledCount = useMemo(() => {
    return appointmentRows.filter(
      (appointment) => appointment.status?.toLowerCase() === 'scheduled',
    ).length
  }, [appointmentRows])

  const specialtyCount = useMemo(() => {
    const specialties = new Set(
      appointmentRows.map((appointment) => appointment.specialty?.toLowerCase()).filter(Boolean),
    )
    return specialties.size
  }, [appointmentRows])

  const deleteMessage = useMemo(() => {
    if (!deleteState.target) {
      return ''
    }

    if (deleteState.type === 'patient') {
      return `Are you sure you want to delete patient "${deleteState.target.name}"?`
    }

    return 'Are you sure you want to delete this appointment?'
  }, [deleteState])

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

  function openCreatePatientModal() {
    setPatientForm({ ...EMPTY_PATIENT_FORM })
    setPatientModalOpen(true)
  }

  function openCreateAppointmentModal() {
    setAppointmentForm({ ...EMPTY_APPOINTMENT_FORM })
    setAppointmentModalOpen(true)
  }

  function closePatientModal() {
    if (busyAction) {
      return
    }
    setPatientModalOpen(false)
    setPatientForm({ ...EMPTY_PATIENT_FORM })
  }

  function closeAppointmentModal() {
    if (busyAction) {
      return
    }
    setAppointmentModalOpen(false)
    setAppointmentForm({ ...EMPTY_APPOINTMENT_FORM })
  }

  function closeDeleteModal() {
    if (busyAction) {
      return
    }
    setDeleteState({ ...EMPTY_DELETE_STATE })
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
      await loadData(appointmentFilters)
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

  function requestDeletePatient(patient) {
    setDeleteState({
      open: true,
      type: 'patient',
      target: patient,
    })
  }

  function requestDeleteAppointment(appointment) {
    setDeleteState({
      open: true,
      type: 'appointment',
      target: appointment,
    })
  }

  async function confirmDelete() {
    if (!deleteState.target) {
      return
    }

    setBusyAction(true)
    try {
      if (deleteState.type === 'patient') {
        await deletePatient(deleteState.target.id)
        if (patientForm.id === deleteState.target.id) {
          setPatientModalOpen(false)
          setPatientForm({ ...EMPTY_PATIENT_FORM })
        }
        setFeedback({ type: 'success', message: 'Patient deleted.' })
      } else {
        await deleteAppointment(deleteState.target.id)
        if (appointmentForm.id === deleteState.target.id) {
          setAppointmentModalOpen(false)
          setAppointmentForm({ ...EMPTY_APPOINTMENT_FORM })
        }
        setFeedback({ type: 'success', message: 'Appointment deleted.' })
      }

      setDeleteState({ ...EMPTY_DELETE_STATE })
      await loadData(appointmentFilters)
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
    <main className={styles.appShell}>
      <TopNavbar
        onCreatePatient={openCreatePatientModal}
        onCreateAppointment={openCreateAppointmentModal}
      />
      <HeroHeader />
      <FeedbackBanner feedback={feedback} />

      {pageLoading ? (
        <div className={styles.loadingState}>Loading data from backend...</div>
      ) : (
        <>
          <StatsGrid
            patientsCount={patients.length}
            appointmentsCount={appointmentRows.length}
            scheduledCount={scheduledCount}
            specialtyCount={specialtyCount}
          />

          <div className={styles.layoutGrid}>
            <PatientsPanel
              patients={patients}
              busyAction={busyAction}
              onCreate={openCreatePatientModal}
              onStartEdit={startPatientEdit}
              onDelete={requestDeletePatient}
            />
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
          </div>
        </>
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

      <Modal
        isOpen={deleteState.open}
        title="Confirm Deletion"
        onClose={closeDeleteModal}
        maxWidth="460px"
      >
        <ConfirmActionModal
          message={deleteMessage}
          busyAction={busyAction}
          confirmLabel="Delete"
          onConfirm={confirmDelete}
          onCancel={closeDeleteModal}
        />
      </Modal>
    </main>
  )
}

export default ClinicDashboardPage
