package com.dojomanager.dojomanager.AttendanceTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dojomanager.data.dto.attendance.AttendanceRecordDto;
import com.dojomanager.data.dto.authorization.JwtResponse;
import com.dojomanager.data.dto.student.StudentDto;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.schedule.DailySchedule;
import com.dojomanager.data.entities.schedule.WeeklySchedule;
import com.dojomanager.data.entities.rank.BeltColor;
import com.dojomanager.data.entities.rank.RankLevel;
import com.dojomanager.data.entities.rank.RankName;
import com.dojomanager.data.entities.rank.RankSetting;
import com.dojomanager.data.repositories.dojo.DojoRepository;
import com.dojomanager.data.repositories.schedule.DailyScheduleRepository;
import com.dojomanager.data.repositories.schedule.WeeklyScheduleRepository;
import com.dojomanager.services.DojoOwnerService;
import com.dojomanager.services.DojoService;
import com.dojomanager.services.RankService;

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

/**
 * End-to-end tests for the attendance check-in API: a real servlet container
 * on a random port, real HTTP requests, real JWT bearer tokens obtained through
 * {@code POST /api/auth/login}. Owner isolation, duplicate detection and
 * session-based check-ins are therefore proven with requests rather than by
 * reading the code.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AttendanceApiIntegrationTest {

    private static final String PASSWORD = "attendance-api-pass";
    private static final String ATTENDANCE_URL = "/api/v1/attendance";
    private static final String STUDENTS_URL = "/api/v1/students";
    private static final LocalDate SESSION_DATE = LocalDate.of(2026, 1, 5);

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DojoOwnerService ownerService;

    @Autowired
    private DojoService dojoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DojoRepository dojoRepository;

    @Autowired
    private DailyScheduleRepository dailyScheduleRepository;

    @Autowired
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @Autowired
    private RankService rankService;

    @Test
    void checkInAddsDurationToStudentHoursInRankAndDeletionRevertsIt() {
        OwnerFixture owner = fixture("hours");
        Dojo dojo = owner.dojo();

        // 1. Setup rank for dojo
        RankName rankName = rankService.saveRankName(new RankName("White Belt", dojo));
        RankLevel rankLevel = rankService.saveRankLevel(new RankLevel(0, 1, 20, rankName));
        BeltColor beltColor = rankService.saveBeltColor(new BeltColor("White", null, 0, rankLevel));
        RankSetting rankSetting = rankService.saveRankSetting(new RankSetting(dojo, rankName, rankLevel, beltColor));

        // 2. Create student assigned to this rank
        Map<String, Object> sBody = new HashMap<>();
        sBody.put("firstName", "Attendance");
        sBody.put("lastName", "Tester");
        sBody.put("email", unique("attend"));
        sBody.put("rankId", rankSetting.getId());
        sBody.put("dojoId", dojo.getId());

        ResponseEntity<StudentDto> studentRes = rest.exchange(STUDENTS_URL, HttpMethod.POST,
                authed(owner.token(), sBody), StudentDto.class);
        assertEquals(HttpStatus.CREATED, studentRes.getStatusCode());
        Long studentId = studentRes.getBody().getId();
        assertEquals(0.0, studentRes.getBody().getHoursInRank());

        // 3. Create weekly session of 1.5 hours duration (17:00 to 18:30)
        WeeklySchedule weekly = new WeeklySchedule();
        weekly.setDojo(dojo);
        weekly.setDayOfWeek(DayOfWeek.MONDAY);
        weekly.setStartTime(LocalTime.of(17, 0));
        weekly.setEndTime(LocalTime.of(18, 30));
        weekly.setTitle("Evening Karate");
        weekly = weeklyScheduleRepository.save(weekly);

        // 4. Check in student for this session
        Map<String, Object> checkInBody = new HashMap<>();
        checkInBody.put("studentId", studentId);
        checkInBody.put("status", "PRESENT");
        checkInBody.put("sessionId", weekly.getId());
        checkInBody.put("date", SESSION_DATE.toString());

        ResponseEntity<List<AttendanceRecordDto>> checkInRes = rest.exchange(ATTENDANCE_URL, HttpMethod.POST,
                authed(owner.token(), checkInBody),
                new ParameterizedTypeReference<List<AttendanceRecordDto>>() {});
        assertEquals(HttpStatus.CREATED, checkInRes.getStatusCode());
        Long recordId = checkInRes.getBody().get(0).getId();

        // 5. Verify student has gained 1.5 hours
        ResponseEntity<StudentDto> updatedStudent = rest.exchange(STUDENTS_URL + "/" + studentId,
                HttpMethod.GET, authed(owner.token(), null), StudentDto.class);
        assertEquals(HttpStatus.OK, updatedStudent.getStatusCode());
        assertEquals(1.5, updatedStudent.getBody().getHoursInRank());

        // 6. Delete attendance record and verify hours reverted
        ResponseEntity<Void> deleteRes = rest.exchange(ATTENDANCE_URL + "/" + recordId,
                HttpMethod.DELETE, authed(owner.token(), null), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleteRes.getStatusCode());

        ResponseEntity<StudentDto> revertedStudent = rest.exchange(STUDENTS_URL + "/" + studentId,
                HttpMethod.GET, authed(owner.token(), null), StudentDto.class);
        assertEquals(0.0, revertedStudent.getBody().getHoursInRank());
    }

    @Test
    void ownerCanCheckInListAndFetchAttendance() {
        OwnerFixture owner = fixture("a");
        StudentDto student = createStudent(owner.token(), "Ada", "Lovelace", unique("ada"));
        List<AttendanceRecordDto> created = checkIn(owner.token(), student.getId(), "PRESENT", null);

        assertEquals(1, created.size());
        AttendanceRecordDto record = created.get(0);
        assertEquals(student.getId(), record.getStudentId(), "check-in must belong to the student");
        assertEquals("PRESENT", record.getStatus().name());
        assertNotNull(record.getSessionDate(), "date-based check-in must carry a date");

        // Listing by the same date returns exactly that record.
        ResponseEntity<List<AttendanceRecordDto>> listed = rest.exchange(ATTENDANCE_URL + "?date=" + SESSION_DATE,
                HttpMethod.GET, authed(owner.token(), null),
                new ParameterizedTypeReference<List<AttendanceRecordDto>>() {});
        assertEquals(HttpStatus.OK, listed.getStatusCode());
        assertEquals(1, listed.getBody().size());

        // Fetching the record by id returns it.
        ResponseEntity<AttendanceRecordDto> fetched = rest.exchange(ATTENDANCE_URL + "/" + record.getId(),
                HttpMethod.GET, authed(owner.token(), null), AttendanceRecordDto.class);
        assertEquals(HttpStatus.OK, fetched.getStatusCode());
        assertEquals(student.getId(), fetched.getBody().getStudentId());
    }

    @Test
    void ownerBCannotSeeOrReadOwnerAttendance() {
        OwnerFixture ownerA = fixture("a");
        OwnerFixture ownerB = fixture("b");

        StudentDto student = createStudent(ownerA.token(), "Ada", "Lovelace", unique("isolation"));
        List<AttendanceRecordDto> created = checkIn(ownerA.token(), student.getId(), "PRESENT", null);
        Long recordId = created.get(0).getId();

        // B's listing for that date is empty - A's record is not exposed.
        ResponseEntity<List<AttendanceRecordDto>> listed = rest.exchange(ATTENDANCE_URL + "?date=" + SESSION_DATE,
                HttpMethod.GET, authed(ownerB.token(), null),
                new ParameterizedTypeReference<List<AttendanceRecordDto>>() {});
        assertEquals(HttpStatus.OK, listed.getStatusCode());
        assertEquals(0, listed.getBody().size(), "owner B must not see owner A's attendance");

        // B cannot read the record directly.
        assertEquals(HttpStatus.NOT_FOUND, rest.exchange(ATTENDANCE_URL + "/" + recordId,
                HttpMethod.GET, authed(ownerB.token(), null), String.class).getStatusCode());
    }

    @Test
    void duplicateCheckInIsRejected() {
        OwnerFixture owner = fixture("a");
        StudentDto student = createStudent(owner.token(), "Ada", "Lovelace", unique("dup"));
        Map<String, Object> body = checkInBody(student.getId(), "PRESENT", null, SESSION_DATE);

        assertEquals(HttpStatus.CREATED, rest.exchange(ATTENDANCE_URL, HttpMethod.POST,
                authed(owner.token(), body),
                new ParameterizedTypeReference<List<AttendanceRecordDto>>() {}).getStatusCode());

        ResponseEntity<String> second = rest.exchange(ATTENDANCE_URL, HttpMethod.POST,
                authed(owner.token(), body), String.class);
        assertEquals(HttpStatus.CONFLICT, second.getStatusCode());
        assertTrue(second.getBody().contains("already exists"), "duplicate must be a 409, got: " + second.getBody());
    }

    @Test
    void checkInCanBeSessionBased() {
        OwnerFixture owner = fixture("a");
        StudentDto student = createStudent(owner.token(), "Ada", "Lovelace", unique("session"));
        DailySchedule session = sessionFor(owner.dojo());

        Map<String, Object> body = new HashMap<>();
        body.put("studentId", student.getId());
        body.put("status", "LATE");
        body.put("sessionId", session.getId());
        body.put("date", SESSION_DATE.toString());

        ResponseEntity<List<AttendanceRecordDto>> created = rest.exchange(ATTENDANCE_URL, HttpMethod.POST,
                authed(owner.token(), body),
                new ParameterizedTypeReference<List<AttendanceRecordDto>>() {});

        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        AttendanceRecordDto record = created.getBody().get(0);
        assertEquals(session.getId(), record.getSessionId(), "session-based check-in must carry the session");
        assertEquals(SESSION_DATE, record.getSessionDate());
        assertEquals("LATE", record.getStatus().name());
    }

    @Test
    void sessionIdWithoutDateIsRejected() {
        OwnerFixture owner = fixture("a");
        StudentDto student = createStudent(owner.token(), "Ada", "Lovelace", unique("nodate"));
        DailySchedule session = sessionFor(owner.dojo());

        Map<String, Object> body = new HashMap<>();
        body.put("studentId", student.getId());
        body.put("status", "PRESENT");
        body.put("sessionId", session.getId());

        ResponseEntity<String> response = rest.exchange(ATTENDANCE_URL, HttpMethod.POST,
                authed(owner.token(), body), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("requires a date"),
                "a sessionId without a date must be rejected, got: " + response.getBody());
    }

    @Test
    void requestsWithoutAValidTokenAreRejected() {
        assertEquals(HttpStatus.UNAUTHORIZED, rest.getForEntity(ATTENDANCE_URL, String.class).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                rest.exchange(ATTENDANCE_URL, HttpMethod.GET, authed("not-a-real-token", null), String.class)
                        .getStatusCode());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private record OwnerFixture(DojoOwner owner, Dojo dojo, String token) {
    }

    private OwnerFixture fixture(String label) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "owner-" + label + "-" + suffix + "@example.com";

        DojoOwner owner = new DojoOwner("Owner" + label.toUpperCase(), "Fixture", email,
                passwordEncoder.encode(PASSWORD));
        owner = ownerService.saveDojoOwner(owner);

        Dojo dojo = new Dojo("Dojo-" + label + "-" + suffix, "https://example.com/" + suffix);
        DojoOwner savedOwner = dojoService.addDojoToOwner(dojo, owner);

        Dojo savedDojo = dojoRepository.findByOwner(savedOwner).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("fixture dojo was not persisted"));

        return new OwnerFixture(savedOwner, savedDojo, login(email, PASSWORD));
    }

    private DailySchedule sessionFor(Dojo dojo) {
        WeeklySchedule weekly = new WeeklySchedule(DayOfWeek.MONDAY, 1, dojo);
        weekly = weeklyScheduleRepository.save(weekly);
        DailySchedule session = new DailySchedule(1.0, LocalTime.of(17, 0), weekly);
        return dailyScheduleRepository.save(session);
    }

    private List<AttendanceRecordDto> checkIn(String token, Long studentId, String status, Long sessionId) {
        Map<String, Object> body = checkInBody(studentId, status, sessionId, SESSION_DATE);
        ResponseEntity<List<AttendanceRecordDto>> resp = rest.exchange(ATTENDANCE_URL, HttpMethod.POST,
                authed(token, body),
                new ParameterizedTypeReference<List<AttendanceRecordDto>>() {});
        return resp.getBody();
    }

    private Map<String, Object> checkInBody(Long studentId, String status, Long sessionId, LocalDate date) {
        Map<String, Object> body = new HashMap<>();
        body.put("studentId", studentId);
        body.put("status", status);
        body.put("sessionId", sessionId);
        body.put("date", date == null ? null : date.toString());
        return body;
    }

    private StudentDto createStudent(String token, String firstName, String lastName, String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("email", email);
        body.put("rankId", null);
        body.put("dojoId", null);

        ResponseEntity<StudentDto> created = rest.exchange(STUDENTS_URL, HttpMethod.POST,
                authed(token, body), StudentDto.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode(), "student creation must succeed");
        return created.getBody();
    }

    private String login(String email, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        ResponseEntity<JwtResponse> response = rest.postForEntity("/api/auth/login", credentials, JwtResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "login must succeed for " + email);

        JwtResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getToken());

        return body.getToken();
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private HttpEntity<Object> authed(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }

        return new HttpEntity<>(body, headers);
    }
}
