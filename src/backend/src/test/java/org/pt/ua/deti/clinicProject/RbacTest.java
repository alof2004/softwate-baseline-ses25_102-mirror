package org.pt.ua.deti.clinicProject;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Covers every role × action × resource combination defined in application.yml
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacTest {

    @Autowired
    MockMvc mvc;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor role(String r) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + r));
    }

    private static final String PATIENT_JSON =
            """
            {"name":"Test","dateOfBirth":"1990-01-01","phoneNumber":"+351900000000","email":"t@test.com"}
            """;

    private static final String APPOINTMENT_JSON =
            """
            {"dateTime":"2030-01-01T10:00:00","specialty":"General","status":"SCHEDULED"}
            """;

    // ── PATIENTS ────────────────────────────────────────────────────────────────

    @Test @DisplayName("ADMIN can READ patients")
    void admin_readPatients() throws Exception {
        mvc.perform(get("/api/patients").with(role("ADMIN"))).andExpect(status().isOk());
    }

    @Test @DisplayName("DOCTOR can READ patients")
    void doctor_readPatients() throws Exception {
        mvc.perform(get("/api/patients").with(role("DOCTOR"))).andExpect(status().isOk());
    }

    @Test @DisplayName("RECEPTIONIST can READ patients")
    void receptionist_readPatients() throws Exception {
        mvc.perform(get("/api/patients").with(role("RECEPTIONIST"))).andExpect(status().isOk());
    }

    @Test @DisplayName("ADMIN can CREATE patient")
    void admin_createPatient() throws Exception {
        mvc.perform(post("/api/patients").with(role("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(PATIENT_JSON))
                .andExpect(status().isCreated());
    }

    @Test @DisplayName("RECEPTIONIST can CREATE patient")
    void receptionist_createPatient() throws Exception {
        mvc.perform(post("/api/patients").with(role("RECEPTIONIST"))
                .contentType(MediaType.APPLICATION_JSON).content(PATIENT_JSON))
                .andExpect(status().isCreated());
    }

    @Test @DisplayName("DOCTOR cannot CREATE patient")
    void doctor_cannotCreatePatient() throws Exception {
        mvc.perform(post("/api/patients").with(role("DOCTOR"))
                .contentType(MediaType.APPLICATION_JSON).content(PATIENT_JSON))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("ADMIN can UPDATE patient")
    void admin_updatePatient() throws Exception {
        mvc.perform(put("/api/patients/999").with(role("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(PATIENT_JSON))
                .andExpect(status().isNotFound()); // 404 = auth passed, resource absent
    }

    @Test @DisplayName("DOCTOR can UPDATE patient")
    void doctor_updatePatient() throws Exception {
        mvc.perform(put("/api/patients/999").with(role("DOCTOR"))
                .contentType(MediaType.APPLICATION_JSON).content(PATIENT_JSON))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("RECEPTIONIST cannot UPDATE patient")
    void receptionist_cannotUpdatePatient() throws Exception {
        mvc.perform(put("/api/patients/999").with(role("RECEPTIONIST"))
                .contentType(MediaType.APPLICATION_JSON).content(PATIENT_JSON))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("ADMIN can DELETE patient")
    void admin_deletePatient() throws Exception {
        mvc.perform(delete("/api/patients/999").with(role("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("DOCTOR cannot DELETE patient")
    void doctor_cannotDeletePatient() throws Exception {
        mvc.perform(delete("/api/patients/999").with(role("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("RECEPTIONIST cannot DELETE patient")
    void receptionist_cannotDeletePatient() throws Exception {
        mvc.perform(delete("/api/patients/999").with(role("RECEPTIONIST")))
                .andExpect(status().isForbidden());
    }

    // ── APPOINTMENTS ─────────────────────────────────────────────────────────

    @Test @DisplayName("All roles can READ appointments")
    void allRoles_readAppointments() throws Exception {
        for (String r : new String[]{"ADMIN", "DOCTOR", "RECEPTIONIST"}) {
            mvc.perform(get("/api/appointments").with(role(r))).andExpect(status().isOk());
        }
    }

    @Test @DisplayName("ADMIN can CREATE appointment")
    void admin_createAppointment() throws Exception {
        mvc.perform(post("/api/appointments?patientId=999").with(role("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(APPOINTMENT_JSON))
                .andExpect(status().isNotFound()); // patientId 999 not found
    }

    @Test @DisplayName("RECEPTIONIST can CREATE appointment")
    void receptionist_createAppointment() throws Exception {
        mvc.perform(post("/api/appointments?patientId=999").with(role("RECEPTIONIST"))
                .contentType(MediaType.APPLICATION_JSON).content(APPOINTMENT_JSON))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("DOCTOR cannot CREATE appointment")
    void doctor_cannotCreateAppointment() throws Exception {
        mvc.perform(post("/api/appointments?patientId=999").with(role("DOCTOR"))
                .contentType(MediaType.APPLICATION_JSON).content(APPOINTMENT_JSON))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("ADMIN can DELETE appointment")
    void admin_deleteAppointment() throws Exception {
        mvc.perform(delete("/api/appointments/999").with(role("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("RECEPTIONIST can DELETE appointment")
    void receptionist_deleteAppointment() throws Exception {
        mvc.perform(delete("/api/appointments/999").with(role("RECEPTIONIST")))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("DOCTOR cannot DELETE appointment")
    void doctor_cannotDeleteAppointment() throws Exception {
        mvc.perform(delete("/api/appointments/999").with(role("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test @DisplayName("Unauthenticated request is rejected")
    void unauthenticated_isRejected() throws Exception {
        mvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());
    }
}
