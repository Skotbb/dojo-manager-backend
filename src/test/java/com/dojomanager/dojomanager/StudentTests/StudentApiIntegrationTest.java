package com.dojomanager.dojomanager.StudentTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.dojomanager.data.dto.authorization.JwtResponse;
import com.dojomanager.data.dto.student.StudentDto;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.rank.BeltColor;
import com.dojomanager.data.entities.rank.RankLevel;
import com.dojomanager.data.entities.rank.RankName;
import com.dojomanager.data.entities.rank.RankSetting;
import com.dojomanager.data.entities.rank.StudentRank;
import com.dojomanager.data.repositories.dojo.DojoRepository;
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
 * End-to-end tests for the student API: a real servlet container on a random
 * port, real HTTP requests, real JWT bearer tokens obtained through
 * {@code POST /api/auth/login}. Owner isolation is therefore proven with
 * requests rather than by reading the code.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StudentApiIntegrationTest {

    private static final String PASSWORD = "student-api-pass";
    private static final String STUDENTS_URL = "/api/v1/students";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DojoOwnerService ownerService;

    @Autowired
    private DojoService dojoService;

    @Autowired
    private RankService rankService;

    @Autowired
    private DojoRepository dojoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void ownerCanCreateListFetchUpdateAndDeactivateAStudent() {
        OwnerFixture owner = fixture("a");

        ResponseEntity<StudentDto> created = createStudent(owner.token(),
                studentBody("Ada", "Lovelace", unique("ada"), null, null));

        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        StudentDto student = created.getBody();
        assertNotNull(student);
        assertNotNull(student.getId(), "created student must have an id");
        assertEquals(owner.dojo().getId(), student.getDojoId(), "dojo must come from the authenticated principal");
        assertTrue(student.isActive());

        assertEquals(HttpStatus.OK, listStudents(owner.token()).getStatusCode());
        assertTrue(studentIds(owner.token()).contains(student.getId()), "own student must be listed");

        ResponseEntity<StudentDto> fetched = getStudent(owner.token(), student.getId());
        assertEquals(HttpStatus.OK, fetched.getStatusCode());
        assertEquals("Ada", fetched.getBody().getFirstName());

        ResponseEntity<StudentDto> updated = updateStudent(owner.token(), student.getId(),
                studentBody("Ada", "Byron", unique("ada.byron"), null, null));
        assertEquals(HttpStatus.OK, updated.getStatusCode());
        assertEquals("Byron", updated.getBody().getLastName());

        ResponseEntity<StudentDto> deactivated = deactivateStudent(owner.token(), student.getId());
        assertEquals(HttpStatus.OK, deactivated.getStatusCode());
        assertFalse(deactivated.getBody().isActive());

        assertTrue(studentIds(owner.token()).contains(student.getId()),
                "deactivated student must still be listed (soft delete)");
    }

    @Test
    void ownerBCannotSeeFetchModifyOrDeactivateOwnerAsStudent() {
        OwnerFixture ownerA = fixture("a");
        OwnerFixture ownerB = fixture("b");

        StudentDto student = createStudent(ownerA.token(),
                studentBody("Ada", "Lovelace", unique("isolation"), null, null)).getBody();
        assertNotNull(student);

        // B's listing must not contain A's student.
        assertFalse(studentIds(ownerB.token()).contains(student.getId()),
                "owner B must not see owner A's student");

        // B cannot read it...
        assertEquals(HttpStatus.NOT_FOUND, getStudent(ownerB.token(), student.getId()).getStatusCode());
        // ...cannot update it...
        assertEquals(HttpStatus.NOT_FOUND, updateStudent(ownerB.token(), student.getId(),
                studentBody("Mallory", "Intruder", unique("mallory"), null, null)).getStatusCode());
        // ...cannot deactivate it...
        assertEquals(HttpStatus.NOT_FOUND, deactivateStudent(ownerB.token(), student.getId()).getStatusCode());

        // ...and A's student is untouched.
        StudentDto after = getStudent(ownerA.token(), student.getId()).getBody();
        assertNotNull(after);
        assertEquals("Ada", after.getFirstName());
        assertEquals("Lovelace", after.getLastName());
        assertTrue(after.isActive(), "owner B's delete attempt must not have deactivated the student");
    }

    @Test
    void ownerBCannotCreateAStudentInOwnerAsDojo() {
        OwnerFixture ownerA = fixture("a");
        OwnerFixture ownerB = fixture("b");

        ResponseEntity<String> response = rest.exchange(STUDENTS_URL, HttpMethod.POST,
                authed(ownerB.token(), studentBody("Mallory", "Intruder", unique("cross"), null, ownerA.dojo().getId())),
                String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
                "a dojo id belonging to another owner must be rejected");
        assertTrue(response.getBody().contains("does not belong to the authenticated account"));

        assertTrue(studentIds(ownerA.token()).isEmpty(), "no student may be written into owner A's dojo");
    }

    @Test
    void duplicateEmailIsRejected() {
        OwnerFixture ownerA = fixture("a");
        OwnerFixture ownerB = fixture("b");
        String email = unique("duplicate");

        assertEquals(HttpStatus.CREATED, createStudent(ownerA.token(),
                studentBody("First", "Student", email, null, null)).getStatusCode());

        // Same dojo: rejected by the dojo-scoped check with a 409 and a JSON body.
        ResponseEntity<String> sameDojo = rest.exchange(STUDENTS_URL, HttpMethod.POST,
                authed(ownerA.token(), studentBody("Second", "Student", email, null, null)), String.class);
        assertEquals(HttpStatus.CONFLICT, sameDojo.getStatusCode());
        assertTrue(sameDojo.getBody().contains("already exists in this dojo"));

        // Another dojo: email is still refused, because the inherited Person
        // mapping declares a single global unique index on email. Documented
        // limitation - see the task report.
        ResponseEntity<String> otherDojo = rest.exchange(STUDENTS_URL, HttpMethod.POST,
                authed(ownerB.token(), studentBody("Third", "Student", email, null, null)), String.class);
        assertEquals(HttpStatus.CONFLICT, otherDojo.getStatusCode());
    }

    @Test
    void invalidPayloadReturnsJsonValidationBody() {
        OwnerFixture owner = fixture("a");

        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "   ");
        body.put("lastName", "");
        body.put("email", "not-an-email");

        ResponseEntity<String> response = rest.exchange(STUDENTS_URL, HttpMethod.POST,
                authed(owner.token(), body), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String json = response.getBody();
        assertNotNull(json);
        assertTrue(json.contains("\"fieldErrors\""), "validation errors must be a JSON body, got: " + json);
        assertTrue(json.contains("firstName"), "expected a firstName field error, got: " + json);
        assertTrue(json.contains("lastName"), "expected a lastName field error, got: " + json);
        assertTrue(json.contains("email"), "expected an email field error, got: " + json);
    }

    @Test
    void deactivationKeepsTheRankAndKeepsTheRecord() {
        OwnerFixture owner = fixture("a");
        StudentRank rank = rankIn(owner.dojo());

        StudentDto student = createStudent(owner.token(),
                studentBody("Randy", "Ranked", unique("ranked"), rank.getId(), null)).getBody();
        assertNotNull(student);
        assertEquals(rank.getId(), student.getRankId());
        assertEquals("kyu", student.getCurrentRankName());

        ResponseEntity<StudentDto> deactivated = deactivateStudent(owner.token(), student.getId());
        assertEquals(HttpStatus.OK, deactivated.getStatusCode());
        assertFalse(deactivated.getBody().isActive());
        assertEquals(rank.getId(), deactivated.getBody().getRankId(),
                "deactivating must not clear the student's rank");

        StudentDto reloaded = getStudent(owner.token(), student.getId()).getBody();
        assertNotNull(reloaded, "deactivation must not hard-delete the student");
        assertFalse(reloaded.isActive());
        assertEquals(rank.getId(), reloaded.getRankId(), "rank history must survive deactivation");
        assertEquals("kyu", reloaded.getCurrentRankName());
        assertEquals(Double.valueOf(12.5d), reloaded.getHoursInRank(),
                "hours accumulated in the rank must survive deactivation");
    }

    @Test
    void requestsWithoutAValidTokenAreRejected() {
        assertEquals(HttpStatus.UNAUTHORIZED, rest.getForEntity(STUDENTS_URL, String.class).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                rest.exchange(STUDENTS_URL, HttpMethod.GET, authed("not-a-real-token", null), String.class).getStatusCode());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    @Test
    void ownerCanAddHoursAndPromoteStudent() {
        OwnerFixture fixture = fixture("promote");
        Dojo dojo = fixture.dojo();

        // Configure 2 ranks
        RankName rank1 = rankService.saveRankName(new RankName("White Belt", dojo));
        RankLevel level1 = rankService.saveRankLevel(new RankLevel(0, 1, 10, rank1));
        BeltColor belt1 = rankService.saveBeltColor(new BeltColor("White", null, 0, level1));
        RankSetting setting1 = rankService.saveRankSetting(new RankSetting(dojo, rank1, level1, belt1));

        RankName rank2 = rankService.saveRankName(new RankName("Yellow Belt", dojo));
        RankLevel level2 = rankService.saveRankLevel(new RankLevel(1, 2, 20, rank2));
        BeltColor belt2 = rankService.saveBeltColor(new BeltColor("Yellow", null, 0, level2));
        RankSetting setting2 = rankService.saveRankSetting(new RankSetting(dojo, rank2, level2, belt2));

        // Create student with rank1
        Map<String, Object> body = studentBody("Karate", "Kid", "kid-" + UUID.randomUUID() + "@example.com",
                setting1.getId(), dojo.getId());
        ResponseEntity<StudentDto> created = rest.exchange(STUDENTS_URL, HttpMethod.POST,
                authed(fixture.token(), body), StudentDto.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        Long studentId = created.getBody().getId();
        assertEquals("White Belt", created.getBody().getCurrentRankName());
        assertEquals(0.0, created.getBody().getHoursInRank());

        // Add 5 hours
        Map<String, Object> hoursBody = Map.of("hours", 5.5);
        ResponseEntity<StudentDto> hoursRes = rest.exchange(STUDENTS_URL + "/" + studentId + "/hours",
                HttpMethod.POST, authed(fixture.token(), hoursBody), StudentDto.class);
        assertEquals(HttpStatus.OK, hoursRes.getStatusCode());
        assertEquals(5.5, hoursRes.getBody().getHoursInRank());

        // Promote to next rank
        ResponseEntity<StudentDto> promoteRes = rest.exchange(STUDENTS_URL + "/" + studentId + "/promote",
                HttpMethod.POST, authed(fixture.token(), null), StudentDto.class);
        assertEquals(HttpStatus.OK, promoteRes.getStatusCode());
        assertEquals("Yellow Belt", promoteRes.getBody().getCurrentRankName());
        assertEquals(0.0, promoteRes.getBody().getHoursInRank());
    }

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

    private StudentRank rankIn(Dojo dojo) {
        RankName rankName = rankService.saveRankName(new RankName("kyu", dojo));
        RankLevel rankLevel = rankService.saveRankLevel(new RankLevel(0, 10, 40, rankName));
        BeltColor beltColor = rankService.saveBeltColor(new BeltColor("white", "none", 0, rankLevel));
        RankSetting setting = rankService.saveRankSetting(new RankSetting(dojo, rankName, rankLevel, beltColor));

        return rankService.saveRank(new StudentRank(12.5d, setting));
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

    private Map<String, Object> studentBody(String firstName, String lastName, String email, Long rankId, Long dojoId) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("email", email);
        body.put("rankId", rankId);
        body.put("dojoId", dojoId);

        return body;
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

    private ResponseEntity<StudentDto> createStudent(String token, Map<String, Object> body) {
        return rest.exchange(STUDENTS_URL, HttpMethod.POST, authed(token, body), StudentDto.class);
    }

    private ResponseEntity<StudentDto> getStudent(String token, Long id) {
        return rest.exchange(STUDENTS_URL + "/" + id, HttpMethod.GET, authed(token, null), StudentDto.class);
    }

    private ResponseEntity<StudentDto> updateStudent(String token, Long id, Map<String, Object> body) {
        return rest.exchange(STUDENTS_URL + "/" + id, HttpMethod.PUT, authed(token, body), StudentDto.class);
    }

    private ResponseEntity<StudentDto> deactivateStudent(String token, Long id) {
        return rest.exchange(STUDENTS_URL + "/" + id, HttpMethod.DELETE, authed(token, null), StudentDto.class);
    }

    private ResponseEntity<List<StudentDto>> listStudents(String token) {
        return rest.exchange(STUDENTS_URL, HttpMethod.GET, authed(token, null),
                new ParameterizedTypeReference<List<StudentDto>>() {
                });
    }

    private Set<Long> studentIds(String token) {
        List<StudentDto> students = listStudents(token).getBody();
        assertNotNull(students);

        return students.stream().map(StudentDto::getId).collect(Collectors.toSet());
    }
}
