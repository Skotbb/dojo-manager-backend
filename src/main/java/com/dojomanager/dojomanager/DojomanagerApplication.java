package com.dojomanager.dojomanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan({"com.dojomanager"})
@EntityScan({"com.dojomanager.data.entities"})
@EnableJpaRepositories("com.dojomanager.data.repositories")
public class DojomanagerApplication {


	public static void main(String[] args) {
		SpringApplication.run(DojomanagerApplication.class, args);
	}
}

@Configuration
class ApplicationConfiguration {

}
