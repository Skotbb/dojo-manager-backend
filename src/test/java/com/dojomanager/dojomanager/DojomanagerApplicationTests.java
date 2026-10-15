package com.dojomanager.dojomanager;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

class DojomanagerApplicationTests extends AbstractTest{
	@Value("${spring.datasource.url}")
	String dbUrl;

	@Test
	void contextLoads() {
		// The point of this test is that the Spring context boots hermetically
		// against in-memory H2 (see application-test.properties). The datasource
		// URL must be present; it is intentionally not pinned to a MySQL URL so
		// the test stays hermetic.
		assertThat(dbUrl).isNotNull();
	}

}
