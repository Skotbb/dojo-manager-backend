package com.dojomanager.dojomanager;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Base class for the servlet-less (MOCK) test context.
 *
 * <p>This context must not share its in-memory database with the
 * {@code RANDOM_PORT} integration context: both are configured with
 * {@code ddl-auto=create-drop}, so whichever context boots second recreates the
 * schema and resets the shared id sequence while the first context keeps
 * handing out ids from its own already-allocated pool. That produced
 * non-deterministic primary-key collisions in unrelated tests depending on how
 * many rows earlier tests had created. A dedicated database name keeps the two
 * contexts hermetic.</p>
 */
@ActiveProfiles("test")
@ContextConfiguration(classes=DojomanagerApplication.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:dojomanager-mock;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL"
})
public abstract class AbstractTest {}
