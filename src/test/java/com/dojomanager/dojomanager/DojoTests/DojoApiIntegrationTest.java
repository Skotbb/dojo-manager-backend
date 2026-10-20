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
