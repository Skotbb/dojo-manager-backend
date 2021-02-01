package com.dojomanager.dojomanager;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

class DojomanagerApplicationTests extends AbstractTest{
	@Value("${spring.datasource.url}")
	String dbUrl;

	@Test
	void contextLoads() {
		assertThat(dbUrl).isNotNull();
		assertThat(dbUrl).isEqualTo("jdbc:mysql://localhost:3306/dojo_manager_test"); 
	}

}
