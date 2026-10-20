package com.dojomanager.dojomanager.ScheduleTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dojomanager.data.dto.authorization.JwtResponse;
import com.dojomanager.data.dto.schedule.ScheduleSessionDTO;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.schedule.WeeklySchedule;
import com.dojomanager.data.repositories.dojo.DojoOwnerRepository;
import com.dojomanager.data.repositories.dojo.DojoRepository;
import com.dojomanager.data.repositories.schedule.WeeklyScheduleRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ScheduleApiIntegrationTest {

    private static final String PASSWORD = "schedule-test-pass";
    private static final String SCHEDULES_URL = "/api/v1/schedules";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private com.dojomanager.services.DojoService dojoService;

    @Autowired
    private DojoOwnerRepository ownerRepository;

    @Autowired
    private DojoRepository dojoRepository;

    @Autowired
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createSessionPersistsWeeklySchedule() {
        TestOwner owner = createOwnerWithDojo();

        ScheduleSessionDTO request = new ScheduleSessionDTO(
                null,
                owner.dojo.getId(),
                "MONDAY",
                "17:00",
                "18:00",
                "Kids Karate",
                null
        );

        ResponseEntity<ScheduleSessionDTO> response = rest.exchange(
                SCHEDULES_URL,
                HttpMethod.POST,
                authed(owner.token, request),
                ScheduleSessionDTO.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        ScheduleSessionDTO created = response.getBody();
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("MONDAY", created.getDayOfWeek());
        assertEquals("Kids Karate", created.getTitle());
        assertEquals("17:00", created.getStartTime());
        assertEquals("18:00", created.getEndTime());
        assertEquals(owner.dojo.getId(), created.getDojoId());

        WeeklySchedule inDb = weeklyScheduleRepository.findById(created.getId()).orElse(null);
        assertNotNull(inDb, "WeeklySchedule must be persisted in DB");
        assertEquals(DayOfWeek.MONDAY, inDb.getDayOfWeek());
        assertEquals(LocalTime.of(17, 0), inDb.getStartTime());
        assertEquals(LocalTime.of(18, 0), inDb.getEndTime());
        assertEquals("Kids Karate", inDb.getTitle());
        assertEquals(owner.dojo.getId(), inDb.getDojo().getId());
    }

    @Test
    void invalidTimesRejectWithBadRequest() {
        TestOwner owner = createOwnerWithDojo();

        // Start time after end time
        ScheduleSessionDTO request = new ScheduleSessionDTO(
                null,
                owner.dojo.getId(),
                "MONDAY",
                "19:00",
                "18:00",
                "Adults Karate",
                null
        );

        ResponseEntity<Map> response = rest.exchange(
                SCHEDULES_URL,
                HttpMethod.POST,
                authed(owner.token, request),
                Map.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updateSessionPersistsChanges() {
        TestOwner owner = createOwnerWithDojo();

        WeeklySchedule session = new WeeklySchedule();
        session.setDojo(owner.dojo);
        session.setDayOfWeek(DayOfWeek.TUESDAY);
        session.setStartTime(LocalTime.of(10, 0));
        session.setEndTime(LocalTime.of(11, 0));
        session.setTitle("Morning Practice");
        session = weeklyScheduleRepository.save(session);

        ScheduleSessionDTO updateDto = new ScheduleSessionDTO(
                session.getId(),
                owner.dojo.getId(),
                "TUESDAY",
                "10:30",
                "11:30",
                "Advanced Practice",
                null
        );

        ResponseEntity<ScheduleSessionDTO> response = rest.exchange(
                SCHEDULES_URL + "/" + session.getId(),
                HttpMethod.PUT,
                authed(owner.token, updateDto),
                ScheduleSessionDTO.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ScheduleSessionDTO updated = response.getBody();
        assertNotNull(updated);
        assertEquals("Advanced Practice", updated.getTitle());
        assertEquals("10:30", updated.getStartTime());

        WeeklySchedule inDb = weeklyScheduleRepository.findById(session.getId()).orElseThrow();
        assertEquals("Advanced Practice", inDb.getTitle());
        assertEquals(LocalTime.of(10, 30), inDb.getStartTime());
    }

    @Test
    void deleteSessionRemovesEntity() {
        TestOwner owner = createOwnerWithDojo();

        WeeklySchedule session = new WeeklySchedule();
        session.setDojo(owner.dojo);
        session.setDayOfWeek(DayOfWeek.WEDNESDAY);
        session.setStartTime(LocalTime.of(16, 0));
        session.setEndTime(LocalTime.of(17, 0));
        session.setTitle("To Delete");
        session = weeklyScheduleRepository.save(session);

        ResponseEntity<Void> response = rest.exchange(
                SCHEDULES_URL + "/" + session.getId(),
                HttpMethod.DELETE,
                authed(owner.token, null),
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertFalse(weeklyScheduleRepository.findById(session.getId()).isPresent());
    }

    @Test
    void crossOwnerAccessIsForbidden() {
        TestOwner ownerA = createOwnerWithDojo();
        TestOwner ownerB = createOwnerWithDojo();

        WeeklySchedule sessionA = new WeeklySchedule();
        sessionA.setDojo(ownerA.dojo);
        sessionA.setDayOfWeek(DayOfWeek.FRIDAY);
        sessionA.setStartTime(LocalTime.of(18, 0));
        sessionA.setEndTime(LocalTime.of(19, 0));
        sessionA.setTitle("Owner A Class");
        sessionA = weeklyScheduleRepository.save(sessionA);

        // Owner B tries to update Owner A's session
        ScheduleSessionDTO updateDto = new ScheduleSessionDTO(
                sessionA.getId(),
                ownerB.dojo.getId(),
                "FRIDAY",
                "18:00",
                "19:00",
                "Hacked Class",
                null
        );

        ResponseEntity<Map> updateResponse = rest.exchange(
                SCHEDULES_URL + "/" + sessionA.getId(),
                HttpMethod.PUT,
                authed(ownerB.token, updateDto),
                Map.class
        );
        assertEquals(HttpStatus.FORBIDDEN, updateResponse.getStatusCode());

        // Owner B tries to delete Owner A's session
        ResponseEntity<Map> deleteResponse = rest.exchange(
                SCHEDULES_URL + "/" + sessionA.getId(),
                HttpMethod.DELETE,
                authed(ownerB.token, null),
                Map.class
        );
        assertEquals(HttpStatus.FORBIDDEN, deleteResponse.getStatusCode());
        assertTrue(weeklyScheduleRepository.findById(sessionA.getId()).isPresent());
    }

    @Test
    void getWeekViewGroupsByDayOfWeekAndIsOwnerScoped() {
        TestOwner ownerA = createOwnerWithDojo();
        TestOwner ownerB = createOwnerWithDojo();

        WeeklySchedule sessionA1 = new WeeklySchedule();
        sessionA1.setDojo(ownerA.dojo);
        sessionA1.setDayOfWeek(DayOfWeek.MONDAY);
        sessionA1.setStartTime(LocalTime.of(9, 0));
        sessionA1.setEndTime(LocalTime.of(10, 0));
        sessionA1.setTitle("Owner A Monday");
        weeklyScheduleRepository.save(sessionA1);

        WeeklySchedule sessionA2 = new WeeklySchedule();
        sessionA2.setDojo(ownerA.dojo);
        sessionA2.setDayOfWeek(DayOfWeek.MONDAY);
        sessionA2.setStartTime(LocalTime.of(10, 0));
        sessionA2.setEndTime(LocalTime.of(11, 0));
        sessionA2.setTitle("Owner A Monday 2");
        weeklyScheduleRepository.save(sessionA2);

        WeeklySchedule sessionB = new WeeklySchedule();
        sessionB.setDojo(ownerB.dojo);
        sessionB.setDayOfWeek(DayOfWeek.MONDAY);
        sessionB.setStartTime(LocalTime.of(14, 0));
        sessionB.setEndTime(LocalTime.of(15, 0));
        sessionB.setTitle("Owner B Monday");
        weeklyScheduleRepository.save(sessionB);

        ParameterizedTypeReference<Map<String, List<ScheduleSessionDTO>>> typeRef =
                new ParameterizedTypeReference<>() {};

        ResponseEntity<Map<String, List<ScheduleSessionDTO>>> response = rest.exchange(
                SCHEDULES_URL + "/week",
                HttpMethod.GET,
                authed(ownerA.token, null),
                typeRef
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, List<ScheduleSessionDTO>> weekMap = response.getBody();
        assertNotNull(weekMap);
        List<ScheduleSessionDTO> mondayList = weekMap.get("MONDAY");
        assertNotNull(mondayList);
        assertEquals(2, mondayList.size());
        assertTrue(mondayList.stream().allMatch(s -> s.getDojoId().equals(ownerA.dojo.getId())));
    }

    private TestOwner createOwnerWithDojo() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "sched-owner-" + suffix + "@example.com";

        DojoOwner owner = ownerRepository.save(new DojoOwner("Test", "Owner", email, passwordEncoder.encode(PASSWORD)));
        Dojo dojo = new Dojo("Dojo " + suffix, "https://example.com/" + suffix);
        DojoOwner savedOwner = dojoService.addDojoToOwner(dojo, owner);
        Dojo savedDojo = dojoRepository.findByOwner(savedOwner).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("fixture dojo was not persisted"));

        String token = login(email);
        return new TestOwner(savedOwner, savedDojo, token);
    }

    private String login(String email) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", PASSWORD);

        ResponseEntity<JwtResponse> response = rest.postForEntity("/api/auth/login", credentials, JwtResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JwtResponse body = response.getBody();
        assertNotNull(body);
        return body.getToken();
    }

    private <T> HttpEntity<T> authed(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private record TestOwner(DojoOwner owner, Dojo dojo, String token) {}
}
