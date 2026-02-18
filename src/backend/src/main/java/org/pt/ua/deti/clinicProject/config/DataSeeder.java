package org.pt.ua.deti.clinicProject.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.pt.ua.deti.clinicProject.models.Appointment;
import org.pt.ua.deti.clinicProject.models.Patient;
import org.pt.ua.deti.clinicProject.repositories.AppointmentRepository;
import org.pt.ua.deti.clinicProject.repositories.PatientRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private static final List<String> FIRST_NAMES =
            List.of(
                    "Afonso",
                    "Beatriz",
                    "Bernardo",
                    "Catarina",
                    "Diogo",
                    "Duarte",
                    "Francisca",
                    "Goncalo",
                    "Ines",
                    "Joao",
                    "Leonor",
                    "Madalena",
                    "Mafalda",
                    "Matilde",
                    "Miguel",
                    "Rita",
                    "Rodrigo",
                    "Salvador",
                    "Santiago",
                    "Tiago");

    private static final List<String> LAST_NAMES =
            List.of(
                    "Silva",
                    "Santos",
                    "Rodrigues",
                    "Costa",
                    "Ferreira",
                    "Martins",
                    "Pereira",
                    "Jesus",
                    "Fernandes",
                    "Ribeiro",
                    "Goncalves",
                    "Almeida",
                    "Araujo",
                    "Teixeira",
                    "Carvalho",
                    "Ramos",
                    "Figueiredo",
                    "Mota",
                    "Coelho",
                    "Neves",
                    "Correia",
                    "Sousa");

    private static final List<String> SPECIALTIES =
            List.of(
                    "General Medicine",
                    "Cardiology",
                    "Dermatology",
                    "Endocrinology",
                    "Gastroenterology",
                    "Gynecology",
                    "Neurology",
                    "Oncology",
                    "Ophthalmology",
                    "Orthopedics",
                    "Pediatrics",
                    "Psychiatry",
                    "Pulmonology",
                    "Rheumatology",
                    "Urology");

    private static final List<String> STATUSES =
            List.of("Scheduled", "Completed", "Cancelled", "Rescheduled", "No-Show");

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    public DataSeeder(
            PatientRepository patientRepository, AppointmentRepository appointmentRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public void run(String... args) {
        if (patientRepository.count() > 0 || appointmentRepository.count() > 0) {
            return;
        }

        List<Patient> patients = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            String firstName = FIRST_NAMES.get(i % FIRST_NAMES.size());
            String lastName = LAST_NAMES.get((i * 3) % LAST_NAMES.size());
            int mobilePrefix = 910 + (i % 80);
            int middle = 100 + ((i * 7) % 900);
            int suffix = 100 + ((i * 11) % 900);

            Patient patient = new Patient();
            patient.setName(firstName + " " + lastName);
            patient.setDateOfBirth(LocalDate.of(1958 + (i % 47), (i % 12) + 1, (i % 28) + 1));
            patient.setPhoneNumber(
                    String.format("+351 %03d %03d %03d", mobilePrefix, middle, suffix));
            patient.setEmail(
                    (firstName + "." + lastName + i + "@clinic.local").toLowerCase(Locale.ROOT));
            patients.add(patient);
        }

        List<Patient> savedPatients = patientRepository.saveAll(patients);
        LocalDateTime baseDateTime = LocalDateTime.now().minusDays(140);

        List<Appointment> appointments = new ArrayList<>();
        for (int i = 0; i < 520; i++) {
            Appointment appointment = new Appointment();
            appointment.setPatient(savedPatients.get(i % savedPatients.size()));
            appointment.setDateTime(baseDateTime.plusHours((long) i * 9 + (i % 7)));
            appointment.setSpecialty(SPECIALTIES.get(i % SPECIALTIES.size()));
            appointment.setStatus(STATUSES.get(i % STATUSES.size()));
            appointments.add(appointment);
        }

        appointmentRepository.saveAll(appointments);
    }
}
