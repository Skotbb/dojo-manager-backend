package com.dojomanager.dojomanager.DojoTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dojomanager.data.dto.authorization.JwtResponse;
import com.dojomanager.data.dto.dojo.DojoDto;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.rank.RankSetting;
import com.dojomanager.data.repositories.dojo.DojoRepository;
import com.dojomanager.services.DojoOwnerService;
import com.dojomanager.services.RankService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end coverage for the dojo-creation rank wiring: a real servlet
 * container, a real JWT, and the persisted rank rows read back through the
 * service/repository layer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DojoApiIntegrationTest {

    private static final String PASSWORD = "dojo-create-pass";
    private static final String DOJOS_URL = "/api/v1/dojos";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private DojoOwnerService ownerService;

    @Autowired
    private RankService rankService;

    @Autowired
    private DojoRepository dojoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createDojoPersistsTheRankGraph() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "dojo-owner-" + suffix + "@example.com";

        ownerService.saveDojoOwner(new DojoOwner("Dojo", "Owner", email, passwordEncoder.encode(PASSWORD)));
        String token = login(email);

        Map<String, Object> level = new HashMap<>();
        level.put("sortOrder", 0);
        level.put("number", 1);
        level.put("hoursRequired", 10);
        level.put("beltColor", "White");
        level.put("stripeColor", null);
        level.put("stripeCount", 0);

        Map<String, Object> rank = new HashMap<>();
        rank.put("name", "White Belt");
        rank.put("levels", List.of(level));

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Dojo-" + suffix);
        body.put("website", "https://example.com/" + suffix);
        body.put("ranks", List.of(rank));

        ResponseEntity<DojoDto> response = rest.exchange(DOJOS_URL, HttpMethod.POST,
                authed(token, body), DojoDto.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        DojoDto created = response.getBody();
        assertNotNull(created);
        assertNotNull(created.getId(), "created dojo must have an id");

        Dojo dojo = dojoRepository.findById(created.getId())
                .orElseThrow(() -> new IllegalStateException("created dojo was not persisted"));
        List<RankSetting> settings = rankService.getRankSettingsByDojo(dojo);

        assertEquals(1, settings.size(), "one rank level must produce one RankSetting");
        RankSetting setting = settings.get(0);
        assertEquals("White Belt", setting.getRankName().getName());
        assertEquals(dojo.getId(), setting.getRankName().getDojo().getId());
        assertEquals(1, setting.getRankLevel().getNumber());
        assertEquals(10, setting.getRankLevel().getHoursRequired());
        assertEquals("White", setting.getBeltColor().getColor());
        assertNull(setting.getBeltColor().getStripeColor());
        assertEquals(0, setting.getBeltColor().getStripeCount());
    }

    @Test
    void ownerCanGetRanksAndAddNewRankToExistingDojo() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "dojo-owner-ranks-" + suffix + "@example.com";

        ownerService.saveDojoOwner(new DojoOwner("Dojo", "Owner", email, passwordEncoder.encode(PASSWORD)));
        String token = login(email);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Rank Dojo " + suffix);
        body.put("ranks", List.of());

        ResponseEntity<DojoDto> createResponse = rest.exchange(DOJOS_URL, HttpMethod.POST,
                authed(token, body), DojoDto.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        Long dojoId = createResponse.getBody().getId();

        // 1. Initially no ranks
        ResponseEntity<List> initialRanks = rest.exchange(DOJOS_URL + "/" + dojoId + "/ranks",
                HttpMethod.GET, authed(token, null), List.class);
        assertEquals(HttpStatus.OK, initialRanks.getStatusCode());
        assertEquals(0, initialRanks.getBody().size());

        // 2. Add a new rank to the existing dojo
        Map<String, Object> level = new HashMap<>();
        level.put("sortOrder", 0);
        level.put("number", 1);
        level.put("hoursRequired", 20);
        level.put("beltColor", "Yellow");
        level.put("stripeColor", "Black");
        level.put("stripeCount", 1);

        Map<String, Object> rank = new HashMap<>();
        rank.put("name", "Yellow Belt");
        rank.put("levels", List.of(level));

        ResponseEntity<Map> postRankResponse = rest.exchange(DOJOS_URL + "/" + dojoId + "/ranks",
                HttpMethod.POST, authed(token, rank), Map.class);
        assertEquals(HttpStatus.CREATED, postRankResponse.getStatusCode());
        assertEquals("Yellow Belt", postRankResponse.getBody().get("name"));

        // 3. Verify rank is now returned by GET
        ResponseEntity<List> updatedRanks = rest.exchange(DOJOS_URL + "/" + dojoId + "/ranks",
                HttpMethod.GET, authed(token, null), List.class);
        assertEquals(HttpStatus.OK, updatedRanks.getStatusCode());
        assertEquals(1, updatedRanks.getBody().size());
    }

    @Test
    void ownerCanUpdateDojoWithRanksViaPut() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "dojo-owner-put-" + suffix + "@example.com";

        ownerService.saveDojoOwner(new DojoOwner("Dojo", "Owner", email, passwordEncoder.encode(PASSWORD)));
        String token = login(email);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Initial Name " + suffix);
        body.put("ranks", List.of());

        ResponseEntity<DojoDto> createResponse = rest.exchange(DOJOS_URL, HttpMethod.POST,
                authed(token, body), DojoDto.class);
        Long dojoId = createResponse.getBody().getId();

        // Update via PUT /api/v1/dojos/{id}
        Map<String, Object> level = new HashMap<>();
        level.put("sortOrder", 0);
        level.put("number", 1);
        level.put("hoursRequired", 15);
        level.put("beltColor", "Orange");
        level.put("stripeCount", 0);

        Map<String, Object> rank = new HashMap<>();
        rank.put("name", "Orange Belt");
        rank.put("levels", List.of(level));

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", "Updated Name " + suffix);
        updateBody.put("website", "https://updated.example.com");
        updateBody.put("ranks", List.of(rank));

        ResponseEntity<DojoDto> updateResponse = rest.exchange(DOJOS_URL + "/" + dojoId,
                HttpMethod.PUT, authed(token, updateBody), DojoDto.class);
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("Updated Name " + suffix, updateResponse.getBody().getName());
        assertEquals(1, updateResponse.getBody().getRanks().size());
        assertEquals("Orange Belt", updateResponse.getBody().getRanks().get(0).getName());
    }

    @Test
    void ownerBCannotReadOrModifyRanksOfOwnerAsDojo() {
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        String emailA = "dojo-a-" + suffixA + "@example.com";
        ownerService.saveDojoOwner(new DojoOwner("Owner", "A", emailA, passwordEncoder.encode(PASSWORD)));
        String tokenA = login(emailA);

        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        String emailB = "dojo-b-" + suffixB + "@example.com";
        ownerService.saveDojoOwner(new DojoOwner("Owner", "B", emailB, passwordEncoder.encode(PASSWORD)));
        String tokenB = login(emailB);

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Dojo A");
        ResponseEntity<DojoDto> createResponse = rest.exchange(DOJOS_URL, HttpMethod.POST,
                authed(tokenA, body), DojoDto.class);
        Long dojoAId = createResponse.getBody().getId();

        // Owner B tries to GET dojo A's ranks
        ResponseEntity<String> getResponse = rest.exchange(DOJOS_URL + "/" + dojoAId + "/ranks",
                HttpMethod.GET, authed(tokenB, null), String.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());

        // Owner B tries to POST to dojo A's ranks
        Map<String, Object> rank = new HashMap<>();
        rank.put("name", "Malicious Belt");
        rank.put("levels", List.of());
        ResponseEntity<String> postResponse = rest.exchange(DOJOS_URL + "/" + dojoAId + "/ranks",
                HttpMethod.POST, authed(tokenB, rank), String.class);
        assertEquals(HttpStatus.NOT_FOUND, postResponse.getStatusCode());
    }

    private String login(String email) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", PASSWORD);

        ResponseEntity<JwtResponse> response = rest.postForEntity("/api/auth/login", credentials, JwtResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "login must succeed for " + email);

        JwtResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getToken());

        return body.getToken();
    }

    private HttpEntity<Object> authed(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        return new HttpEntity<>(body, headers);
    }
}
